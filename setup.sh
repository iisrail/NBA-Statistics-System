#!/bin/bash

echo "🏀 NBA Stats System - Docker Setup"
echo "=================================="

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not installed"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not installed"  
    exit 1
fi

echo "✅ Prerequisites check passed"

# Create directories
mkdir -p logs

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose down

# Start services
echo "🚀 Starting NBA Stats services..."
docker-compose up -d

# Wait for services
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check health
echo "🏥 Checking service health..."
curl -f http://localhost:8080/actuator/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ NBA Stats API is healthy"
else
    echo "⚠️  NBA Stats API not ready yet"
fi

echo ""
echo "🎉 NBA Stats System is running!"
echo ""
echo "🌐 API Endpoints:"
echo "   - Health: http://localhost:8080/actuator/health"
echo "   - Player: http://localhost:8080/stat/player/23"
echo "   - Team:   http://localhost:8080/stat/team/10"
echo ""
echo "📋 Commands:"
echo "   - View logs: docker-compose logs -f"
echo "   - Stop:      docker-compose down"
