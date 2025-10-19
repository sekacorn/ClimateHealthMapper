#!/bin/bash

# ClimateHealth API Gateway - Local Development Runner
# This script sets up and runs the API Gateway with Redis for local development

set -e

echo "=========================================="
echo "ClimateHealth API Gateway - Local Setup"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Check if Redis is running
echo -e "${YELLOW}Checking for Redis...${NC}"
if ! docker ps | grep -q redis; then
    echo -e "${YELLOW}Starting Redis container...${NC}"
    docker run -d \
        --name climate-redis \
        -p 6379:6379 \
        redis:7-alpine \
        redis-server --appendonly yes
    echo -e "${GREEN}Redis started successfully${NC}"
else
    echo -e "${GREEN}Redis is already running${NC}"
fi

# Wait for Redis to be ready
echo -e "${YELLOW}Waiting for Redis to be ready...${NC}"
timeout=30
counter=0
until docker exec climate-redis redis-cli ping > /dev/null 2>&1 || [ $counter -eq $timeout ]; do
    sleep 1
    counter=$((counter + 1))
done

if [ $counter -eq $timeout ]; then
    echo -e "${RED}Error: Redis failed to start within ${timeout} seconds${NC}"
    exit 1
fi
echo -e "${GREEN}Redis is ready${NC}"

# Set environment variables
export JWT_SECRET="${JWT_SECRET:-ClimateHealthMapper-LocalDevelopmentSecret-2025}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"

echo ""
echo "=========================================="
echo "Environment Configuration:"
echo "=========================================="
echo "Profile: ${SPRING_PROFILES_ACTIVE}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "JWT Secret: ${JWT_SECRET:0:20}..."
echo "=========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed. Please install Maven and try again.${NC}"
    exit 1
fi

# Build and run the application
echo -e "${YELLOW}Building and starting the API Gateway...${NC}"
mvn spring-boot:run \
    -Dspring-boot.run.profiles=${SPRING_PROFILES_ACTIVE} \
    -Dspring-boot.run.jvmArguments="-Djwt.secret=${JWT_SECRET}"

# Cleanup on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    if [ "$1" == "--stop-redis" ]; then
        echo -e "${YELLOW}Stopping Redis container...${NC}"
        docker stop climate-redis > /dev/null 2>&1 || true
        docker rm climate-redis > /dev/null 2>&1 || true
        echo -e "${GREEN}Redis stopped${NC}"
    fi
}

trap 'cleanup' EXIT
