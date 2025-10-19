# Climate Integrator - Project Structure

## Complete File Structure

```
climate-integrator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── climate/
│   │   │           └── integrator/
│   │   │               ├── ClimateIntegratorApp.java          # Main application entry point
│   │   │               ├── config/
│   │   │               │   ├── CacheConfig.java               # Redis cache configuration
│   │   │               │   └── SecurityConfig.java            # Spring Security & CORS config
│   │   │               ├── controller/
│   │   │               │   └── DataUploadController.java      # REST API endpoints
│   │   │               ├── exception/
│   │   │               │   ├── DataProcessingException.java   # Custom exception
│   │   │               │   ├── GlobalExceptionHandler.java    # Global error handling
│   │   │               │   └── ResourceNotFoundException.java # Custom exception
│   │   │               ├── model/
│   │   │               │   ├── EnvData.java                   # Environmental data entity
│   │   │               │   ├── GenomicData.java               # Genomic data entity (VCF)
│   │   │               │   └── HealthData.java                # Health data entity (FHIR)
│   │   │               ├── repository/
│   │   │               │   ├── EnvDataRepository.java         # JPA repository for env data
│   │   │               │   ├── GenomicDataRepository.java     # JPA repository for genomic
│   │   │               │   └── HealthDataRepository.java      # JPA repository for health
│   │   │               ├── service/
│   │   │               │   ├── EnvironmentalDataService.java  # CSV/JSON processing
│   │   │               │   ├── GenomicDataService.java        # VCF processing
│   │   │               │   └── HealthDataService.java         # FHIR processing
│   │   │               └── utils/
│   │   │                   └── CsvValidator.java              # Input validation utility
│   │   └── resources/
│   │       └── application.yml                                # Application configuration
│   └── test/
│       └── java/
│           └── com/
│               └── climate/
│                   └── integrator/
│                       └── service/
│                           └── EnvironmentalDataServiceTest.java  # JUnit tests
├── pom.xml                                                    # Maven dependencies
├── Dockerfile                                                 # Docker image definition
├── docker-compose.yml                                         # Multi-container setup
├── init-db.sql                                               # Database initialization
├── .env.example                                              # Environment variables template
├── .gitignore                                                # Git ignore rules
├── README.md                                                 # Comprehensive documentation
├── QUICKSTART.md                                             # Quick start guide
└── PROJECT_STRUCTURE.md                                      # This file
```

## File Descriptions

### Core Application Files

#### `ClimateIntegratorApp.java`
- Main Spring Boot application class
- Entry point for the microservice
- Enables JPA auditing and caching

### Configuration (`config/`)

#### `SecurityConfig.java`
- Spring Security configuration
- CORS settings
- HTTP Basic authentication
- Public endpoints configuration

#### `CacheConfig.java`
- Redis cache manager setup
- Cache TTL configurations per data type
- Serialization settings

### Controllers (`controller/`)

#### `DataUploadController.java`
- REST API endpoints for data upload
- Endpoints for data retrieval
- File upload handling (CSV, JSON, VCF)
- Request/response mapping
- Error handling

**Key Endpoints:**
- `POST /api/integrator/upload/environmental`
- `POST /api/integrator/upload/health`
- `POST /api/integrator/upload/genomic`
- `GET /api/integrator/data/{userId}`
- `GET /api/integrator/health`

### Models (`model/`)

#### `EnvData.java`
- JPA entity for environmental data
- Fields: temperature, air quality (AQI, PM2.5, PM10), precipitation, humidity
- Indexed by userId, location, and measurement date
- Supports NOAA, EPA data formats

#### `HealthData.java`
- JPA entity for FHIR health data
- Supports: Observations, Conditions, Medications, Procedures
- Stores parsed FHIR resources
- Includes raw FHIR JSON for traceability

#### `GenomicData.java`
- JPA entity for VCF genomic variants
- Fields: chromosome, position, alleles, gene annotations
- Climate-relevance flagging
- Clinical significance tracking

### Repositories (`repository/`)

#### `EnvDataRepository.java`
- Spring Data JPA repository
- Custom queries for environmental data
- Geographic and temporal filtering

#### `HealthDataRepository.java`
- Spring Data JPA repository
- FHIR resource type filtering
- Active conditions and medications queries

#### `GenomicDataRepository.java`
- Spring Data JPA repository
- Variant filtering by chromosome, gene, clinical significance
- Climate-relevant variant queries

### Services (`service/`)

#### `EnvironmentalDataService.java`
- CSV and JSON parsing
- Data validation and transformation
- Batch processing
- Caching support

#### `HealthDataService.java`
- FHIR resource parsing using HAPI FHIR
- Supports Bundle and single resources
- Observation, Condition, Medication, Procedure handling
- FHIR R4 standard compliance

#### `GenomicDataService.java`
- VCF file parsing
- Variant annotation extraction
- Climate-relevant gene identification
- Large file batch processing

### Utilities (`utils/`)

#### `CsvValidator.java`
- CSV format validation
- Required field checking
- Data type validation
- Input sanitization
- Security checks

### Exception Handling (`exception/`)

#### `GlobalExceptionHandler.java`
- Centralized exception handling
- Consistent error responses
- Validation error formatting
- Logging

#### `DataProcessingException.java` & `ResourceNotFoundException.java`
- Custom exception types
- Business logic error handling

### Configuration Files

#### `application.yml`
- Spring Boot configuration
- Database connection settings
- Redis cache settings
- File upload limits
- Logging configuration
- Security settings

#### `pom.xml`
- Maven project definition
- Dependencies:
  - Spring Boot 3.2.0
  - PostgreSQL driver
  - Redis client
  - HAPI FHIR 6.10.0
  - BioJava 7.1.0
  - Apache Commons CSV
  - Lombok
  - JUnit 5, Mockito

### Docker Files

#### `Dockerfile`
- Multi-stage build
- Optimized for production
- Non-root user
- Health checks
- JVM optimization

#### `docker-compose.yml`
- Complete stack definition
- PostgreSQL database
- Redis cache
- Optional management tools (PgAdmin, Redis Commander)
- Network and volume configuration

#### `init-db.sql`
- Database initialization script
- Extension setup
- Schema creation
- Initial configurations

### Documentation

#### `README.md`
- Comprehensive project documentation
- API reference
- Configuration guide
- Deployment instructions

#### `QUICKSTART.md`
- Quick start guide
- Example API calls
- Common use cases
- Troubleshooting

#### `PROJECT_STRUCTURE.md`
- This file
- Complete project overview
- File descriptions

### Development Files

#### `.gitignore`
- Git ignore patterns
- Build artifacts
- IDE files
- Local configurations

#### `.env.example`
- Environment variable template
- Configuration examples
- Security settings

### Tests

#### `EnvironmentalDataServiceTest.java`
- JUnit 5 tests
- Mockito mocking
- CSV/JSON processing tests
- Validation tests
- Coverage for edge cases

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                          │
│                (DataUploadController)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                   Service Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Environmental│  │   Health     │  │   Genomic    │     │
│  │   Service    │  │   Service    │  │   Service    │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │             │
│    CSV/JSON          HAPI FHIR          VCF Parser         │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼─────────────┐
│                   Repository Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   EnvData    │  │  HealthData  │  │ GenomicData  │     │
│  │  Repository  │  │  Repository  │  │  Repository  │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼─────────────┐
│                    Data Layer                               │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │  PostgreSQL  │  │    Redis     │                        │
│  │   Database   │  │    Cache     │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack Summary

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.0 |
| Database | PostgreSQL | 15 |
| Cache | Redis | 7 |
| FHIR | HAPI FHIR | 6.10.0 |
| Genomics | BioJava | 7.1.0 |
| Build | Maven | 3.9+ |
| Container | Docker | Latest |
| Testing | JUnit 5 + Mockito | Latest |

## Key Features

- Multi-format data ingestion (CSV, JSON, FHIR, VCF)
- Comprehensive validation and error handling
- Caching for improved performance
- Batch processing for large files
- RESTful API design
- Docker containerization
- Database indexing for query optimization
- Climate-relevant gene identification
- FHIR R4 compliance
- Security with Spring Security
- Comprehensive test coverage

## Next Steps for Development

1. Add authentication with JWT
2. Implement data export functionality
3. Add data visualization endpoints
4. Create integration tests
5. Add Swagger/OpenAPI documentation
6. Implement rate limiting
7. Add metrics and monitoring
8. Create CI/CD pipeline
9. Add data validation rules engine
10. Implement data versioning
