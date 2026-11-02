package com.institutojf.mottainai.client.notifications;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.junit.jupiter.api.Test;

/** Confere os limites que evitam encaminhar a credencial privada a um destino inadequado. */
class NotificationDispatchPropertiesTest {
    private static final String SECRET="fixture-only-publisher-secret-32-characters";

    /** Produção exige HTTPS e uma origem sem caminhos, consulta, fragmento ou credencial embutida. */
    @Test void refusesUnsafePublisherConfiguration(){
        for(String uri:new String[]{"http://example.test/","https://user:pass@example.test/",
                "https://example.test/path","https://example.test/?token=x","https://example.test/#fragment"}){
            assertThrows(IllegalStateException.class,()->new NotificationDispatchProperties(true,URI.create(uri),SECRET,false));
        }
        assertThrows(IllegalStateException.class,()->new NotificationDispatchProperties(true,URI.create("https://example.test/"),"short",false));
    }

    /** O modo local precisa ser autorizado explicitamente e nunca imprime a credencial. */
    @Test void permitsExplicitLocalModeWithoutLeakingSecret(){
        var properties=new NotificationDispatchProperties(true,URI.create("http://127.0.0.1:8093/"),SECRET,true);
        assertTrue(properties.enabled());
        assertFalse(properties.toString().contains(SECRET));
        assertDoesNotThrow(()->new NotificationDispatchProperties(false,null,null,false));
    }
}
