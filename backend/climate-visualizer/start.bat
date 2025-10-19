@echo off
echo ========================================
echo Climate Visualizer Service
echo ========================================
echo.

echo Starting services with Docker Compose...
docker-compose up -d

echo.
echo Waiting for services to be ready...
timeout /t 10 /nobreak > nul

echo.
echo Services started!
echo.
echo PostgreSQL: localhost:5432
echo Redis: localhost:6379
echo Application: http://localhost:8083
echo Health Check: http://localhost:8083/actuator/health
echo.
echo To view logs:
echo   docker-compose logs -f climate-visualizer
echo.
echo To stop services:
echo   docker-compose down
echo.
pause
