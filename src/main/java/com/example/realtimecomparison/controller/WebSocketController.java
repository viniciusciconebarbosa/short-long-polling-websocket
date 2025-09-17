package com.example.realtimecomparison.controller;

import com.example.realtimecomparison.entity.Notification;
import com.example.realtimecomparison.service.MetricsService;
import com.example.realtimecomparison.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
public class WebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Endpoint REST para obter estatísticas do WebSocket
     */
    @GetMapping("/stats")
    public ResponseEntity<WebSocketStats> getStats() {
        try {
            // Simular estatísticas (em uma aplicação real, você manteria essas informações)
            WebSocketStats stats = new WebSocketStats(
                "WebSocket ativo",
                "Conexão persistente",
                "Tempo real"
            );
            
            logger.debug("Estatísticas do WebSocket solicitadas");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas do WebSocket", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint REST para enviar notificação manual via WebSocket
     */
    @PostMapping("/send-notification")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Mensagem é obrigatória");
            }
            
            Notification notification = new Notification(message);
            // Salvar notificação (usando o serviço)
            notification = notificationService.notificationRepository.save(notification);
            
            // Enviar via WebSocket
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            
            // Registrar métricas
            metricsService.incrementNotificationCount("websocket");
            
            logger.info("Notificação manual enviada via WebSocket: {}", notification);
            return ResponseEntity.ok("Notificação enviada com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação via WebSocket", e);
            return ResponseEntity.internalServerError().body("Erro ao enviar notificação");
        }
    }
    
    /**
     * Endpoint REST para obter histórico de notificações
     */
    @GetMapping("/notifications/history")
    public ResponseEntity<List<Notification>> getNotificationHistory(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<Notification> notifications = notificationService.getLatestNotifications(limit);
            
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordRequest("websocket", latency);
            
            logger.debug("Histórico de notificações: {} notificações retornadas em {}ms", 
                        notifications.size(), latency);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            logger.error("Erro ao obter histórico de notificações", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint REST para resetar métricas do WebSocket
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<String> resetMetrics() {
        try {
            metricsService.resetMetrics("websocket");
            logger.info("Métricas do WebSocket resetadas");
            return ResponseEntity.ok("Métricas do WebSocket resetadas com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar métricas do WebSocket", e);
            return ResponseEntity.internalServerError().body("Erro ao resetar métricas");
        }
    }
    
    /**
     * Handler para mensagens WebSocket (quando cliente envia mensagem)
     */
    @MessageMapping("/notifications")
    @SendTo("/topic/notifications")
    public Notification handleNotification(Notification notification) {
        logger.debug("Mensagem recebida via WebSocket: {}", notification);
        
        // Registrar métricas (WebSocket tem latência muito baixa)
        metricsService.recordRequest("websocket", 1); // 1ms para WebSocket
        
        return notification;
    }
    
    /**
     * Classe para estatísticas do WebSocket
     */
    @lombok.Value
    public static class WebSocketStats {
        String status;
        String connectionType;
        String latency;
    }
}
