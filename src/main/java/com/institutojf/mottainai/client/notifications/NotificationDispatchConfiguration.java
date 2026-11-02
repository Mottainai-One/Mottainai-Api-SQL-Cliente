package com.institutojf.mottainai.client.notifications;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita o agendamento; o produtor permanece desativado até configuração explícita. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationDispatchProperties.class)
public class NotificationDispatchConfiguration {}
