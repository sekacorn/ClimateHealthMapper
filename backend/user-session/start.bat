@echo off
REM Climate Health Mapper - User Session Service Startup Script (Windows)

echo ==========================================
echo Climate Health Mapper - User Session Service
echo ==========================================
echo.

REM Check if .env file exists
if not exist .env (
    echo Warning: .env file not found!
    echo Creating from .env.example...
    copy .env.example .env
    echo Created .env file. Please update with your configuration.
    echo.
)

echo Checking PostgreSQL...
netstat -an | findstr :5432 >nul
if errorlevel 1 (
    echo PostgreSQL is not running on port 5432
    echo Please start PostgreSQL or run: docker-compose up -d postgres
) else (
    echo PostgreSQL is running
)

echo Checking Redis...
netstat -an | findstr :6379 >nul
if errorlevel 1 (
    echo Redis is not running on port 6379
    echo Please start Redis or run: docker-compose up -d redis
) else (
    echo Redis is running
)

echo.
echo Starting User Session Service...
echo ==========================================
echo.

REM Start the application
mvn spring-boot:run
