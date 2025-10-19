# Climate Visualizer Microservice

A Spring Boot microservice for generating 3D climate visualizations and health risk heatmaps with MBTI-specific styling.

## Features

- **3D Climate Visualization**: Generate Three.js-compatible 3D climate maps
- **Health Risk Heatmaps**: Create interactive health risk visualizations
- **MBTI Styling**: Personalized visual styling for all 16 MBTI types
- **Multi-format Export**: Export to PNG, SVG, and STL formats
- **Resource Monitoring**: Dynamic resource management with CPU/memory/GPU monitoring
- **Multithreading**: Adaptive multithreading based on system resources
- **Caching**: Redis-based caching for improved performance

## Technology Stack

- **Spring Boot 3.2.0**
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **OSHI** - System monitoring
- **Apache Commons Math3** - Mathematical calculations
- **Batik** - SVG generation
- **JUnit 5 & Mockito** - Testing

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+
- Redis 7+
- Docker (optional)

## Getting Started

### 1. Database Setup

```bash
# Create PostgreSQL database
createdb climate_visualizer

# Or using psql
psql -U postgres
CREATE DATABASE climate_visualizer;
```

### 2. Configuration

Update `src/main/resources/application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/climate_visualizer
    username: your_username
    password: your_password
```

### 3. Build and Run

```bash
# Build the application
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/climate-visualizer-1.0.0-SNAPSHOT.jar
```

### 4. Using Docker

```bash
# Build Docker image
docker build -t climate-visualizer:latest .

# Run with Docker
docker run -p 8083:8083 \
  -e DB_PASSWORD=your_password \
  -e REDIS_PASSWORD=your_redis_password \
  climate-visualizer:latest
```

## API Endpoints

### Generate Climate Map
```http
POST /api/visualizer/generate/climate-map
Content-Type: application/json

{
  "userId": "user123",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "radius": 50.0,
  "mbtiType": "INTJ",
  "quality": "medium"
}
```

### Generate Health Heatmap
```http
POST /api/visualizer/generate/health-heatmap
Content-Type: application/json

{
  "userId": "user123",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "radius": 50.0,
  "healthFactors": ["heat_stress", "respiratory"],
  "mbtiType": "ENFP",
  "quality": "medium"
}
```

### Get User Visualizations
```http
GET /api/visualizer/visualizations/{userId}?type=CLIMATE_MAP_3D
```

### Get Specific Visualization
```http
GET /api/visualizer/visualization/{id}
```

### Export Visualization
```http
POST /api/visualizer/export/{id}
Content-Type: application/json

{
  "format": "PNG",
  "options": {
    "width": 1920,
    "height": 1080
  }
}
```

### Check System Resources
```http
GET /api/visualizer/resources/check
```

## MBTI Styling

The service supports personalized styling for all 16 MBTI types:

- **Analysts**: INTJ, INTP, ENTJ, ENTP
- **Diplomats**: INFJ, INFP, ENFJ, ENFP
- **Sentinels**: ISTJ, ISFJ, ESTJ, ESFJ
- **Explorers**: ISTP, ISFP, ESTP, ESFP

Each type has unique:
- Color schemes
- Material properties
- Heatmap gradients
- Opacity and contrast settings

## Quality Levels

- **Low**: 20x20 grid, ~400 data points
- **Medium**: 50x50 grid, ~2,500 data points (default)
- **High**: 100x100 grid, ~10,000 data points

## Export Formats

- **PNG**: Raster image (1920x1080 default)
- **SVG**: Vector graphics
- **STL**: 3D printing format (3D maps only)

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=VisualizationServiceTest

# Run with coverage
mvn test jacoco:report
```

## Resource Management

The service dynamically adjusts resource usage:

- **CPU Monitoring**: Adjusts thread pool size based on usage
- **Memory Management**: Monitors available memory
- **GPU Detection**: Identifies available graphics hardware
- **Load Balancing**: Distributes work based on system capacity

## Monitoring

Access monitoring endpoints:

- Health: `http://localhost:8083/actuator/health`
- Metrics: `http://localhost:8083/actuator/metrics`
- Prometheus: `http://localhost:8083/actuator/prometheus`

## Configuration

Key configuration options in `application.yml`:

```yaml
app:
  resources:
    max-cpu-usage: 85.0
    max-memory-usage: 90.0

  visualization:
    default-quality: medium
    max-data-points: 10000
    cache-enabled: true

  export:
    output-directory: ./exports
    max-export-size-mb: 50
```

## Architecture

```
ClimateVisualizerApp
├── Controller Layer
│   └── VisualizationController
├── Service Layer
│   ├── VisualizationService
│   ├── ExportService
│   ├── MbtiStyleService
│   └── ResourceMonitorService
├── Repository Layer
│   └── VisualizationRepository
├── Model Layer
│   ├── Visualization
│   ├── ClimateMap
│   └── HealthMap
└── Utilities
    ├── ResourceMonitor
    └── MultithreadingManager
```

## Performance Tips

1. Use caching for frequently accessed visualizations
2. Choose appropriate quality level for your needs
3. Monitor system resources regularly
4. Batch export operations when possible
5. Clean up old exports periodically

## Troubleshooting

### High Memory Usage
- Reduce quality level
- Decrease max concurrent tasks
- Increase JVM heap size: `-Xmx2048m`

### Slow Generation
- Check system resources via `/resources/check`
- Reduce data point count
- Enable multithreading if disabled

### Export Failures
- Verify export directory permissions
- Check available disk space
- Ensure required libraries are installed

## License

Copyright (c) 2024 ClimateHealthMapper Team

## Support

For issues and questions:
- Create an issue in the repository
- Contact: support@climatehealthmapper.com
