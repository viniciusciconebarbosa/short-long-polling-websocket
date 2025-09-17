package com.example.realtimecomparison.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String message;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private boolean delivered = false;
    
    public Notification(String message) {
        this.message = message;
        this.createdAt = Instant.now();
        this.delivered = false;
    }
}
