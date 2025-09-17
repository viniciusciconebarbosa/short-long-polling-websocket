package com.example.realtimecomparison.controller;

import com.example.realtimecomparison.entity.Notification;
import com.example.realtimecomparison.service.MetricsService;
import com.example.realtimecomparison.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/short-polling")
@CrossOrigin(origins = "*")
public class ShortPollingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ShortPollingController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * Endpoint para short polling - retorna notificações não entregues
     * Cliente deve chamar este endpoint a cada 5 segundos
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(value = "since", required = false) String sinceParam) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<Notification> notifications;
            
            if (sinceParam != null && !sinceParam.isEmpty()) {
                // Buscar notificações criadas após o timestamp fornecido
                Instant since = Instant.parse(sinceParam);
                notifications = notificationService.getNotificationsAfter(since);
                logger.debug("Short polling: {} notificações encontradas desde {}", 
                           notifications.size(), since);
            } else {
                // Buscar todas as notificações não entregues
                notifications = notificationService.getUndeliveredNotifications();
                logger.debug("Short polling: {} notificações não entregues encontradas", 
                           notifications.size());
            }
            
            // Marcar notificações como entregues
            notifications.forEach(notification -> {
                notificationService.markAsDelivered(notification.getId());
            });
            
            // Calcular latência
            long latency = System.currentTimeMillis() - startTime;
            
            // Registrar métricas
            metricsService.recordRequest("short", latency);
            if (!notifications.isEmpty()) {
                for (int i = 0; i < notifications.size(); i++) {
                    metricsService.incrementNotificationCount("short");
                }
            }
            
            logger.info("Short polling: {} notificações retornadas em {}ms", 
                       notifications.size(), latency);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            logger.error("Erro no short polling", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para buscar as últimas N notificações
     */
    @GetMapping("/notifications/latest")
    public ResponseEntity<List<Notification>> getLatestNotifications(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<Notification> notifications = notificationService.getLatestNotifications(limit);
            
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordRequest("short", latency);
            
            logger.debug("Short polling latest: {} notificações retornadas em {}ms", 
                        notifications.size(), latency);
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            logger.error("Erro ao buscar últimas notificações", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para contar notificações criadas após um timestamp
     */
    @GetMapping("/notifications/count")
    public ResponseEntity<Long> getNotificationCount(
            @RequestParam(value = "since", required = false) String sinceParam) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            long count;
            
            if (sinceParam != null && !sinceParam.isEmpty()) {
                Instant since = Instant.parse(sinceParam);
                count = notificationService.countNotificationsAfter(since);
            } else {
                count = notificationService.getUndeliveredNotifications().size();
            }
            
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordRequest("short", latency);
            
            logger.debug("Short polling count: {} notificações em {}ms", count, latency);
            
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            logger.error("Erro ao contar notificações", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para resetar métricas do short polling
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<String> resetMetrics() {
        try {
            metricsService.resetMetrics("short");
            logger.info("Métricas do short polling resetadas");
            return ResponseEntity.ok("Métricas do short polling resetadas com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar métricas do short polling", e);
            return ResponseEntity.internalServerError().body("Erro ao resetar métricas");
        }
    }
}
