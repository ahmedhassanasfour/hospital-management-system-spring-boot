package com.ahmed.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    /**
     * Comma-separated list of allowed WebSocket origins, resolved from the
     * profile-specific application properties.
     *
     * dev profile  → defaults to localhost origins (application-dev.properties)
     * prod profile → WEBSOCKET_ALLOWED_ORIGINS env var is REQUIRED; no fallback
     *
     * No Java-level default — the property files are the single source of truth.
     */
    @Value("${websocket.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        registry.enableSimpleBroker("/topic");

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }
}