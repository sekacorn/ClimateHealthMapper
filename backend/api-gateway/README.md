# ClimateHealth API Gateway

The API Gateway serves as the single entry point for all client requests to the ClimateHealthMapper platform. It handles routing, authentication, authorization, rate limiting, and load balancing across microservices.

## Features

### Security
- **JWT Authentication**: Token-based authentication for secure API access
- **OAuth2/SSO Support**: Integration with external identity providers
- **MFA Validation**: Multi-factor authentication support
- **CORS Configuration**: Cross-origin resource sharing for web clients
- **Rate Limiting**: Redis-backed distributed rate limiting

### Resilience
- **Circuit Breaker**: Prevents cascading failures using Resilience4j
- **Retry Logic**: Automatic retry for transient failures
- **Timeout Management**: Configurable timeouts for all services
- **Fallback Mechanisms**: Graceful degradation when services are unavailable

### Routing
The gateway routes requests to the following microservices:

| Path | Service | Port |
|------|---------|------|
| `/api/integrator/**` | Climate Integrator Service | 8081 |
| `/api/visualizer/**` | Climate Visualizer Service | 8082 |
| `/api/session/**` | User Session Service | 8083 |
| `/api/llm/**` | LLM Service | 8084 |
| `/api/collab/**` | Collaboration Service | 8085 |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                          │
│                         (Port 8080)                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ JWT Auth     │  │ Rate Limit   │  │ Circuit      │    │
│  │ Filter       │→ │ Filter       │→ │ Breaker      │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │                    Router                            │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼─────┐        ┌─────▼──────┐      ┌──────▼───────┐
   │Integrator│        │Visualizer  │      │  Session     │
   │Service   │        │Service     │      │  Service     │
   └──────────┘        └────────────┘      └──────────────┘
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Secret key for JWT signing | (must be set) |
| `JWT_REQUIRE_MFA` | Require MFA validation | `true` |
| `REDIS_HOST` | Redis server host | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `REDIS_PASSWORD` | Redis password | (empty) |
| `OAUTH2_ISSUER_URI` | OAuth2 issuer URI for SSO | (empty) |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` |

### Service URLs

Configure microservice URLs using environment variables:

```bash
INTEGRATOR_SERVICE_URL=http://climate-integrator:8081
VISUALIZER_SERVICE_URL=http://climate-visualizer:8082
SESSION_SERVICE_URL=http://user-session:8083
LLM_SERVICE_URL=http://llm-service:8084
COLLABORATION_SERVICE_URL=http://collaboration-service:8085
```

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.9 or higher
- Redis server

### Local Development

1. Start Redis:
```bash
docker run -d -p 6379:6379 redis:latest
```

2. Run the application:
```bash
mvn spring-boot:run
```

Or with a specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker

Build the Docker image:
```bash
docker build -t climate-api-gateway:latest .
```

Run the container:
```bash
docker run -p 8080:8080 \
  -e JWT_SECRET=your-secret-key \
  -e REDIS_HOST=redis \
  -e SPRING_PROFILES_ACTIVE=docker \
  climate-api-gateway:latest
```

### Docker Compose

```yaml
version: '3.8'

services:
  api-gateway:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=${JWT_SECRET}
      - REDIS_HOST=redis
    depends_on:
      - redis
    networks:
      - climate-network

  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    networks:
      - climate-network

networks:
  climate-network:
    driver: bridge
```

## API Documentation

### Public Endpoints (No Authentication Required)

- `POST /api/session/auth/login` - User login
- `POST /api/session/auth/register` - User registration
- `POST /api/session/auth/refresh` - Refresh JWT token
- `POST /api/session/auth/sso/**` - SSO authentication
- `GET /api/session/health` - Session service health check
- `GET /actuator/health` - Gateway health check
- `GET /actuator/prometheus` - Prometheus metrics

### Protected Endpoints (Authentication Required)

All other endpoints require a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt-token>
```

### Rate Limiting

Rate limits are enforced per user (for authenticated requests) or per IP (for unauthenticated requests):

| User Type | Rate Limit |
|-----------|------------|
| Unauthenticated | 100 requests/hour |
| Authenticated | 1000 requests/hour |
| Premium | 5000 requests/hour |

Rate limit information is included in response headers:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in current window
- `X-RateLimit-Reset`: Unix timestamp when the limit resets

## Monitoring

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

Prometheus metrics are available at:
```bash
curl http://localhost:8080/actuator/prometheus
```

### Circuit Breaker Status

Check circuit breaker status:
```bash
curl http://localhost:8080/actuator/health/circuitBreakers
```

## Security Best Practices

1. **Always use HTTPS in production**
2. **Rotate JWT secrets regularly**
3. **Use strong, unique secrets** (minimum 256 bits)
4. **Enable MFA for sensitive operations**
5. **Monitor rate limit violations**
6. **Keep dependencies updated**
7. **Review security logs regularly**

## Testing

Run unit tests:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Ensure Redis is running and accessible
   - Check Redis host and port configuration
   - Verify network connectivity

2. **JWT Validation Failed**
   - Verify JWT secret matches across services
   - Check token expiration
   - Ensure MFA validation if required

3. **Circuit Breaker Open**
   - Check downstream service health
   - Review service logs for errors
   - Wait for circuit breaker to half-open (10 seconds default)

4. **Rate Limit Exceeded**
   - Wait for rate limit window to reset
   - Request rate limit increase if needed
   - Consider upgrading to premium tier

## Contributing

Please follow the SOLID principles and ensure:
- Comprehensive error handling
- SLF4J logging at appropriate levels
- Unit tests for new features
- Documentation for public APIs

## License

Copyright 2025 ClimateHealth Team. All rights reserved.
