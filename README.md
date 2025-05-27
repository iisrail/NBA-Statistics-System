# NBA Statistics System

A scalable backend system for logging and retrieving NBA player statistics with real-time capabilities.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13-blue)
![Redis](https://img.shields.io/badge/Redis-Alpine-red)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

## Features

- ✅ **Real-time statistics ingestion** during live games
- ✅ **Season averages calculation** for players and teams
- ✅ **Live game data integration** with historical data
- ✅ **High availability architecture** with Redis + PostgreSQL
- ✅ **SOLID design principles** with clean architecture
- ✅ **Docker deployment ready** with docker-compose
- ✅ **18 comprehensive tests** ensuring reliability

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.0
- **Database:** PostgreSQL 13 (persistent storage)
- **Cache:** Redis Alpine (live data)
- **Deployment:** Docker, Docker Compose
- **Testing:** JUnit 5, Spring Boot Test

## Quick Start

### Option 1: Using Docker Compose (Recommended)

The easiest way to run the entire system:

```bash
# Clone the repository
git clone https://github.com/iisrail/NBA-Statistics-System.git
cd NBA-Statistics-System

# Start all services (app + database + redis)
docker-compose up --build

# The application will be available at http://localhost:8080
```

### Option 2: Manual Setup (Development)

If you want to run the application manually for development:

#### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker (for dependencies)

#### Step 1: Start Dependencies
```bash
# Start Redis
docker run -d --name nba-redis -p 6379:6379 redis:alpine

# Start PostgreSQL
docker run -d --name nba-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=nbadb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  postgres:13

# Verify services are running
docker ps
```

#### Step 2: Configure Application
Create `application-dev.yml` in `src/main/resources/`:

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/nbadb
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

nba:
  current-season: 2024/25

logging:
  level:
    com.nba.stats: DEBUG
```

#### Step 3: Build and Run
```bash
# Compile the application
mvn clean compile

# Run tests to ensure everything works
mvn test

# Start the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or build JAR and run
mvn clean package
java -jar target/nba-stats-1.0.0.jar --spring.profiles.active=dev
```

#### Step 4: Verify Installation
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### Option 3: IDE Setup (IntelliJ/Eclipse)

1. **Import Project:** Import as Maven project
2. **Set JDK:** Configure Java 17
3. **Start Dependencies:** Run the Docker commands from Step 1 above
4. **Run Configuration:** 
   - Main class: `com.nba.stats.NbaStatsApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Environment variables: Set database and Redis connection details
5. **Run Tests:** Execute all tests to verify setup

## API Documentation

### Submit Live Statistics
```bash
PUT /stat/live/game
Content-Type: application/json

{
    "gameId": 1001,
    "teamId": 10,
    "playerId": 23,
    "points": 25,
    "rebounds": 8,
    "assists": 6,
    "steals": 2,
    "blocks": 1,
    "fouls": 3,
    "turnovers": 2,
    "minutesPlayed": 35.5
}
```

### Get Player Statistics
```bash
GET /stat/player/{playerId}?season=2024/25

# Example
curl http://localhost:8080/stat/player/23?season=2024/25
```

**Response:**
```json
{
    "playerId": 23,
    "playerName": "LeBron James",
    "gamesPlayed": 5,
    "hasLiveGame": false,
    "avgPoints": 25.4,
    "avgRebounds": 8.2,
    "avgAssists": 6.8,
    "avgSteals": 1.4,
    "avgBlocks": 0.8,
    "avgFouls": 2.6,
    "avgTurnovers": 3.2,
    "avgMinutes": 35.8
}
```

### Get Team Statistics
```bash
GET /stat/team/{teamId}?season=2024/25

# Example
curl http://localhost:8080/stat/team/10?season=2024/25
```

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Categories
```bash
# Unit tests only
mvn test -Dtest="*Test"

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# Service layer tests
mvn test -Dtest="*ServiceTest"
```

### Test Coverage
- **18 comprehensive tests** covering all major functionality
- Unit tests for service layer logic
- Integration tests for end-to-end scenarios
- Repository tests for data layer operations

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `production` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/nbadb` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `NBA_CURRENT_SEASON` | Current NBA season | `2024/25` |

### Application Profiles

- **`dev`**: Development profile with debug logging
- **`test`**: Test profile with H2 in-memory database
- **`docker`**: Docker profile for containerized deployment
- **`production`**: Production profile with optimized settings

## Architecture

### High-Level Overview
- **Redis-First Architecture**: Season statistics maintained in Redis for sub-second response times
- **Delta-Based Updates**: Efficient incremental updates using Redis atomic operations
- **Hybrid Storage**: Redis for hot data, PostgreSQL for durability
- **SOLID Principles**: Clean architecture with proper separation of concerns

### Key Components
- **Ingestion Service**: Processes live statistics with delta calculation
- **Retrieval Service**: Serves player/team statistics with caching
- **Repository Layer**: Redis and PostgreSQL data access
- **Background Sync**: Ensures data consistency between Redis and PostgreSQL

For detailed architecture documentation, see [docs/architecture.md](docs/architecture.md).

## Deployment

### Local Development
```bash
# Start dependencies only
docker-compose up postgres redis

# Run application locally
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Full Docker Deployment
```bash
# Build and start everything
docker-compose up --build

# Run in background
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Production Deployment
See [docs/architecture.md](docs/architecture.md) for AWS deployment guidelines.

## Troubleshooting

### Common Issues

**Problem**: Application fails to start
```bash
# Check if dependencies are running
docker ps

# Check application logs
docker-compose logs app

# Verify database connection
docker exec -it nba-postgres psql -U postgres -d nbadb -c "\dt"
```

**Problem**: Redis connection issues
```bash
# Test Redis connection
docker exec -it nba-redis redis-cli ping

# Should return: PONG
```

**Problem**: Tests failing
```bash
# Run tests with debug output
mvn test -X

# Run specific failing test
mvn test -Dtest="LiveStatServiceTest#shouldProcessFirstGameStatsCorrectly"
```

### Performance Monitoring
```bash
# Check Redis memory usage
docker exec -it nba-redis redis-cli info memory

# Check PostgreSQL connections
docker exec -it nba-postgres psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
```
