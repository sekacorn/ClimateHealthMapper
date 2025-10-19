#!/bin/bash

# Climate Health Mapper - User Session Service Startup Script

echo "=========================================="
echo "Climate Health Mapper - User Session Service"
echo "=========================================="
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  Warning: .env file not found!"
    echo "Creating from .env.example..."
    cp .env.example .env
    echo "✅ Created .env file. Please update with your configuration."
    echo ""
fi

# Check if PostgreSQL is running
echo "Checking PostgreSQL..."
if ! nc -z localhost 5432 2>/dev/null; then
    echo "❌ PostgreSQL is not running on port 5432"
    echo "Starting with Docker Compose..."
    docker-compose up -d postgres
    echo "Waiting for PostgreSQL to be ready..."
    sleep 10
else
    echo "✅ PostgreSQL is running"
fi

# Check if Redis is running
echo "Checking Redis..."
if ! nc -z localhost 6379 2>/dev/null; then
    echo "❌ Redis is not running on port 6379"
    echo "Starting with Docker Compose..."
    docker-compose up -d redis
    echo "Waiting for Redis to be ready..."
    sleep 5
else
    echo "✅ Redis is running"
fi

echo ""
echo "Starting User Session Service..."
echo "=========================================="
echo ""

# Load environment variables
export $(cat .env | grep -v '^#' | xargs)

# Start the application
mvn spring-boot:run
