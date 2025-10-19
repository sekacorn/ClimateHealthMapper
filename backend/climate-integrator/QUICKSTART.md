# Climate Integrator - Quick Start Guide

This guide will help you get the Climate Integrator microservice up and running quickly.

## Prerequisites

- Docker and Docker Compose installed
- Java 17+ (for local development)
- Maven 3.6+ (for local development)

## Quick Start with Docker Compose (Recommended)

1. **Navigate to the project directory:**
   ```bash
   cd backend/climate-integrator
   ```

2. **Create environment file:**
   ```bash
   cp .env.example .env
   ```

3. **Start all services:**
   ```bash
   docker-compose up -d
   ```

   This will start:
   - Climate Integrator service (port 8081)
   - PostgreSQL database (port 5432)
   - Redis cache (port 6379)

4. **Verify services are running:**
   ```bash
   docker-compose ps
   ```

5. **Check health:**
   ```bash
   curl http://localhost:8081/api/integrator/health
   ```

   Expected response:
   ```json
   {
     "status": "UP",
     "service": "climate-integrator",
     "timestamp": 1642345678901
   }
   ```

## Testing the API

### 1. Upload Environmental Data (CSV)

Create a test CSV file `env-data.csv`:
```csv
date,latitude,longitude,temperature,aqi,pm25
2025-01-15,40.7128,-74.0060,15.5,45,12.3
2025-01-16,40.7128,-74.0060,14.2,52,15.8
```

Upload the file:
```bash
curl -X POST http://localhost:8081/api/integrator/upload/environmental \
  -u admin:admin \
  -F "file=@env-data.csv" \
  -F "userId=user123" \
  -F "dataSource=NOAA"
```

### 2. Upload Health Data (FHIR JSON)

Create a test FHIR file `health-data.json`:
```json
{
  "resourceType": "Observation",
  "id": "obs-123",
  "effectiveDateTime": "2025-01-15T10:00:00Z",
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

Upload the file:
```bash
curl -X POST http://localhost:8081/api/integrator/upload/health \
  -u admin:admin \
  -F "file=@health-data.json" \
  -F "userId=user123"
```

### 3. Upload Genomic Data (VCF)

Create a test VCF file `genomic-data.vcf`:
```
##fileformat=VCFv4.2
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	SAMPLE1
chr1	12345	rs123	A	G	99	PASS	GENE=HSP70;IMPACT=MODERATE	GT:GQ:DP	0/1:99:30
chr2	67890	rs456	C	T	95	PASS	GENE=GPX1;IMPACT=HIGH	GT:GQ:DP	1/1:95:25
```

Upload the file:
```bash
curl -X POST http://localhost:8081/api/integrator/upload/genomic \
  -u admin:admin \
  -F "file=@genomic-data.vcf" \
  -F "userId=user123"
```

### 4. Retrieve Integrated Data

Get all data for a user:
```bash
curl -X GET http://localhost:8081/api/integrator/data/user123 \
  -u admin:admin
```

Get only environmental data:
```bash
curl -X GET http://localhost:8081/api/integrator/data/user123/environmental \
  -u admin:admin
```

Get climate-relevant genomic variants:
```bash
curl -X GET http://localhost:8081/api/integrator/data/user123/genomic/climate-relevant \
  -u admin:admin
```

## Local Development (Without Docker)

### 1. Start PostgreSQL and Redis

```bash
# PostgreSQL
docker run -d --name climate-postgres \
  -e POSTGRES_DB=climate_health_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15

# Redis
docker run -d --name climate-redis \
  -p 6379:6379 redis:7-alpine
```

### 2. Build and Run

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

Or run with Java directly:
```bash
java -jar target/climate-integrator-1.0.0.jar
```

## Run Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# With coverage
mvn clean test jacoco:report
```

## View Logs

### Docker Compose
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f climate-integrator
```

### Local Development
Logs are written to `logs/climate-integrator.log`

## Stopping Services

### Docker Compose
```bash
# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Optional Management Tools

Start PgAdmin and Redis Commander:
```bash
docker-compose --profile tools up -d
```

Access:
- PgAdmin: http://localhost:5050 (admin@climate.com / admin)
- Redis Commander: http://localhost:8082

## Troubleshooting

### Port Already in Use
If port 8081 is already in use, change it in `.env`:
```
SERVER_PORT=8082
```

### Database Connection Issues
Check if PostgreSQL is running:
```bash
docker-compose ps postgres
```

View PostgreSQL logs:
```bash
docker-compose logs postgres
```

### Redis Connection Issues
Check if Redis is running:
```bash
docker-compose ps redis
```

Test Redis connection:
```bash
docker exec -it climate-redis redis-cli ping
```

### Application Errors
Check application logs:
```bash
docker-compose logs climate-integrator
```

## Next Steps

1. **Configure Security**: Update security credentials in `.env`
2. **Review Configuration**: Check `application.yml` for customization
3. **Add Data**: Upload real environmental, health, and genomic data
4. **Integration**: Connect with other microservices
5. **Monitoring**: Set up Prometheus/Grafana for monitoring

## API Documentation

For detailed API documentation, see the main README.md file.

## Support

For issues or questions, contact the ClimateHealthMapper development team.
