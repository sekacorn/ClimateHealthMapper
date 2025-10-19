# Climate Integrator Microservice

A Spring Boot microservice for integrating environmental, health, and genomic data for the ClimateHealthMapper platform.

## Overview

The Climate Integrator microservice handles the ingestion, validation, and storage of multiple data types:

- **Environmental Data**: CSV/JSON files from NOAA, EPA, and other climate data sources
- **Health Data**: FHIR-formatted health records (Observations, Conditions, Medications, Procedures)
- **Genomic Data**: VCF files containing genetic variants

## Features

- Multi-format data ingestion (CSV, JSON, FHIR, VCF)
- Comprehensive data validation
- RESTful API endpoints
- PostgreSQL for persistent storage
- Redis for caching
- Docker support
- Comprehensive error handling and logging
- JUnit test coverage

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Database**: PostgreSQL
- **Cache**: Redis
- **FHIR**: HAPI FHIR 6.10.0
- **Genomics**: BioJava 7.1.0
- **Build Tool**: Maven
- **Containerization**: Docker

## API Endpoints

### Data Upload

#### Upload Environmental Data
```
POST /api/integrator/upload/environmental
Parameters:
  - file: MultipartFile (CSV or JSON)
  - userId: String
  - dataSource: String (NOAA, EPA, etc.)
```

#### Upload Health Data
```
POST /api/integrator/upload/health
Parameters:
  - file: MultipartFile (FHIR JSON)
  - userId: String
```

#### Upload Genomic Data
```
POST /api/integrator/upload/genomic
Parameters:
  - file: MultipartFile (VCF)
  - userId: String
```

### Data Retrieval

#### Get All Data for User
```
GET /api/integrator/data/{userId}
```

#### Get Environmental Data
```
GET /api/integrator/data/{userId}/environmental
```

#### Get Health Data
```
GET /api/integrator/data/{userId}/health
```

#### Get Genomic Data
```
GET /api/integrator/data/{userId}/genomic
```

#### Get Climate-Relevant Variants
```
GET /api/integrator/data/{userId}/genomic/climate-relevant
```

#### Health Check
```
GET /api/integrator/health
```

## Data Formats

### Environmental Data CSV Example
```csv
date,latitude,longitude,temperature,aqi,pm25,pm10,ozone,no2,humidity,precipitation
2025-01-15,40.7128,-74.0060,15.5,45,12.3,20.5,0.05,15.2,65,2.5
```

### FHIR Health Data Example
```json
{
  "resourceType": "Observation",
  "id": "obs-123",
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "8867-4",
      "display": "Heart rate"
    }]
  },
  "valueQuantity": {
    "value": 75,
    "unit": "beats/minute"
  }
}
```

### VCF Genomic Data Example
```
#CHROM  POS     ID      REF ALT QUAL    FILTER  INFO    FORMAT  SAMPLE1
chr1    12345   rs123   A   G   99      PASS    GENE=HSP70;IMPACT=MODERATE   GT:GQ:DP    0/1:99:30
```

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/climate_health_db
  data:
    redis:
      host: localhost
      port: 6379
  servlet:
    multipart:
      max-file-size: 50MB
server:
  port: 8081
```

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+

### Local Development

1. Clone the repository
2. Configure database connection in `application.yml`
3. Run the application:
```bash
mvn spring-boot:run
```

### Using Docker

Build the Docker image:
```bash
docker build -t climate-integrator:1.0.0 .
```

Run the container:
```bash
docker run -p 8081:8081 \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  climate-integrator:1.0.0
```

### Using Docker Compose

```yaml
version: '3.8'
services:
  climate-integrator:
    build: .
    ports:
      - "8081:8081"
    environment:
      DB_HOST: postgres
      REDIS_HOST: redis
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: climate_health_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

## Testing

Run unit tests:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## Security

- Spring Security for authentication
- Input validation and sanitization
- SQL injection prevention via JPA
- File size limits
- Content type validation

## Monitoring

The service exposes health and metrics endpoints:
- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

## Error Handling

The service provides detailed error responses:

```json
{
  "success": false,
  "error": "CSV validation failed: Missing required header: date",
  "timestamp": 1642345678901
}
```

## Database Schema

### Environmental Data Table
- Stores temperature, air quality, precipitation, and other environmental measurements
- Indexed by userId, location, and measurement date

### Health Data Table
- Stores parsed FHIR resources
- Supports Observations, Conditions, Medications, and Procedures
- Indexed by userId, resource type, and observation date

### Genomic Data Table
- Stores VCF variants
- Climate-relevant gene flagging
- Indexed by userId, chromosome, position, and gene name

## Contributing

1. Follow Java coding standards
2. Write unit tests for new features
3. Update documentation
4. Submit pull requests for review

## License

Copyright 2025 ClimateHealthMapper Team

## Contact

For questions or support, contact the ClimateHealthMapper development team.
