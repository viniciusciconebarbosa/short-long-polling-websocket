package com.example.realtimecomparison.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(@org.springframework.lang.NonNull MessageBrokerRegistry config) {
        // Habilitar broker de mensagens simples em memória
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefixo para mensagens destinadas ao servidor
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(@org.springframework.lang.NonNull StompEndpointRegistry registry) {
        // Registrar endpoint WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Permitir CORS para desenvolvimento
                .withSockJS(); // Fallback para navegadores que não suportam WebSocket
    }
}
