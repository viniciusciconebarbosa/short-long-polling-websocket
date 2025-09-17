package com.example.realtimecomparison.service;

import com.example.realtimecomparison.entity.Notification;
import com.example.realtimecomparison.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    public NotificationRepository notificationRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LongPollingManager longPollingManager;
    
    @Autowired
    private MetricsService metricsService;
    
    private int notificationCounter = 0;
    
    /**
     * Gera notificações automaticamente a cada 5 segundos
     */
    @Scheduled(fixedRate = 5000)
    public void createRandomNotification() {
        notificationCounter++;
        
        // Criar notificação com mensagem aleatória
        String[] messages = {
            "Nova notificação #" + notificationCounter,
            "Sistema atualizado - " + Instant.now().toString(),
            "Alerta de segurança detectado",
            "Backup concluído com sucesso",
            "Usuário conectado: user" + ThreadLocalRandom.current().nextInt(1000, 9999),
            "Processo finalizado: " + ThreadLocalRandom.current().nextInt(1, 100),
            "Memória utilizada: " + ThreadLocalRandom.current().nextInt(60, 95) + "%",
            "Temperatura do servidor: " + ThreadLocalRandom.current().nextInt(35, 75) + "°C"
        };
        
        String randomMessage = messages[ThreadLocalRandom.current().nextInt(messages.length)];
        Notification notification = new Notification(randomMessage);
        
        // Salvar no banco
        notification = notificationRepository.save(notification);
        logger.info("Nova notificação criada: {}", notification);
        
        // Notificar clientes em long polling
        notifyLongPollingClients(notification);
        
        // Enviar via WebSocket
        notifyWebSocketClients(notification);
        
        // Atualizar métricas
        metricsService.incrementNotificationCount("websocket");
    }
    
    /**
     * Notifica clientes em long polling
     */
    private void notifyLongPollingClients(Notification notification) {
        try {
            longPollingManager.notifyClients(notification);
            logger.debug("Clientes de long polling notificados sobre: {}", notification.getId());
        } catch (Exception e) {
            logger.error("Erro ao notificar clientes de long polling", e);
        }
    }
    
    /**
     * Notifica clientes via WebSocket
     */
    private void notifyWebSocketClients(Notification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            logger.debug("Clientes WebSocket notificados sobre: {}", notification.getId());
        } catch (Exception e) {
            logger.error("Erro ao notificar clientes WebSocket", e);
        }
    }
    
    /**
     * Busca notificações criadas após um timestamp
     */
    public List<Notification> getNotificationsAfter(Instant since) {
        return notificationRepository.findNotificationsAfter(since);
    }
    
    /**
     * Busca todas as notificações não entregues
     */
    public List<Notification> getUndeliveredNotifications() {
        return notificationRepository.findUndeliveredNotifications();
    }
    
    /**
     * Busca as últimas N notificações
     */
    public List<Notification> getLatestNotifications(int limit) {
        return notificationRepository.findLatestNotifications().stream()
                .limit(limit)
                .toList();
    }
    
    /**
     * Marca notificação como entregue
     */
    public void markAsDelivered(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setDelivered(true);
            notificationRepository.save(notification);
        });
    }
    
    /**
     * Conta notificações criadas após um timestamp
     */
    public long countNotificationsAfter(Instant since) {
        return notificationRepository.countNotificationsAfter(since);
    }
}
