package com.example.realtimecomparison.controller;

import com.example.realtimecomparison.entity.Notification;
import com.example.realtimecomparison.service.LongPollingManager;
import com.example.realtimecomparison.service.MetricsService;
import com.example.realtimecomparison.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/long-polling")
@CrossOrigin(origins = "*")
public class LongPollingController {
    
    private static final Logger logger = LoggerFactory.getLogger(LongPollingController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private LongPollingManager longPollingManager;
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * Endpoint para long polling - aguarda até 30 segundos por novas notificações
     */
    @GetMapping("/notifications")
    public DeferredResult<ResponseEntity<List<Notification>>> getNotifications(
            @RequestParam(value = "since", required = false) String sinceParam,
            @RequestParam(value = "clientId", required = false) String clientIdParam) {
        
        long startTime = System.currentTimeMillis();
        String clientId = clientIdParam != null ? clientIdParam : UUID.randomUUID().toString();
        
        DeferredResult<ResponseEntity<List<Notification>>> deferredResult = new DeferredResult<>(30000L);
        
        // Configurar callbacks
        deferredResult.onTimeout(() -> {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordRequest("long", latency);
            
            logger.debug("Long polling timeout para cliente {} após {}ms", clientId, latency);
            deferredResult.setResult(ResponseEntity.ok(List.of()));
        });
        
        deferredResult.onCompletion(() -> {
            long latency = System.currentTimeMillis() - startTime;
            logger.debug("Long polling completado para cliente {} em {}ms", clientId, latency);
        });
        
        deferredResult.onError(throwable -> {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("Erro no long polling para cliente {} após {}ms", clientId, latency, throwable);
            deferredResult.setResult(ResponseEntity.internalServerError().build());
        });
        
        // Verificar se já existem notificações não entregues
        try {
            List<Notification> existingNotifications;
            
            if (sinceParam != null && !sinceParam.isEmpty()) {
                Instant since = Instant.parse(sinceParam);
                existingNotifications = notificationService.getNotificationsAfter(since);
            } else {
                existingNotifications = notificationService.getUndeliveredNotifications();
            }
            
            if (!existingNotifications.isEmpty()) {
                // Se já existem notificações, retornar imediatamente
                long latency = System.currentTimeMillis() - startTime;
                metricsService.recordRequest("long", latency);
                
                // Marcar como entregues
                existingNotifications.forEach(notification -> {
                    notificationService.markAsDelivered(notification.getId());
                    metricsService.incrementNotificationCount("long");
                });
                
                logger.info("Long polling: {} notificações existentes retornadas imediatamente para cliente {} em {}ms", 
                           existingNotifications.size(), clientId, latency);
                
                deferredResult.setResult(ResponseEntity.ok(existingNotifications));
                return deferredResult;
            }
            
        } catch (Exception e) {
            logger.error("Erro ao verificar notificações existentes para cliente {}", clientId, e);
            deferredResult.setResult(ResponseEntity.internalServerError().build());
            return deferredResult;
        }
        
        // Se não há notificações existentes, adicionar cliente para aguardar
        DeferredResult<List<Notification>> internalResult = new DeferredResult<>(30000L);
        
        internalResult.onTimeout(() -> {
            long latency = System.currentTimeMillis() - startTime;
            metricsService.recordRequest("long", latency);
            logger.debug("Long polling interno timeout para cliente {} após {}ms", clientId, latency);
        });
        
        internalResult.onCompletion(() -> {
            long latency = System.currentTimeMillis() - startTime;
            logger.debug("Long polling interno completado para cliente {} em {}ms", clientId, latency);
        });
        
        internalResult.onError(throwable -> {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("Erro no long polling interno para cliente {} após {}ms", clientId, latency, throwable);
            deferredResult.setResult(ResponseEntity.internalServerError().build());
        });
        
        // Adicionar cliente para aguardar
        longPollingManager.addClient(clientId, internalResult);
        logger.debug("Cliente {} adicionado para long polling. Aguardando notificações...", clientId);
        
        return deferredResult;
    }
    
    /**
     * Endpoint para obter estatísticas dos clientes em long polling
     */
    @GetMapping("/stats")
    public ResponseEntity<LongPollingStats> getStats() {
        try {
            int waitingClients = longPollingManager.getWaitingClientsCount();
            LongPollingStats stats = new LongPollingStats(waitingClients);
            
            logger.debug("Estatísticas de long polling: {} clientes aguardando", waitingClients);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de long polling", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint para forçar timeout em todos os clientes (para testes)
     */
    @PostMapping("/force-timeout")
    public ResponseEntity<String> forceTimeout() {
        try {
            longPollingManager.forceTimeoutAllClients();
            logger.info("Timeout forçado para todos os clientes de long polling");
            return ResponseEntity.ok("Timeout forçado com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao forçar timeout", e);
            return ResponseEntity.internalServerError().body("Erro ao forçar timeout");
        }
    }
    
    /**
     * Endpoint para resetar métricas do long polling
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<String> resetMetrics() {
        try {
            metricsService.resetMetrics("long");
            logger.info("Métricas do long polling resetadas");
            return ResponseEntity.ok("Métricas do long polling resetadas com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar métricas do long polling", e);
            return ResponseEntity.internalServerError().body("Erro ao resetar métricas");
        }
    }
    
    /**
     * Classe para estatísticas de long polling
     */
    @lombok.Value
    public static class LongPollingStats {
        int waitingClients;
    }
}
