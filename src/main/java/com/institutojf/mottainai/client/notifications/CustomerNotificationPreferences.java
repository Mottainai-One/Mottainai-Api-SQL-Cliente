package com.institutojf.mottainai.client.notifications;

import com.institutojf.mottainai.client.security.ProtectedDataToken;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Mantém preferências e posição temporária somente para o cliente Firebase autenticado. */
@Service
public class CustomerNotificationPreferences {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    /** Usa o mesmo contexto transacional protegido do restante da API Cliente. */
    public CustomerNotificationPreferences(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc=jdbc; this.transactions=new TransactionTemplate(manager);
    }

    /** Consulta a preferência própria; nenhuma adesão é criada automaticamente. */
    public Preferences get(String uid) {
        return transactions.execute(tx -> read(customer(uid)));
    }

    /** Retorna até cem lojas mais próximas para registrar geocercas; o raio de alerta continua em cinco ou dez km. */
    public java.util.List<NotificationStore> stores(String uid) {
        return transactions.execute(tx -> jdbc.query("""
            SELECT s.id,s.latitude,s.longitude,n.radius_km*1000 AS radius_meters
            FROM mottainai.customer_notification_preference n
            CROSS JOIN mottainai.api_client_catalog_stores s
            WHERE n.customer_id=? AND n.enabled IS TRUE AND n.location_expires_at>statement_timestamp()
              AND mottainai.fn_notification_distance_km(n.latitude,n.longitude,s.latitude,s.longitude)<=n.radius_km*2
            ORDER BY mottainai.fn_notification_distance_km(n.latitude,n.longitude,s.latitude,s.longitude),s.id
            LIMIT 100
            """,(rs,row)->new NotificationStore(rs.getInt("id"),rs.getDouble("latitude"),
                rs.getDouble("longitude"),rs.getInt("radius_meters")),customer(uid)));
    }

    /** Dados mínimos de uma geocerca; não contém endereço residencial ou histórico de posição. */
    public record NotificationStore(int storeId,double latitude,double longitude,int radiusMeters){}

    /** Registra adesão explícita e raio permitido; a desativação remove imediatamente a posição. */
    public Preferences save(String uid, Boolean enabled, Integer radiusKm) {
        if(enabled==null || radiusKm==null || radiusKm!=5 && radiusKm!=10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use enabled and radiusKm=5 or 10");
        }
        return transactions.execute(tx -> {
            int id=customer(uid);
            jdbc.update("""
                INSERT INTO mottainai.customer_notification_preference AS n(customer_id,enabled,radius_km)
                VALUES(?,?,?) ON CONFLICT(customer_id) DO UPDATE
                SET enabled=EXCLUDED.enabled,radius_km=EXCLUDED.radius_km,updated_at=clock_timestamp(),
                    latitude=CASE WHEN EXCLUDED.enabled THEN n.latitude END,
                    longitude=CASE WHEN EXCLUDED.enabled THEN n.longitude END,
                    accuracy_meters=CASE WHEN EXCLUDED.enabled THEN n.accuracy_meters END,
                    location_observed_at=CASE WHEN EXCLUDED.enabled THEN n.location_observed_at END,
                    location_expires_at=CASE WHEN EXCLUDED.enabled THEN n.location_expires_at END
                """,id,enabled,radiusKm);
            return read(id);
        });
    }

    /** Aceita uma posição recente, sem habilitar notificações ou aceitar outro cliente no corpo. */
    public Preferences locate(String uid, Double latitude, Double longitude, Double accuracy, Instant observedAt) {
        Instant now=Instant.now();
        if(latitude==null || longitude==null || accuracy==null || observedAt==null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(accuracy)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180
                || accuracy<0 || accuracy>10000 || observedAt.isBefore(now.minusSeconds(120))
                || observedAt.isAfter(now.plusSeconds(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or stale location");
        }
        return transactions.execute(tx -> {
            int id=customer(uid);
            if(!read(id).enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Enable notifications first");
            jdbc.update("""
                UPDATE mottainai.customer_notification_preference
                SET latitude=?,longitude=?,accuracy_meters=?,location_observed_at=?,location_expires_at=?,updated_at=clock_timestamp()
                WHERE customer_id=? AND enabled IS TRUE
                  AND (location_observed_at IS NULL OR location_observed_at<=?)
                """,latitude,longitude,accuracy,Timestamp.from(observedAt),Timestamp.from(observedAt.plusSeconds(1800)),
                    id,Timestamp.from(observedAt));
            return read(id);
        });
    }

    /** Apaga a posição quando a localização é revogada ou desligada, preservando a escolha de adesão. */
    public void clearLocation(String uid) {
        transactions.executeWithoutResult(tx -> jdbc.update("""
            UPDATE mottainai.customer_notification_preference
            SET latitude=NULL,longitude=NULL,accuracy_meters=NULL,location_observed_at=NULL,location_expires_at=NULL,
                updated_at=clock_timestamp() WHERE customer_id=?
            """,customer(uid)));
    }

    /** Estabelece a titularidade por token protegido, sem confiar em IDs fornecidos pelo aplicativo. */
    private int customer(String uid) {
        Integer id=jdbc.queryForObject("SELECT mottainai.fn_bootstrap_customer_context(?)",Integer.class,
                ProtectedDataToken.externalAuthUid(uid));
        if(id==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active customer profile not found");
        return id;
    }

    /** Retorna apenas metadados da posição; não expõe coordenadas ou histórico de deslocamento. */
    private Preferences read(int id) {
        var rows=jdbc.query("""
            SELECT enabled,radius_km,location_observed_at,location_expires_at
            FROM mottainai.customer_notification_preference WHERE customer_id=?
            """,(rs,row)->new Preferences(rs.getBoolean("enabled"),rs.getInt("radius_km"),
                instant(rs.getTimestamp("location_observed_at")),instant(rs.getTimestamp("location_expires_at"))),id);
        return rows.isEmpty()?new Preferences(false,5,null,null):rows.getFirst();
    }

    /** Converte o instante do PostgreSQL sem inventar uma posição quando o campo está vazio. */
    private static Instant instant(Timestamp value){return value==null?null:value.toInstant();}

    /** Estado oficial da preferência, com validade explícita da posição consentida. */
    public record Preferences(boolean enabled,int radiusKm,Instant locationObservedAt,Instant locationExpiresAt){}
}
