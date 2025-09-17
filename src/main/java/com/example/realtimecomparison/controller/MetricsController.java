package com.example.realtimecomparison.controller;

import com.example.realtimecomparison.entity.PerformanceMetrics;
import com.example.realtimecomparison.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * Obtém todas as métricas de performance
     */
    @GetMapping
    public ResponseEntity<List<PerformanceMetrics>> getAllMetrics() {
        try {
            List<PerformanceMetrics> metrics = metricsService.getAllMetrics();
            logger.debug("Métricas solicitadas: {} técnicas encontradas", metrics.size());
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Erro ao obter métricas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém métricas de uma técnica específica
     */
    @GetMapping("/{technique}")
    public ResponseEntity<PerformanceMetrics> getMetrics(@PathVariable String technique) {
        try {
            Optional<PerformanceMetrics> metrics = metricsService.getMetrics(technique);
            if (metrics.isPresent()) {
                logger.debug("Métricas da técnica '{}' solicitadas", technique);
                return ResponseEntity.ok(metrics.get());
            } else {
                logger.debug("Nenhuma métrica encontrada para a técnica '{}'", technique);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Erro ao obter métricas da técnica '{}'", technique, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém resumo das métricas
     */
    @GetMapping("/summary")
    public ResponseEntity<MetricsService.MetricsSummary> getSummary() {
        try {
            MetricsService.MetricsSummary summary = metricsService.getSummary();
            logger.debug("Resumo das métricas solicitado");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Erro ao obter resumo das métricas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Reseta todas as métricas
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetAllMetrics() {
        try {
            metricsService.resetAllMetrics();
            logger.info("Todas as métricas foram resetadas");
            return ResponseEntity.ok("Todas as métricas foram resetadas com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar todas as métricas", e);
            return ResponseEntity.internalServerError().body("Erro ao resetar métricas");
        }
    }
    
    /**
     * Reseta métricas de uma técnica específica
     */
    @PostMapping("/{technique}/reset")
    public ResponseEntity<String> resetMetrics(@PathVariable String technique) {
        try {
            metricsService.resetMetrics(technique);
            logger.info("Métricas da técnica '{}' foram resetadas", technique);
            return ResponseEntity.ok("Métricas da técnica '" + technique + "' foram resetadas com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao resetar métricas da técnica '{}'", technique, e);
            return ResponseEntity.internalServerError().body("Erro ao resetar métricas");
        }
    }
    
    /**
     * Obtém estatísticas comparativas das três técnicas
     */
    @GetMapping("/comparison")
    public ResponseEntity<ComparisonStats> getComparison() {
        try {
            List<PerformanceMetrics> allMetrics = metricsService.getAllMetrics();
            
            PerformanceMetrics shortMetrics = allMetrics.stream()
                    .filter(m -> "short".equals(m.getTechnique()))
                    .findFirst()
                    .orElse(new PerformanceMetrics("short"));
            
            PerformanceMetrics longMetrics = allMetrics.stream()
                    .filter(m -> "long".equals(m.getTechnique()))
                    .findFirst()
                    .orElse(new PerformanceMetrics("long"));
            
            PerformanceMetrics websocketMetrics = allMetrics.stream()
                    .filter(m -> "websocket".equals(m.getTechnique()))
                    .findFirst()
                    .orElse(new PerformanceMetrics("websocket"));
            
            ComparisonStats comparison = new ComparisonStats(
                new TechniqueStats("Short Polling", shortMetrics),
                new TechniqueStats("Long Polling", longMetrics),
                new TechniqueStats("WebSocket", websocketMetrics)
            );
            
            logger.debug("Estatísticas comparativas solicitadas");
            return ResponseEntity.ok(comparison);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas comparativas", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Classe para estatísticas de uma técnica
     */
    @lombok.Value
    public static class TechniqueStats {
        String name;
        long requestCount;
        long notificationCount;
        double averageLatency;
        String lastUpdate;
        
        public TechniqueStats(String name, PerformanceMetrics metrics) {
            this.name = name;
            this.requestCount = metrics.getRequestCount();
            this.notificationCount = metrics.getNotificationCount();
            this.averageLatency = metrics.getAverageLatency();
            this.lastUpdate = metrics.getLastUpdate().toString();
        }
    }
    
    /**
     * Classe para estatísticas comparativas
     */
    @lombok.Value
    public static class ComparisonStats {
        TechniqueStats shortPolling;
        TechniqueStats longPolling;
        TechniqueStats websocket;
    }
}
