package com.example.realtimecomparison.repository;

import com.example.realtimecomparison.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Busca notificações criadas após um timestamp específico
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsAfter(@Param("since") Instant since);
    
    /**
     * Busca notificações não entregues
     */
    @Query("SELECT n FROM Notification n WHERE n.delivered = false ORDER BY n.createdAt ASC")
    List<Notification> findUndeliveredNotifications();
    
    /**
     * Conta notificações criadas após um timestamp específico
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt > :since")
    long countNotificationsAfter(@Param("since") Instant since);
    
    /**
     * Busca as últimas N notificações
     */
    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findLatestNotifications();
}
