package com.institutojf.mottainai.client.notifications;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração privada do produtor, separada da credencial emitida para o aplicativo. */
@ConfigurationProperties(prefix="notifications.dispatch")
public record NotificationDispatchProperties(boolean enabled,URI redisBaseUrl,String publisherSecret,boolean allowInsecure) {
    /** Recusa destinos com credenciais, redirecionamentos ou HTTP não autorizado. */
    public NotificationDispatchProperties {
        if(enabled && (redisBaseUrl==null || redisBaseUrl.getHost()==null || redisBaseUrl.getUserInfo()!=null
                || redisBaseUrl.getQuery()!=null || redisBaseUrl.getFragment()!=null
                || !(redisBaseUrl.getPath().isEmpty() || redisBaseUrl.getPath().equals("/"))
                || !("https".equals(redisBaseUrl.getScheme()) || allowInsecure && "http".equals(redisBaseUrl.getScheme()))
                || redisBaseUrl.getPort()==0 || redisBaseUrl.getPort()>65535
                || publisherSecret==null || !publisherSecret.matches("[!-~]{32,512}"))) {
            throw new IllegalStateException("Configure a private HTTPS notification publisher before enabling dispatch");
        }
    }
    /** Nunca imprime a credencial de publicação em logs de configuração. */
    @Override public String toString(){return "NotificationDispatchProperties[enabled="+enabled+", publisherSecret=[REDACTED]]";}
}
