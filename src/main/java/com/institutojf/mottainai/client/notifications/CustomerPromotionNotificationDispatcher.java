package com.institutojf.mottainai.client.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.client.security.NotificationAccessTokenService;
import com.institutojf.mottainai.client.security.NotificationTokenProperties;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Publica entregas autorizadas pelo SQL, com lease e ID estável para tentativas idempotentes. */
@Service
public class CustomerPromotionNotificationDispatcher implements AutoCloseable {
    private static final Logger LOG=LoggerFactory.getLogger(CustomerPromotionNotificationDispatcher.class);
    private final NotificationDispatchProperties properties;
    private final NotificationTokenProperties tokens;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final HttpClient http;
    private final UUID worker=UUID.randomUUID();

    /** Prepara o HTTP sem redirecionamentos e verifica a configuração de destinatários. */
    public CustomerPromotionNotificationDispatcher(NotificationDispatchProperties properties,NotificationTokenProperties tokens,
            JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json) {
        this.properties=properties;this.tokens=tokens;this.jdbc=jdbc;this.transactions=new TransactionTemplate(manager);this.json=json;
        if(properties.enabled() && !tokens.enabled()) throw new IllegalStateException("Enable notification credentials before dispatch");
        http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    /** Consulta novos avisos sem depender de uma chamada do aplicativo ou de geração de texto por IA. */
    @Scheduled(fixedDelay=15000,initialDelay=10000)
    public void poll() {
        if(!properties.enabled())return;
        try { dispatchBatch(); }
        catch(RuntimeException error){LOG.warn("Notification dispatch deferred ({})",error.getClass().getSimpleName());}
    }

    /** Libera a conexão SQL antes da rede; cada lote cabe no lease de noventa segundos. */
    public int dispatchBatch() {
        if(!properties.enabled())return 0;
        var deliveries=transactions.execute(tx->{jdbc.execute("SET LOCAL statement_timeout='3s'");return jdbc.query(
                "SELECT * FROM mottainai.fn_claim_customer_promotion_notifications(?,?)",
                (rs,row)->new Delivery(rs.getLong("delivery_id"),rs.getInt("customer_id"),rs.getInt("promotion_id"),
                    rs.getInt("store_id"),rs.getInt("product_id"),rs.getString("title"),rs.getString("body"),
                    rs.getInt("priority"),rs.getTimestamp("expires_at").toInstant()),worker,10);});
        if(deliveries==null)return 0;
        int published=0;
        for(Delivery delivery:deliveries) {
            if(Thread.currentThread().isInterrupted())break;
            String failure="publish_failed";
            boolean accepted=false;
            try {
                String id="customer-promotion-delivery-"+delivery.id();
                String body=json.writeValueAsString(Map.of(
                    "companyId",tokens.companyId(),"userId",NotificationAccessTokenService.recipientId(delivery.customerId()),
                    "id",id,"title",delivery.title(),"body",delivery.body(),"priority",delivery.priority(),
                    "promotionId",delivery.promotionId(),"storeId",delivery.storeId(),"productId",delivery.productId(),
                    "expiresAt",delivery.expiresAt().toEpochMilli()));
                HttpRequest request=HttpRequest.newBuilder(properties.redisBaseUrl().resolve("/internal/v1/notifications"))
                        .timeout(Duration.ofSeconds(5)).header("Content-Type","application/json")
                        .header("X-Mottainai-Notification-Secret",properties.publisherSecret())
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
                failure="http_"+response.statusCode();
                if(response.statusCode()==202) {
                    var receipt=json.readTree(response.body());
                    accepted=id.equals(receipt.path("id").asText()) && "queued".equals(receipt.path("status").asText());
                    if(!accepted)failure="invalid_receipt";
                }
            }catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}
            catch(Exception error){failure=error.getClass().getSimpleName();}
            boolean success=accepted;
            String reason=failure;
            transactions.executeWithoutResult(tx->jdbc.queryForObject(
                "SELECT mottainai.fn_complete_customer_promotion_notification(?,?,?,?)",
                Boolean.class,delivery.id(),worker,success,success?null:reason));
            if(accepted)published++;
        }
        return published;
    }

    /** Fecha somente as conexões deste produtor ao encerrar a aplicação. */
    @PreDestroy public void close(){http.close();}

    /** Transporta dados públicos da oferta e o identificador interno do destinatário. */
    public record Delivery(long id,int customerId,int promotionId,int storeId,int productId,
                           String title,String body,int priority,Instant expiresAt){}
}
