package com.example.realtimecomparison.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "performance_metrics")
@Data
@NoArgsConstructor
public class PerformanceMetrics {
    
    @Id
    @Column(name = "technique", nullable = false)
    private String technique; // "short", "long", "websocket"
    
    @Column(name = "request_count", nullable = false)
    private long requestCount = 0;
    
    @Column(name = "total_latency", nullable = false)
    private long totalLatency = 0;
    
    @Column(name = "notification_count", nullable = false)
    private long notificationCount = 0;
    
    @Column(name = "last_update", nullable = false)
    private Instant lastUpdate = Instant.now();
    
    public PerformanceMetrics(String technique) {
        this.technique = technique;
        this.lastUpdate = Instant.now();
    }
    
    // Métodos auxiliares
    public void incrementRequestCount() {
        this.requestCount++;
        this.lastUpdate = Instant.now();
    }
    
    public void addLatency(long latency) {
        this.totalLatency += latency;
        this.lastUpdate = Instant.now();
    }
    
    public void incrementNotificationCount() {
        this.notificationCount++;
        this.lastUpdate = Instant.now();
    }
    
    public double getAverageLatency() {
        return requestCount > 0 ? (double) totalLatency / requestCount : 0.0;
    }
}
