package com.example.realtimecomparison.repository;

import com.example.realtimecomparison.entity.PerformanceMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetrics, String> {
    
    /**
     * Busca métricas por técnica
     */
    Optional<PerformanceMetrics> findByTechnique(String technique);
}
