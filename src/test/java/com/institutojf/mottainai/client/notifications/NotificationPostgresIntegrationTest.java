package com.institutojf.mottainai.client.notifications;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.institutojf.mottainai.client.security.NotificationTokenProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/** Exercita o papel restrito e o produtor HTTP somente no PostgreSQL descartável identificado no teste. */
@EnabledIfEnvironmentVariable(named="NOTIFICATION_TEST_DB_URL",matches="jdbc:postgresql://mottainai-notification-tests-db:5432/notification_test")
class NotificationPostgresIntegrationTest {
    /** Confere preferência própria, posição, geocercas, reserva, recibo e ausência de repetição após confirmação. */
    @Test void persistsOwnPreferenceAndPublishesLeasedDeliveries()throws Exception{
        DriverManagerDataSource source=new DriverManagerDataSource(System.getenv("NOTIFICATION_TEST_DB_URL"),
            "notification_api_test","notification-test-only");
        JdbcTemplate jdbc=new JdbcTemplate(source);DataSourceTransactionManager tx=new DataSourceTransactionManager(source);
        CustomerNotificationPreferences preferences=new CustomerNotificationPreferences(jdbc,tx);
        assertFalse(preferences.get("notification-customer-10").enabled());
        assertTrue(preferences.save("notification-customer-10",true,10).enabled());
        assertEquals(10,preferences.get("notification-customer-10").radiusKm());
        preferences.locate("notification-customer-10",0.0,0.0,10.0,Instant.now());
        assertFalse(preferences.stores("notification-customer-10").isEmpty());
        preferences.save("notification-customer-10",false,5);
        assertNull(preferences.get("notification-customer-10").locationExpiresAt());
        for(int n=1;n<=5;n++)preferences.locate("notification-customer-"+n,0.0,.001*n,10.0,Instant.now());
        ObjectMapper json=new ObjectMapper();List<com.fasterxml.jackson.databind.JsonNode> messages=new ArrayList<>();
        String secret="integration-test-only-publisher-secret";
        HttpServer server=HttpServer.create(new InetSocketAddress("0.0.0.0",0),0);
        server.createContext("/internal/v1/notifications",exchange->{
            assertEquals("POST",exchange.getRequestMethod());
            assertEquals(secret,exchange.getRequestHeaders().getFirst("X-Mottainai-Notification-Secret"));
            var body=json.readTree(exchange.getRequestBody());messages.add(body);
            byte[] response=json.writeValueAsBytes(java.util.Map.of("id",body.path("id").asText(),"status","queued"));
            exchange.sendResponseHeaders(202,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        NotificationDispatchProperties dispatch=new NotificationDispatchProperties(true,
            URI.create("http://127.0.0.1:"+server.getAddress().getPort()),secret,true);
        NotificationTokenProperties tokens=new NotificationTokenProperties(true,java.util.Base64.getEncoder().encodeToString(new byte[32]),1,300);
        try(CustomerPromotionNotificationDispatcher dispatcher=new CustomerPromotionNotificationDispatcher(dispatch,tokens,jdbc,tx,json)){
            assertEquals(3,dispatcher.dispatchBatch());assertEquals(0,dispatcher.dispatchBatch());assertEquals(3,messages.size());
            for(var body:messages){assertEquals(4294967296L+920001,body.path("userId").asLong());
                assertEquals(910001,body.path("storeId").asInt());assertTrue(body.path("productId").asInt()>930001);
                assertTrue(body.path("expiresAt").asLong()>System.currentTimeMillis());assertEquals(3,body.path("priority").asInt());}
        }finally{server.stop(0);}
    }
}
