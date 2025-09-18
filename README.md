# Comparação de Técnicas de Comunicação em Tempo Real

Esta aplicação Spring Boot demonstra as diferenças práticas entre **Short Polling**, **Long Polling** e **WebSocket** em cenários reais de notificações em tempo real.

##  Objetivo

Simular cenários reais, permitindo comparação visual de:

- **Volume de requests**
- **Latência de resposta**
- **Uso de recursos de rede**
- **Eficiência de comunicação**

##  Arquitetura

### Backend (Spring Boot)

- **Banco H2** em memória para simular dados reais
- **Geração automática** de notificações a cada 5 segundos
- **Métricas de performance** em tempo real
- **APIs REST** para cada técnica

### Frontend (HTML/JavaScript)

- **Dashboard interativo** com comparação visual
- **Gráficos em tempo real** usando Chart.js
- **Controles independentes** para cada técnica
- **Log de notificações** em tempo real

## Como Executar

### Pré-requisitos

- Java 21+
- Maven 3.6+

### Execução

```bash
# Clonar o repositório
git clone <repository-url>
cd realtime-comparison

# Executar a aplicação
mvn spring-boot:run
```

### Acesso

- **Dashboard**: http://localhost:8080
- **Console H2**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: `password`

##  Funcionalidades

### 1. Short Polling

- **Endpoint**: `/api/short-polling/notifications`
- **Frequência**: Configurável (padrão: 5 segundos)
- **Características**:
  - Muitos requests HTTP
  - Latência alta (até 5s de delay)
  - Alto overhead de rede

### 2. Long Polling

- **Endpoint**: `/api/long-polling/notifications`
- **Timeout**: 30 segundos
- **Características**:
  - Poucos requests HTTP
  - Latência média (instantâneo quando há dados)
  - Médio overhead de rede

### 3. WebSocket

- **Endpoint**: `/ws`
- **Tópico**: `/topic/notifications`
- **Características**:
  - Apenas 1 request inicial
  - Latência baixíssima (milissegundos)
  - Baixo overhead após conexão

## APIs Disponíveis

### Short Polling

```bash
# Buscar notificações
GET /api/short-polling/notifications?since=2024-01-01T00:00:00Z

# Últimas notificações
GET /api/short-polling/notifications/latest?limit=10

# Contar notificações
GET /api/short-polling/notifications/count?since=2024-01-01T00:00:00Z
```

### Long Polling

```bash
# Aguardar notificações (até 30s)
GET /api/long-polling/notifications?clientId=client123

# Estatísticas
GET /api/long-polling/stats
```

### WebSocket

```bash
# Conectar via WebSocket
ws://localhost:8080/ws

# Enviar notificação manual
POST /api/websocket/send-notification
{
  "message": "Notificação manual"
}

# Histórico
GET /api/websocket/notifications/history?limit=50
```

### Métricas

```bash
# Todas as métricas
GET /api/metrics

# Métricas específicas
GET /api/metrics/short
GET /api/metrics/long
GET /api/metrics/websocket

# Resumo
GET /api/metrics/summary

# Comparação
GET /api/metrics/comparison
```

## Demonstração

### Cenários de Teste

1. **Iniciar todas as técnicas** e observar:
   
   - Short Polling: Muitos requests a cada 5s
   - Long Polling: Poucos requests, aguarda dados
   - WebSocket: Conexão persistente, notificações instantâneas

2. **Comparar latência**:
   
   - Short: 0-5000ms (depende do timing)
   - Long: 0-30s (instantâneo quando há dados)
   - WebSocket: <10ms (quase instantâneo)

3. **Analisar uso de rede**:
   
   - Short: Headers HTTP repetidos
   - Long: Headers HTTP ocasionais
   - WebSocket: Headers apenas na conexão inicial

### Métricas Importantes

- **Request Count**: Número de requisições HTTP
- **Notification Count**: Número de notificações recebidas
- **Average Latency**: Latência média em milissegundos
- **Waiting Clients**: Clientes aguardando (Long Polling)

## Interface

O dashboard oferece:

- **Controles independentes** para cada técnica
- **Gráficos em tempo real** de performance
- **Log de notificações** com timestamp
- **Indicadores visuais** de status
- **Métricas comparativas** side-by-side



##  Tecnologias Utilizadas

- **Spring Boot**
- **Spring WebSocket**
- **Spring Data JPA**
- **H2 Database**
- **Bootstrap 5**

## Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abra um Pull Request
