# ClimateHealth API Gateway - Quick Start Guide

Get the API Gateway up and running in minutes!

## Prerequisites

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.9** or higher ([Download](https://maven.apache.org/download.cgi))
- **Docker** ([Download](https://www.docker.com/get-started))
- **Redis** (will be started via Docker)

## Quick Start Options

### Option 1: Using the Helper Script (Recommended)

#### Windows
```bash
run-local.bat
```

#### Linux/Mac
```bash
chmod +x run-local.sh
./run-local.sh
```

This script will:
1. Check if Docker is running
2. Start Redis in a Docker container
3. Configure environment variables
4. Build and run the API Gateway

### Option 2: Docker Compose (Complete Stack)

```bash
docker-compose up -d
```

This will start:
- API Gateway (port 8080)
- Redis (port 6379)

### Option 3: Manual Setup

#### Step 1: Start Redis
```bash
docker run -d --name climate-redis -p 6379:6379 redis:7-alpine
```

#### Step 2: Set Environment Variables

**Windows (PowerShell)**:
```powershell
$env:JWT_SECRET="ClimateHealthMapper-LocalSecret-2025"
$env:SPRING_PROFILES_ACTIVE="dev"
```

**Linux/Mac**:
```bash
export JWT_SECRET="ClimateHealthMapper-LocalSecret-2025"
export SPRING_PROFILES_ACTIVE="dev"
```

#### Step 3: Run the Application
```bash
mvn spring-boot:run
```

## Verify Installation

### 1. Check Health Status
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Test Public Endpoint
```bash
curl http://localhost:8080/api/session/health
```

### 3. Check Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

## Testing with Postman

1. Import the Postman collection:
   - Open Postman
   - Click "Import"
   - Select `postman-collection.json`

2. Set the environment variable:
   - Create a new environment in Postman
   - Add variable: `base_url` = `http://localhost:8080`

3. Test the endpoints:
   - Start with "Health Check" request
   - Try "User Login" to get a JWT token
   - Test protected endpoints with the token

## Common Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | (required) | Secret key for JWT signing |
| `SPRING_PROFILES_ACTIVE` | default | Active Spring profile (dev, docker, prod) |
| `REDIS_HOST` | localhost | Redis server hostname |
| `REDIS_PORT` | 6379 | Redis server port |
| `JWT_REQUIRE_MFA` | true | Require MFA validation |

### Profiles

**Development (`dev`)**:
- Verbose logging (TRACE level)
- All actuator endpoints exposed
- Detailed error messages
- CORS enabled for localhost

**Docker (`docker`)**:
- Uses Docker service names for microservices
- INFO level logging
- Redis hostname: `redis`

**Production (`prod`)**:
- WARN level logging
- Minimal actuator endpoints
- Redis SSL enabled
- Strict CORS policy

## Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/climate/gateway/
│   │   │   ├── ApiGatewayApp.java          # Main application
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java     # Security configuration
│   │   │   │   ├── GatewayConfig.java      # Route configuration
│   │   │   │   └── RedisConfig.java        # Redis configuration
│   │   │   ├── filter/
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT validation
│   │   │   │   └── RateLimitingFilter.java       # Rate limiting
│   │   │   ├── controller/
│   │   │   │   └── FallbackController.java       # Circuit breaker fallbacks
│   │   │   └── exception/
│   │   │       └── BusinessException.java        # Custom exceptions
│   │   └── resources/
│   │       └── application.yml             # Configuration
│   └── test/                               # Unit tests
├── Dockerfile                              # Docker build
├── docker-compose.yml                      # Docker Compose setup
├── pom.xml                                 # Maven dependencies
├── README.md                               # Full documentation
├── QUICK_START.md                          # This file
└── API_DOCUMENTATION.md                    # API documentation
```

## Building for Production

### Build JAR
```bash
mvn clean package
```

The JAR file will be created at: `target/api-gateway-1.0.0.jar`

### Build Docker Image
```bash
docker build -t climate-api-gateway:1.0.0 .
```

### Run in Production Mode
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=your-production-secret \
  -e REDIS_HOST=your-redis-host \
  -e REDIS_PASSWORD=your-redis-password \
  climate-api-gateway:1.0.0
```

## Troubleshooting

### Port Already in Use
If port 8080 is already in use, change it in `application.yml` or set:
```bash
SERVER_PORT=8081 mvn spring-boot:run
```

### Redis Connection Failed
Ensure Redis is running:
```bash
docker ps | grep redis
```

If not running, start it:
```bash
docker start climate-redis
```

Or create a new container:
```bash
docker run -d --name climate-redis -p 6379:6379 redis:7-alpine
```

### JWT Validation Failed
Ensure all services use the same JWT secret. Check:
```bash
echo $JWT_SECRET
```

### Circuit Breaker Always Open
Backend services might be unavailable. Check their status and ensure they're running.

## Next Steps

1. **Review API Documentation**: See `API_DOCUMENTATION.md` for complete API reference
2. **Configure Microservices**: Update service URLs in `application.yml`
3. **Set Up OAuth2/SSO**: Configure OAuth2 settings if using SSO
4. **Enable Monitoring**: Set up Prometheus and Grafana for metrics
5. **Deploy to Production**: Follow deployment guide in `README.md`

## Getting Help

- **Documentation**: See `README.md` and `API_DOCUMENTATION.md`
- **API Testing**: Use the Postman collection in `postman-collection.json`
- **Issues**: Check logs in `logs/api-gateway.log`

## Development Tips

### Hot Reload
Use Spring Boot DevTools for hot reload during development:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### Debug Mode
Run with debug logging:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.root=DEBUG"
```

### View Logs
```bash
tail -f logs/api-gateway.log
```

## Security Reminder

**IMPORTANT**: Before deploying to production:

1. Change the default JWT secret
2. Use strong, unique passwords for Redis
3. Enable HTTPS/TLS
4. Configure appropriate CORS origins
5. Review and update security configurations
6. Enable MFA for sensitive operations
7. Set up proper logging and monitoring

---

**Happy Coding!** 🚀
