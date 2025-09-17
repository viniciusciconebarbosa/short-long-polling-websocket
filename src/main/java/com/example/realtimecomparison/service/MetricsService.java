package com.example.realtimecomparison.service;

import com.example.realtimecomparison.entity.PerformanceMetrics;
import com.example.realtimecomparison.repository.PerformanceMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    @Autowired
    private PerformanceMetricsRepository metricsRepository;
    
    /**
     * Registra uma requisição para uma técnica específica
     */
    public void recordRequest(String technique, long latencyMs) {
        PerformanceMetrics metrics = getOrCreateMetrics(technique);
        metrics.incrementRequestCount();
        metrics.addLatency(latencyMs);
        metricsRepository.save(metrics);
        logger.debug("Métrica registrada - Técnica: {}, Requests: {}, Latência: {}ms", 
                    technique, metrics.getRequestCount(), latencyMs);
    }
    
    /**
     * Incrementa contador de notificações para uma técnica
     */
    public void incrementNotificationCount(String technique) {
        PerformanceMetrics metrics = getOrCreateMetrics(technique);
        metrics.incrementNotificationCount();
        metricsRepository.save(metrics);
        logger.debug("Contador de notificações incrementado - Técnica: {}, Total: {}", 
                    technique, metrics.getNotificationCount());
    }
    
    /**
     * Busca métricas por técnica
     */
    public Optional<PerformanceMetrics> getMetrics(String technique) {
        return metricsRepository.findByTechnique(technique);
    }
    
    /**
     * Busca todas as métricas
     */
    public List<PerformanceMetrics> getAllMetrics() {
        return metricsRepository.findAll();
    }
    
    /**
     * Reseta todas as métricas
     */
    public void resetAllMetrics() {
        metricsRepository.deleteAll();
        logger.info("Todas as métricas foram resetadas");
    }
    
    /**
     * Reseta métricas de uma técnica específica
     */
    public void resetMetrics(String technique) {
        metricsRepository.findByTechnique(technique).ifPresent(metrics -> {
            metricsRepository.delete(metrics);
            logger.info("Métricas da técnica '{}' foram resetadas", technique);
        });
    }
    
    /**
     * Obtém ou cria métricas para uma técnica
     */
    private PerformanceMetrics getOrCreateMetrics(String technique) {
        return metricsRepository.findByTechnique(technique)
                .orElseGet(() -> {
                    PerformanceMetrics newMetrics = new PerformanceMetrics(technique);
                    return metricsRepository.save(newMetrics);
                });
    }
    
    /**
     * Calcula estatísticas resumidas
     */
    public MetricsSummary getSummary() {
        List<PerformanceMetrics> allMetrics = getAllMetrics();
        
        long totalRequests = allMetrics.stream().mapToLong(PerformanceMetrics::getRequestCount).sum();
        long totalNotifications = allMetrics.stream().mapToLong(PerformanceMetrics::getNotificationCount).sum();
        double averageLatency = allMetrics.stream()
                .filter(m -> m.getRequestCount() > 0)
                .mapToDouble(PerformanceMetrics::getAverageLatency)
                .average()
                .orElse(0.0);
        
        return new MetricsSummary(totalRequests, totalNotifications, averageLatency, allMetrics);
    }
    
    /**
     * Classe para resumo das métricas
     */
    @lombok.Value
    public static class MetricsSummary {
        long totalRequests;
        long totalNotifications;
        double averageLatency;
        List<PerformanceMetrics> techniqueMetrics;
    }
}
