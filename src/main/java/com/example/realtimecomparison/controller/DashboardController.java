package com.example.realtimecomparison.controller;

import com.example.realtimecomparison.service.MetricsService;
import com.example.realtimecomparison.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * Obtém dados completos para o dashboard
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboardData = new HashMap<>();
            
            // Obter resumo das métricas
            MetricsService.MetricsSummary metricsSummary = metricsService.getSummary();
            dashboardData.put("metrics", metricsSummary);
            
            // Obter últimas notificações
            List<Map<String, Object>> latestNotifications = notificationService.getLatestNotifications(10).stream()
                    .map(notification -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", notification.getId());
                        map.put("message", notification.getMessage());
                        map.put("createdAt", notification.getCreatedAt().toString());
                        map.put("delivered", notification.isDelivered());
                        return map;
                    })
                    .toList();
            dashboardData.put("latestNotifications", latestNotifications);
            
            // Estatísticas gerais
            Map<String, Object> generalStats = Map.of(
                "totalNotifications", notificationService.countNotificationsAfter(java.time.Instant.EPOCH),
                "undeliveredNotifications", notificationService.getUndeliveredNotifications().size(),
                "timestamp", System.currentTimeMillis()
            );
            dashboardData.put("generalStats", generalStats);
            
            logger.debug("Dados do dashboard solicitados");
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            logger.error("Erro ao obter dados do dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém estatísticas em tempo real (para atualizações via AJAX)
     */
    @GetMapping("/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeStats() {
        try {
            Map<String, Object> realtimeStats = new HashMap<>();
            
            // Obter métricas atualizadas
            MetricsService.MetricsSummary summary = metricsService.getSummary();
            realtimeStats.put("metrics", summary);
            
            // Contar notificações não entregues
            int undeliveredCount = notificationService.getUndeliveredNotifications().size();
            realtimeStats.put("undeliveredCount", undeliveredCount);
            
            // Timestamp da última atualização
            realtimeStats.put("lastUpdate", System.currentTimeMillis());
            
            logger.debug("Estatísticas em tempo real solicitadas");
            return ResponseEntity.ok(realtimeStats);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas em tempo real", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para resetar todas as métricas e dados
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetAll() {
        try {
            metricsService.resetAllMetrics();
            logger.info("Dashboard resetado - todas as métricas foram limpas");
            return ResponseEntity.ok("Dashboard resetado com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar dashboard", e);
            return ResponseEntity.internalServerError().body("Erro ao resetar dashboard");
        }
    }
    
    /**
     * Endpoint de saúde da aplicação
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "services", Map.of(
                    "notificationService", "UP",
                    "metricsService", "UP",
                    "database", "UP"
                )
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Erro ao verificar saúde da aplicação", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
