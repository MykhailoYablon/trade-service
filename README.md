# Trade Service

A comprehensive Spring Boot application that provides real-time trading capabilities through Interactive Brokers (IB) TWS API integration, market data streaming via Finnhub, and WebSocket-based real-time updates.

## 🚀 Features

### Core Trading Functionality
- **Interactive Brokers Integration**: Full TWS API integration for order placement, position tracking, and account management
- **Real-time Market Data**: Live stock quotes and trade data via Finnhub WebSocket API
- **Order Management**: Place, track, and manage trading orders
- **Position Tracking**: Real-time position monitoring and historical data
- **Account Management**: Account information and portfolio tracking

### Technical Features
- **WebSocket Support**: Real-time data streaming for live market updates
- **Redis Caching**: High-performance data caching for market data
- **H2 Database**: In-memory database for development and testing
- **RESTful API**: Comprehensive REST endpoints for all trading operations
- **Server-Sent Events**: Real-time updates to frontend applications

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.3.0
- **Language**: Java 17
- **Database**: H2 (in-memory)
- **Cache**: Redis
- **API Integration**: 
  - Interactive Brokers TWS API
  - Finnhub Financial API
- **Build Tool**: Gradle
- **Libraries**: 
  - Lombok for boilerplate reduction
  - MapStruct for object mapping
  - WebFlux for reactive programming

### Project Structure
```
src/main/java/com/example/tradeservice/
├── configuration/          # Configuration classes
├── controller/            # REST API controllers
├── entity/               # JPA entities
├── handler/              # WebSocket handlers
├── mapper/               # Object mappers
├── model/                # Data models and DTOs
├── repository/           # Data access layer
└── service/              # Business logic services
```

## 🛠️ Setup & Installation

### Prerequisites
- Java 17 or higher
- Gradle 7.0 or higher
- Redis server (for caching)
- Interactive Brokers TWS (for live trading)
- Finnhub API token

### Configuration

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd trade-service
   ```

2. **Configure application.yml**
   ```yaml
   financial:
     api:
       token: ${FINANCIAL_API_TOKEN:your-finnhub-token-here}
   
   spring:
     data:
       redis:
         host: 127.0.0.1
         port: 6379
   
   server:
     port: 8081
   ```

3. **Set environment variables**
   ```bash
   export FINANCIAL_API_TOKEN=your_finnhub_api_token
   ```

4. **Start Redis server**
   ```bash
   redis-server
   ```

5. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

### TWS Configuration
- Ensure Interactive Brokers TWS is running
- Enable API connections in TWS settings
- Default connection: `127.0.0.1:7497` (paper trading)
- For live trading, use port `7496`

## 📡 API Endpoints

### Trade Data Endpoints
```
GET  /trades?symbol={symbol}           # Get latest trade data
GET  /trades/{symbol}/price            # Get latest price
GET  /trades/all                       # Get all latest trades
GET  /trades/{symbol}/history          # Get trade history
GET  /trades/{symbol}/history/range    # Get trades in date range
GET  /trades/stream                    # SSE stream for real-time updates
POST /trades/subscribe/{symbol}        # Subscribe to symbol
POST /trades/unsubscribe/{symbol}      # Unsubscribe from symbol
GET  /trades/status                    # Market status
GET  /trades/quote?symbol={symbol}     # Get quote
GET  /trades/search?symbol={symbol}    # Search symbols
```

### Order Management
```
POST /orders                           # Place order
GET  /orders                           # Get all orders
```

### Position Management
```
GET  /positions                        # Get all positions
GET  /positions/{id}                   # Get position by ID
GET  /positions/{conid}/historical/{timeframe}  # Get historical data
```

### Account Management
```
GET  /accounts                         # Get account information
```

### Contract Management
```
GET  /contracts                        # Get all contracts
GET  /contracts/{id}                   # Get contract by ID
```

## 🔌 WebSocket Integration

The service supports WebSocket connections for real-time data streaming:

- **Connection**: `ws://localhost:8081/websocket`
- **Real-time trade updates**: Subscribe to symbols for live trade data
- **Market data streaming**: Continuous price and volume updates

## 💾 Data Storage

### H2 Database
- In-memory database for development
- Access H2 console at: `http://localhost:8081/h2-console`
- Auto-created tables for positions, orders, and historical data

### Redis Cache
- Caches market data and frequently accessed information
- Configurable retention periods
- Improves response times for repeated requests

## 🔧 Development

### Building the Project
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Code Generation
The project uses MapStruct for object mapping. Generated sources are in:
```
build/generated/sources/annotationProcessor/java
```

## 📊 Monitoring & Logging

- **Application logs**: Spring Boot default logging
- **TWS connection status**: Real-time connection monitoring
- **WebSocket connections**: Session management and error handling
- **API performance**: Request/response logging

## 🔒 Security Considerations

- **API Token Management**: Use environment variables for sensitive tokens
- **CORS Configuration**: Configured for cross-origin requests
- **Input Validation**: Request parameter validation
- **Error Handling**: Comprehensive error responses

## 🚨 Important Notes

1. **TWS Connection**: Ensure TWS is running and API connections are enabled
2. **Paper Trading**: Default configuration uses paper trading port (7497)
3. **API Limits**: Be aware of Finnhub API rate limits
4. **Data Retention**: Historical data is stored in H2 database
5. **WebSocket Reconnection**: Automatic reconnection handling for WebSocket connections

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
