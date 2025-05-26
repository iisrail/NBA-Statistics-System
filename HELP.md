# NBA Stats System - Deployment Guide

## 🚀 Easy Docker Compose Deployment

### Quick Start (Assignment Requirement: Docker Compose)
```bash
# 1. Clone and navigate to project
git clone <repository-url>
cd nba-stats-system

# 2. Deploy entire system with one command
docker compose up -d

# 3. Verify deployment
curl http://localhost:8080/stat/player/23  # LeBron James stats
curl http://localhost:8080/stat/team/10    # Lakers team stats

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   NBA Stats API │    │   PostgreSQL    │    │      Redis      │
│   (Port 8080)   │◄──►│   (Port 5432)   │    │   (Port 6379)   │
│                 │    │                 │    │                 │
│ • Live Stats    │    │ • Player Data   │    │ • Live Cache    │
│ • Season Stats  │    │ • Team Data     │    │ • Game Stats    │
│ • REST API      │    │ • Historical    │    │ • Session Data  │
└─────────────────┘    └─────────────────┘    └─────────────────┘

Docker Services

nba-stats-app: Java Spring Boot API
postgres: Database with sample NBA data
redis: Cache for live game statistics

Sample Data Included

Players: LeBron James (23), Stephen Curry (30), Kevin Durant (35)
Teams: Lakers (10), Warriors (15), Celtics (25)
Historical: 2023/24 season statistics

API Testing Examples
bash# Get player season averages
curl http://localhost:8080/stat/player/23

# Get team season averages  
curl http://localhost:8080/stat/team/10

# Send live game statistics
curl -X PUT http://localhost:8080/stat/live/game \
  -H "Content-Type: application/json" \
  -d '{
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
  }'

# Check updated stats
curl http://localhost:8080/stat/player/23
Management Commands
bash# View service status
docker compose ps

# View logs
docker compose logs -f

# Stop services
docker compose down

# Restart services
docker compose restart

# Clean up (remove volumes)
docker compose down -v

