package com.institutojf.mottainai.client.notifications;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

/** Valida os limites de entrada antes que dados inválidos cheguem ao banco. */
class NotificationPreferencesValidationTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    private final CustomerNotificationPreferences preferences=new CustomerNotificationPreferences(jdbc,manager);

    /** Simula somente a delimitação transacional; os testes de SQL usam PostgreSQL real separado. */
    @BeforeEach void transactions(){when(manager.getTransaction(any())).thenAnswer(call->new SimpleTransactionStatus());}

    /** Nenhum raio fora das opções oficiais pode abrir uma transação de atualização. */
    @Test void rejectsUnsupportedRadiusAndMissingOptIn(){
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            ()->preferences.save("uid",true,15)).getStatusCode());
        assertThrows(ResponseStatusException.class,()->preferences.save("uid",null,5));
        assertThrows(ResponseStatusException.class,()->preferences.save("uid",true,null));
        verifyNoInteractions(jdbc,manager);
    }

    /** Recusa posições impossíveis e precisão sem significado geográfico. */
    @Test void rejectsInvalidCoordinates(){
        for(double latitude:new double[]{Double.NaN,Double.POSITIVE_INFINITY,91,-91}){
            assertThrows(ResponseStatusException.class,()->preferences.locate("uid",latitude,0.0,10.0,Instant.now()));
        }
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,181.0,10.0,Instant.now()));
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,0.0,-1.0,Instant.now()));
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,0.0,10001.0,Instant.now()));
        verifyNoInteractions(jdbc,manager);
    }

    /** Dados antigos ou adiantados não prolongam artificialmente a permanência do cliente perto da loja. */
    @Test void rejectsStaleAndFutureLocation(){
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,0.0,10.0,Instant.now().minusSeconds(121)));
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,0.0,10.0,Instant.now().plusSeconds(60)));
        assertThrows(ResponseStatusException.class,()->preferences.locate("uid",0.0,0.0,10.0,null));
        verifyNoInteractions(jdbc,manager);
    }

    /** A consulta não cria adesão por padrão nem aceita uma identidade SQL no lugar do Firebase. */
    @Test void refusesAnUnlinkedFirebaseIdentity(){
        when(jdbc.queryForObject(anyString(),eq(Integer.class),anyString())).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            ()->preferences.get("firebase-unlinked")).getStatusCode());
        verify(jdbc).queryForObject(eq("SELECT mottainai.fn_bootstrap_customer_context(?)"),eq(Integer.class),
            eq(com.institutojf.mottainai.client.security.ProtectedDataToken.externalAuthUid("firebase-unlinked")));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
