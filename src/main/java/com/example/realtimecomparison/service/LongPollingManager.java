package com.example.realtimecomparison.service;

import com.example.realtimecomparison.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LongPollingManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LongPollingManager.class);
    private final Map<String, DeferredResult<List<Notification>>> waitingClients = new ConcurrentHashMap<>();
    
    /**
     * Adiciona um cliente para long polling
     */
    public void addClient(String clientId, DeferredResult<List<Notification>> result) {
        waitingClients.put(clientId, result);
        logger.debug("Cliente {} adicionado para long polling. Total de clientes: {}", 
                    clientId, waitingClients.size());
        
        // Configurar timeout
        result.onTimeout(() -> {
            logger.debug("Timeout para cliente {}", clientId);
            waitingClients.remove(clientId);
            result.setResult(List.of()); // Retorna lista vazia em caso de timeout
        });
        
        result.onCompletion(() -> {
            logger.debug("Cliente {} completou long polling", clientId);
            waitingClients.remove(clientId);
        });
        
        result.onError(throwable -> {
            logger.error("Erro no long polling para cliente {}", clientId, throwable);
            waitingClients.remove(clientId);
        });
    }
    
    /**
     * Notifica todos os clientes em espera com uma nova notificação
     */
    public void notifyClients(Notification notification) {
        if (waitingClients.isEmpty()) {
            logger.debug("Nenhum cliente em espera para notificar");
            return;
        }
        
        List<Notification> notifications = List.of(notification);
        int notifiedCount = 0;
        
        for (Map.Entry<String, DeferredResult<List<Notification>>> entry : waitingClients.entrySet()) {
            String clientId = entry.getKey();
            DeferredResult<List<Notification>> result = entry.getValue();
            
            try {
                if (!result.isSetOrExpired()) {
                    result.setResult(notifications);
                    notifiedCount++;
                    logger.debug("Cliente {} notificado com nova notificação", clientId);
                } else {
                    logger.debug("Cliente {} já expirou ou foi completado", clientId);
                }
            } catch (Exception e) {
                logger.error("Erro ao notificar cliente {}", clientId, e);
            }
        }
        
        logger.info("Notificados {} clientes de long polling", notifiedCount);
    }
    
    /**
     * Notifica todos os clientes com uma lista de notificações
     */
    public void notifyClients(List<Notification> notifications) {
        if (waitingClients.isEmpty() || notifications.isEmpty()) {
            logger.debug("Nenhum cliente em espera ou notificações vazias");
            return;
        }
        
        int notifiedCount = 0;
        
        for (Map.Entry<String, DeferredResult<List<Notification>>> entry : waitingClients.entrySet()) {
            String clientId = entry.getKey();
            DeferredResult<List<Notification>> result = entry.getValue();
            
            try {
                if (!result.isSetOrExpired()) {
                    result.setResult(notifications);
                    notifiedCount++;
                    logger.debug("Cliente {} notificado com {} notificações", clientId, notifications.size());
                } else {
                    logger.debug("Cliente {} já expirou ou foi completado", clientId);
                }
            } catch (Exception e) {
                logger.error("Erro ao notificar cliente {}", clientId, e);
            }
        }
        
        logger.info("Notificados {} clientes de long polling com {} notificações", 
                   notifiedCount, notifications.size());
    }
    
    /**
     * Remove um cliente específico
     */
    public void removeClient(String clientId) {
        DeferredResult<List<Notification>> result = waitingClients.remove(clientId);
        if (result != null) {
            logger.debug("Cliente {} removido do long polling", clientId);
        }
    }
    
    /**
     * Retorna o número de clientes em espera
     */
    public int getWaitingClientsCount() {
        return waitingClients.size();
    }
    
    /**
     * Limpa todos os clientes em espera
     */
    public void clearAllClients() {
        int count = waitingClients.size();
        waitingClients.clear();
        logger.info("Removidos {} clientes de long polling", count);
    }
    
    /**
     * Força timeout em todos os clientes
     */
    public void forceTimeoutAllClients() {
        for (Map.Entry<String, DeferredResult<List<Notification>>> entry : waitingClients.entrySet()) {
            String clientId = entry.getKey();
            DeferredResult<List<Notification>> result = entry.getValue();
            
            try {
                if (!result.isSetOrExpired()) {
                    result.setResult(List.of());
                    logger.debug("Timeout forçado para cliente {}", clientId);
                }
            } catch (Exception e) {
                logger.error("Erro ao forçar timeout para cliente {}", clientId, e);
            }
        }
        
        waitingClients.clear();
        logger.info("Timeout forçado para todos os clientes de long polling");
    }
}
