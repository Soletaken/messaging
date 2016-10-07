package com.example;

import com.example.util.Utilis;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.IntegrationConfigUtils;

@MessagingGateway
public interface MyGateway {

    @Gateway(requestChannel = Utilis.INBOUND_CHANNEL, requestTimeout = 1000, replyTimeout = 1000)
    String process(String param);
}
