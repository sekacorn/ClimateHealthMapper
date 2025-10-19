@echo off
REM ClimateHealth API Gateway - Local Development Runner (Windows)
REM This script sets up and runs the API Gateway with Redis for local development

echo ==========================================
echo ClimateHealth API Gateway - Local Setup
echo ==========================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Check if Redis is running
echo Checking for Redis...
docker ps | findstr redis >nul 2>&1
if errorlevel 1 (
    echo Starting Redis container...
    docker run -d --name climate-redis -p 6379:6379 redis:7-alpine redis-server --appendonly yes
    echo Redis started successfully
) else (
    echo Redis is already running
)

REM Wait for Redis to be ready
echo Waiting for Redis to be ready...
timeout /t 5 /nobreak >nul

REM Set environment variables
if "%JWT_SECRET%"=="" set JWT_SECRET=ClimateHealthMapper-LocalDevelopmentSecret-2025
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=dev
if "%REDIS_HOST%"=="" set REDIS_HOST=localhost
if "%REDIS_PORT%"=="" set REDIS_PORT=6379

echo.
echo ==========================================
echo Environment Configuration:
echo ==========================================
echo Profile: %SPRING_PROFILES_ACTIVE%
echo Redis: %REDIS_HOST%:%REDIS_PORT%
echo ==========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if errorlevel 1 (
    echo Error: Maven is not installed. Please install Maven and try again.
    exit /b 1
)

REM Build and run the application
echo Building and starting the API Gateway...
mvn spring-boot:run -Dspring-boot.run.profiles=%SPRING_PROFILES_ACTIVE% -Dspring-boot.run.jvmArguments="-Djwt.secret=%JWT_SECRET%"

pause
