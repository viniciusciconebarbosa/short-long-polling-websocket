// Dashboard JavaScript - Comparação de Técnicas de Comunicação em Tempo Real
// Abordagem Funcional com ES6+

// Variáveis globais
let shortPollingInterval = null;
let longPollingActive = false;
let websocket = null;
let stompClient = null;
let chart = null;
let notificationCount = 0;

// Métricas das técnicas
const metrics = {
    short: { requests: 0, notifications: 0, latency: 0 },
    long: { requests: 0, notifications: 0, latency: 0 },
    ws: { messages: 0, notifications: 0, latency: 0 }
};

// Inicialização quando a página carregar
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar componentes e carregar dados
    initializeEventListeners();
    initializeChart();
    loadInitialData();
    initializeTooltips();
    
    // Atualizar dados a cada 2 segundos
    setInterval(updateRealtimeData, 2000);
});

// Configurar event listeners com abordagem mais concisa
const initializeEventListeners = () => {
    // Mapeamento de botões e suas funções
    const buttonHandlers = {
        'start-short': startShortPolling,
        'stop-short': stopShortPolling,
        'start-long': startLongPolling,
        'stop-long': stopLongPolling,
        'start-ws': connectWebSocket,
        'stop-ws': disconnectWebSocket,
        'startAll': startAll,
        'stopAll': stopAll,
        'resetAll': resetAll,
        'exportChart': exportChart,
        'clearNotifications': clearNotifications
    };
    
    // Adicionar event listeners para todos os botões
    Object.entries(buttonHandlers).forEach(([id, handler]) => {
        document.getElementById(id)?.addEventListener('click', handler);
    });
}

// Inicializar gráfico
const initializeChart = () => {
    const ctx = document.getElementById('performanceChart').getContext('2d');
    const chartConfig = {
        type: 'bar',
        data: {
            labels: ['Short Polling', 'Long Polling', 'WebSocket'],
            datasets: [
                {
                    label: 'Requests/Messages',
                    data: [0, 0, 0],
                    backgroundColor: ['#dc3545', '#ffc107', '#28a745'],
                    borderColor: ['#dc3545', '#ffc107', '#28a745'],
                    borderWidth: 1
                }, 
                {
                    label: 'Latência Média (ms)',
                    data: [0, 0, 0],
                    backgroundColor: ['#ff6b6b', '#ffd93d', '#6bcf7f'],
                    borderColor: ['#ff6b6b', '#ffd93d', '#6bcf7f'],
                    borderWidth: 1,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Quantidade' }
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    title: { display: true, text: 'Latência (ms)' },
                    grid: { drawOnChartArea: false }
                }
            },
            plugins: {
                title: { display: true, text: 'Comparação de Performance em Tempo Real' }
            }
        }
    };
    
    chart = new Chart(ctx, chartConfig);
};

// Carregar dados iniciais
const loadInitialData = async () => {
    try {
        const { metrics, latestNotifications } = await fetch('/api/dashboard/data')
            .then(response => response.json());
            
        updateMetrics(metrics);
        updateNotificationsLog(latestNotifications);
    } catch (error) {
        console.error('Erro ao carregar dados iniciais:', error);
    }
};

// Atualizar dados em tempo real
const updateRealtimeData = async () => {
    try {
        const { metrics } = await fetch('/api/dashboard/realtime')
            .then(response => response.json());
        
        if (metrics?.techniqueMetrics) {
            // Atualizar métricas do servidor usando destructuring
            metrics.techniqueMetrics.forEach(({ technique: tech, requestCount, notificationCount, averageLatency }) => {
                if (metrics[tech]) {
                    Object.assign(metrics[tech], {
                        requests: requestCount || 0,
                        notifications: notificationCount || 0,
                        latency: averageLatency || 0
                    });
                }
            });
            
            // Atualizar displays
            updateAllDisplays();
        }
        
        // Atualizar estatísticas do Long Polling
        updateLongPollingStats();
    } catch (error) {
        console.error('Erro ao atualizar dados em tempo real:', error);
    }
};

// Atualizar estatísticas do Long Polling
async function updateLongPollingStats() {
    try {
        const response = await fetch('/api/long-polling/stats');
        const stats = await response.json();
        
        const waitingElement = document.getElementById('long-waiting');
        if (waitingElement) {
            waitingElement.textContent = stats.waitingClients || 0;
        }
    } catch (error) {
        console.error('Erro ao atualizar estatísticas do Long Polling:', error);
    }
}

// Inicializar tooltips do Bootstrap
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

// ==================== SHORT POLLING ====================

const startShortPolling = () => {
    if (shortPollingInterval) return;
    
    const interval = parseInt(document.getElementById('short-interval').value);
    updateStatus('short', 'active', 'Ativo');
    
    // Fazer primeira requisição imediatamente
    performShortPolling();
    
    // Depois fazer a cada intervalo
    shortPollingInterval = setInterval(performShortPolling, interval);
    
    toggleButtons('short', true);
};

const stopShortPolling = () => {
    if (shortPollingInterval) {
        clearInterval(shortPollingInterval);
        shortPollingInterval = null;
    }
    
    updateStatus('short', 'inactive', 'Inativo');
    toggleButtons('short', false);
};

const performShortPolling = async () => {
    const startTime = Date.now();
    
    try {
        const response = await fetch('/api/short-polling/notifications');
        const notifications = await response.json();
        
        const latency = Date.now() - startTime;
        // Atualizar o display local para feedback imediato
        updateLocalMetrics('short', { requests: 1, notifications: notifications.length, latency });
        updateChart();
        
        if (notifications.length > 0) {
            addToNotificationsLog('Short Polling', notifications);
        }
    } catch (error) {
        console.error('Erro no short polling:', error);
    }
};

// ==================== LONG POLLING ====================

const startLongPolling = async () => {
    if (longPollingActive) return;
    
    longPollingActive = true;
    updateStatus('long', 'waiting', 'Aguardando');
    toggleButtons('long', true);
    
    await performLongPolling();
};

const stopLongPolling = () => {
    longPollingActive = false;
    updateStatus('long', 'inactive', 'Inativo');
    toggleButtons('long', false);
};

const performLongPolling = async () => {
    if (!longPollingActive) return;
    
    const startTime = Date.now();
    const clientId = 'client-' + Date.now();
    
    try {
        updateStatus('long', 'active', 'Ativo');
        
        const response = await fetch(`/api/long-polling/notifications?clientId=${clientId}`);
        const notifications = await response.json();
        
        const latency = Date.now() - startTime;
        updateLocalMetrics('long', { requests: 1, notifications: notifications.length, latency });
        updateChart();
        
        if (notifications.length > 0) {
            addToNotificationsLog('Long Polling', notifications);
        }
        
        // Se ainda está ativo, continuar long polling
        if (longPollingActive) {
            updateStatus('long', 'waiting', 'Aguardando');
            setTimeout(performLongPolling, 50);
        }
    } catch (error) {
        console.error('Erro no long polling:', error);
        if (longPollingActive) {
            updateStatus('long', 'waiting', 'Aguardando');
            setTimeout(performLongPolling, 1000);
        }
    }
};

// ==================== WEBSOCKET ====================

function connectWebSocket() {
    if (stompClient && stompClient.connected) return;
    
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        
        stompClient.connect({}, function(frame) {
            console.log('WebSocket conectado:', frame);
            updateStatus('ws', 'active', 'Conectado');
            updateConnectionStatus('ws', 'Conectado', 'success');
            toggleButtons('ws', true);
            
            // Subscrever ao tópico de notificações
            stompClient.subscribe('/topic/notifications', function(message) {
                const notification = JSON.parse(message.body);
                // Não atualizar métricas aqui - o backend já registra
                // Apenas atualizar o display local para feedback imediato
                updateLocalMetrics('ws', { messages: 1, notifications: 1, latency: 0 });
                updateChart();
                addToNotificationsLog('WebSocket', [notification]);
            });
            
        }, function(error) {
            console.error('Erro na conexão WebSocket:', error);
            updateStatus('ws', 'inactive', 'Erro');
            updateConnectionStatus('ws', 'Erro', 'danger');
        });
        
    } catch (error) {
        console.error('Erro ao conectar WebSocket:', error);
        updateStatus('ws', 'inactive', 'Erro');
    }
}

function disconnectWebSocket() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    
    updateStatus('ws', 'inactive', 'Desconectado');
    updateConnectionStatus('ws', 'Fechada', 'secondary');
    toggleButtons('ws', false);
}

// ==================== CONTROLES GLOBAIS ====================

function startAll() {
    startShortPolling();
    startLongPolling();
    connectWebSocket();
}

function stopAll() {
    stopShortPolling();
    stopLongPolling();
    disconnectWebSocket();
}

async function resetAll() {
    stopAll();
    
    try {
        await fetch('/api/dashboard/reset', { method: 'POST' });
        resetMetrics();
        updateAllDisplays();
        clearNotificationsLog();
    } catch (error) {
        console.error('Erro ao resetar:', error);
    }
}

// ==================== FUNÇÕES DE FORMATAÇÃO ====================

function formatNumber(number) {
    if (number === 0) return '0';
    
    if (number < 1000) {
        return number.toString();
    } else if (number < 1000000) {
        return (number / 1000).toFixed(1) + 'K';
    } else if (number < 1000000000) {
        return (number / 1000000).toFixed(1) + 'M';
    } else {
        return (number / 1000000000).toFixed(1) + 'B';
    }
}

function formatLatency(latencyMs) {
    if (latencyMs === 0) return '0ms';
    
    if (latencyMs < 1000) {
        return `${Math.round(latencyMs)}ms`;
    } else if (latencyMs < 60000) {
        const seconds = (latencyMs / 1000).toFixed(1);
        return `${seconds}s`;
    } else {
        const minutes = Math.floor(latencyMs / 60000);
        const seconds = Math.floor((latencyMs % 60000) / 1000);
        return `${minutes}m${seconds}s`;
    }
}

function formatPercentage(value, total) {
    if (total === 0) return '0%';
    const percentage = ((value / total) * 100).toFixed(1);
    return `${percentage}%`;
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// ==================== FUNÇÕES DE UI ====================

function updateStatus(technique, status, text) {
    const statusElement = document.getElementById(`${technique}-status`);
    const indicator = statusElement.querySelector('.status-indicator');
    
    indicator.className = `status-indicator status-${status}`;
    statusElement.innerHTML = `<span class="status-indicator status-${status}"></span>${text}`;
}

function updateConnectionStatus(technique, text, type) {
    const element = document.getElementById(`${technique}-connection`);
    element.textContent = text;
    element.className = `badge bg-${type}`;
}

// Atualizar métricas locais (apenas para feedback visual)
function updateLocalMetrics(technique, newMetrics) {
    Object.assign(metrics[technique], newMetrics);
    updateTechniqueDisplay(technique);
}

// Atualizar métricas do servidor (dados reais)
function updateMetrics(technique, newMetrics) {
    if (typeof technique === 'string') {
        // Atualizar métricas específicas (não usado mais)
        Object.assign(metrics[technique], newMetrics);
        updateTechniqueDisplay(technique);
    } else {
        // Atualizar todas as métricas (dados do servidor)
        const metricsData = technique.techniqueMetrics || [];
        metricsData.forEach(metric => {
            const tech = metric.technique;
            metrics[tech] = {
                requests: metric.requestCount || 0,
                notifications: metric.notificationCount || 0,
                latency: metric.averageLatency || 0
            };
            updateTechniqueDisplay(tech);
        });
    }
}

function updateTechniqueDisplay(technique) {
    const techniqueMetrics = metrics[technique];
    const prefix = technique === 'ws' ? 'ws' : technique;
    
    document.getElementById(`${prefix}-requests`).textContent = formatNumber(techniqueMetrics.requests || 0);
    document.getElementById(`${prefix}-notifications`).textContent = formatNumber(techniqueMetrics.notifications || 0);
    document.getElementById(`${prefix}-latency`).textContent = formatLatency(techniqueMetrics.latency || 0);
}

function updateChart() {
    if (!chart) return;
    
    // Atualizar dados do gráfico
    chart.data.datasets[0].data = [
        metrics.short.requests || 0,
        metrics.long.requests || 0,
        metrics.ws.messages || 0
    ];
    
    chart.data.datasets[1].data = [
        metrics.short.latency || 0,
        metrics.long.latency || 0,
        metrics.ws.latency || 0
    ];
    
    // Forçar atualização do gráfico com animação suave
    chart.update('active');
}

function updateAllDisplays() {
    updateTechniqueDisplay('short');
    updateTechniqueDisplay('long');
    updateTechniqueDisplay('ws');
    updateChart();
}

function toggleButtons(technique, active) {
    const startBtn = document.getElementById(`start-${technique}`);
    const stopBtn = document.getElementById(`stop-${technique}`);
    
    startBtn.disabled = active;
    stopBtn.disabled = !active;
}

function resetMetrics() {
    metrics.short = { requests: 0, notifications: 0, latency: 0 };
    metrics.long = { requests: 0, notifications: 0, latency: 0 };
    metrics.ws = { messages: 0, notifications: 0, latency: 0 };
    notificationCount = 0;
}

// ==================== FUNÇÕES DE NOTIFICAÇÕES ====================

function addToNotificationsLog(technique, notifications) {
    const log = document.getElementById('notifications-log');
    
    if (log.querySelector('.text-muted')) {
        log.innerHTML = '';
    }
    
    notifications.forEach(notification => {
        const item = document.createElement('div');
        item.className = 'notification-item';
        
        const techniqueColors = {
            'Short Polling': 'danger',
            'Long Polling': 'warning', 
            'WebSocket': 'success',
            'Sistema': 'primary'
        };
        
        const color = techniqueColors[technique] || 'secondary';
        
        const timestamp = new Date(notification.createdAt);
        const timeString = timestamp.toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
        
        item.innerHTML = `
            <div class="d-flex justify-content-between align-items-start mb-2">
                <div class="d-flex align-items-center">
                    <span class="badge bg-${color} me-2">${technique}</span>
                    <small class="text-muted">
                        <i class="fas fa-clock me-1"></i>
                        ${timeString}
                    </small>
                </div>
                <small class="text-muted">#${formatNumber(notification.id)}</small>
            </div>
            <div class="fw-medium">${notification.message}</div>
        `;
        log.insertBefore(item, log.firstChild);
    });
    
    // Manter apenas as últimas 20 notificações
    while (log.children.length > 20) {
        log.removeChild(log.lastChild);
    }
    
    // Atualizar contador
    updateNotificationCount(log.children.length);
}

function updateNotificationsLog(notifications) {
    const log = document.getElementById('notifications-log');
    log.innerHTML = '';
    
    if (notifications.length === 0) {
        clearNotificationsLog();
        return;
    }
    
    notifications.forEach(notification => {
        addToNotificationsLog('Sistema', [notification]);
    });
}

function clearNotificationsLog() {
    const log = document.getElementById('notifications-log');
    log.innerHTML = `
        <div class="text-center text-muted py-5">
            <i class="fas fa-bell-slash fa-3x mb-3 opacity-50"></i>
            <p class="fs-5">Nenhuma notificação ainda...</p>
            <small>As notificações aparecerão aqui em tempo real</small>
        </div>
    `;
    updateNotificationCount(0);
}

function updateNotificationCount(count) {
    const countElement = document.getElementById('notification-count');
    if (countElement) {
        countElement.textContent = formatNumber(count);
    }
}

// ==================== FUNÇÕES AUXILIARES GLOBAIS ====================

function exportChart() {
    if (chart) {
        const url = chart.toBase64Image();
        const link = document.createElement('a');
        link.download = 'performance-chart.png';
        link.href = url;
        link.click();
    }
}

function clearNotifications() {
    clearNotificationsLog();
}

function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}