# Climate Visualizer Microservice - Project Structure

## Complete File Structure

```
climate-visualizer/
├── pom.xml                                    # Maven configuration with all dependencies
├── Dockerfile                                 # Multi-stage Docker build
├── docker-compose.yml                         # Complete stack (PostgreSQL, Redis, App)
├── .gitignore                                 # Git ignore patterns
├── .dockerignore                              # Docker ignore patterns
├── README.md                                  # Complete documentation
├── API_EXAMPLES.md                            # API usage examples
├── PROJECT_STRUCTURE.md                       # This file
├── start.bat                                  # Windows startup script
├── start.sh                                   # Unix/Linux startup script
│
├── src/
│   ├── main/
│   │   ├── java/com/climate/visualizer/
│   │   │   ├── ClimateVisualizerApp.java              # Main Spring Boot application
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── VisualizationController.java       # REST API endpoints
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── VisualizationService.java          # Core visualization logic
│   │   │   │   ├── ExportService.java                 # Export to PNG/SVG/STL
│   │   │   │   ├── ResourceMonitorService.java        # System resource monitoring
│   │   │   │   └── MbtiStyleService.java              # MBTI styling (all 16 types)
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── ClimateMap.java                    # Climate data JPA entity
│   │   │   │   ├── HealthMap.java                     # Health risk JPA entity
│   │   │   │   └── Visualization.java                 # Visualization JPA entity
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   └── VisualizationRepository.java       # JPA data access
│   │   │   │
│   │   │   └── utils/
│   │   │       ├── ResourceMonitor.java               # System resource utilities
│   │   │       └── MultithreadingManager.java         # Dynamic thread management
│   │   │
│   │   └── resources/
│   │       └── application.yml                         # Application configuration
│   │
│   └── test/
│       ├── java/com/climate/visualizer/
│       │   └── service/
│       │       └── VisualizationServiceTest.java      # JUnit 5 tests
│       │
│       └── resources/
│           └── application-test.yml                    # Test configuration
│
├── exports/                                   # Generated export files (created at runtime)
└── logs/                                      # Application logs (created at runtime)
```

## Key Components Overview

### 1. Main Application
- **ClimateVisualizerApp.java**: Entry point with Spring Boot configuration

### 2. Controllers (API Layer)
- **VisualizationController.java**: REST endpoints for all visualization operations
  - POST `/api/visualizer/generate/climate-map`
  - POST `/api/visualizer/generate/health-heatmap`
  - GET `/api/visualizer/visualizations/{userId}`
  - GET `/api/visualizer/visualization/{id}`
  - POST `/api/visualizer/export/{id}`
  - GET `/api/visualizer/resources/check`
  - DELETE `/api/visualizer/visualization/{id}`

### 3. Services (Business Logic)
- **VisualizationService.java**:
  - Generates 3D climate maps
  - Creates health risk heatmaps
  - Produces Three.js-compatible JSON
  - Manages data point generation
  - Handles geometry and materials

- **ExportService.java**:
  - PNG export (raster images)
  - SVG export (vector graphics)
  - STL export (3D printing)
  - File management
  - Metadata tracking

- **ResourceMonitorService.java**:
  - CPU usage monitoring
  - Memory tracking
  - GPU detection
  - Load balancing
  - Resource availability checks

- **MbtiStyleService.java**:
  - Styling for all 16 MBTI types
  - Custom color schemes
  - Material properties
  - Heatmap gradients
  - Personalization

### 4. Models (Data Layer)
- **ClimateMap.java**:
  - Temperature, precipitation, humidity
  - Wind speed, air quality
  - 3D mesh data
  - Geospatial coordinates

- **HealthMap.java**:
  - Health risk scores
  - Disease risk factors
  - Population data
  - Risk categorization

- **Visualization.java**:
  - User visualizations
  - MBTI preferences
  - Export metadata
  - Sharing settings

### 5. Repository
- **VisualizationRepository.java**: JPA queries and data access

### 6. Utilities
- **ResourceMonitor.java**: Low-level system monitoring
- **MultithreadingManager.java**: Dynamic thread pool management

## Technology Stack

### Core Framework
- Spring Boot 3.2.0
- Java 17
- Maven 3.9+

### Database & Caching
- PostgreSQL 15 (primary database)
- Redis 7 (caching)
- JPA/Hibernate (ORM)

### Libraries
- **Jackson**: JSON processing
- **Lombok**: Boilerplate reduction
- **Commons Math3**: Scientific calculations
- **OSHI**: System monitoring
- **Batik**: SVG generation

### Testing
- JUnit 5
- Mockito
- H2 (in-memory test database)

### DevOps
- Docker & Docker Compose
- Spring Actuator (monitoring)
- SLF4J/Logback (logging)

## Configuration Files

### application.yml
Main configuration including:
- Database connection
- Redis configuration
- Resource limits
- Visualization settings
- Export settings
- MBTI configuration
- Logging settings

### application-test.yml
Test-specific configuration with:
- H2 in-memory database
- Disabled caching
- Test resource limits

### docker-compose.yml
Complete stack setup:
- PostgreSQL container
- Redis container
- Application container
- Volume management
- Network configuration

## Build & Deployment

### Maven Build
```bash
mvn clean package
```

Produces: `target/climate-visualizer-1.0.0-SNAPSHOT.jar`

### Docker Build
```bash
docker build -t climate-visualizer:latest .
```

### Docker Compose
```bash
docker-compose up -d
```

## Data Flow

1. **Request**: Client sends visualization request
2. **Validation**: Coordinates and MBTI type validated
3. **Data Generation**: Climate/health data points generated
4. **Processing**: Parallel processing using multithreading
5. **Styling**: MBTI-specific styling applied
6. **Scene Building**: Three.js-compatible scene created
7. **Storage**: Visualization saved to PostgreSQL
8. **Caching**: Results cached in Redis
9. **Response**: JSON visualization data returned

## Export Flow

1. **Request**: Client requests export in specific format
2. **Retrieval**: Visualization fetched from database
3. **Rendering**: Data rendered to target format
4. **File Creation**: Export file generated
5. **Metadata Update**: Export path and timestamp saved
6. **Response**: File path and details returned

## Resource Management

1. **Monitoring**: Continuous CPU/memory/GPU monitoring
2. **Assessment**: Resource availability evaluated
3. **Adjustment**: Thread pool dynamically resized
4. **Throttling**: Processing limited when resources low
5. **Recovery**: Automatic recovery when resources available

## MBTI Personalization

Each of the 16 MBTI types has unique:
- Primary color scheme
- Material type (Standard, Phong, Lambert)
- Emissive colors
- Opacity levels
- Wireframe preferences
- Metalness and roughness
- Heatmap gradients
- Interpolation types
- Contrast and saturation

## API Endpoints Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/visualizer/generate/climate-map` | POST | Generate 3D climate visualization |
| `/api/visualizer/generate/health-heatmap` | POST | Generate health risk heatmap |
| `/api/visualizer/visualizations/{userId}` | GET | Get user's visualizations |
| `/api/visualizer/visualization/{id}` | GET | Get specific visualization |
| `/api/visualizer/export/{id}` | POST | Export to PNG/SVG/STL |
| `/api/visualizer/resources/check` | GET | Check system resources |
| `/api/visualizer/visualization/{id}` | DELETE | Delete visualization |
| `/actuator/health` | GET | Health check |
| `/actuator/metrics` | GET | Application metrics |

## Performance Features

1. **Caching**: Redis-based caching for repeated requests
2. **Multithreading**: Parallel processing of data points
3. **Dynamic Scaling**: Thread pool adjusts to system load
4. **Resource Monitoring**: Prevents resource exhaustion
5. **Batch Processing**: Efficient handling of large datasets
6. **Connection Pooling**: Database connection management

## Security Features

1. **Input Validation**: Coordinate and parameter validation
2. **Non-root User**: Docker container runs as non-root
3. **Resource Limits**: Prevents resource abuse
4. **CORS Configuration**: Configurable cross-origin policies
5. **SQL Injection Protection**: JPA parameterized queries

## Scalability

1. **Horizontal Scaling**: Multiple instances supported
2. **Database Indexing**: Optimized queries
3. **Caching Layer**: Reduces database load
4. **Async Processing**: Non-blocking operations
5. **Resource Management**: Efficient resource utilization

## Monitoring & Observability

1. **Health Checks**: `/actuator/health`
2. **Metrics**: `/actuator/metrics`
3. **Prometheus**: Metrics export enabled
4. **Logging**: Comprehensive logging with levels
5. **Resource Monitoring**: Real-time system metrics

## Development Workflow

1. **Local Development**: Run with `mvn spring-boot:run`
2. **Testing**: `mvn test`
3. **Building**: `mvn clean package`
4. **Docker Development**: `docker-compose up`
5. **Production Deployment**: Docker image to registry

## Future Enhancements

Potential additions:
- WebSocket support for real-time updates
- GraphQL API
- Advanced caching strategies
- Machine learning integration
- More export formats (GeoJSON, KML)
- Interactive 3D viewer
- Time-series animations
- Collaborative features

## Dependencies Version Summary

| Dependency | Version |
|------------|---------|
| Spring Boot | 3.2.0 |
| Java | 17 |
| PostgreSQL | 15+ |
| Redis | 7+ |
| OSHI | 6.4.8 |
| Commons Math3 | 3.6.1 |
| Batik | 1.17 |
| JUnit | 5.x |

## Getting Started Checklist

- [ ] Install Java 17+
- [ ] Install Maven 3.8+
- [ ] Install Docker & Docker Compose
- [ ] Clone repository
- [ ] Review configuration in `application.yml`
- [ ] Run `docker-compose up -d`
- [ ] Access http://localhost:8083/actuator/health
- [ ] Test API with examples from `API_EXAMPLES.md`
- [ ] Review logs: `docker-compose logs -f`
- [ ] Run tests: `mvn test`

## Support & Documentation

- **README.md**: General overview and setup
- **API_EXAMPLES.md**: Complete API examples
- **PROJECT_STRUCTURE.md**: This file
- **Javadoc**: In-code documentation
- **Logs**: `logs/climate-visualizer.log`

---

**Project Status**: Production Ready
**Version**: 1.0.0-SNAPSHOT
**Last Updated**: 2024
**License**: Copyright ClimateHealthMapper Team
