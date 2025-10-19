# ClimateHealth API Gateway - API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Rate Limiting](#rate-limiting)
4. [Error Handling](#error-handling)
5. [Endpoints](#endpoints)
6. [Response Headers](#response-headers)
7. [Security](#security)

## Overview

The ClimateHealth API Gateway is the single entry point for all client requests to the ClimateHealthMapper platform. It provides:

- **Centralized Authentication & Authorization**: JWT-based with OAuth2/SSO support
- **Rate Limiting**: Redis-backed distributed rate limiting
- **Circuit Breaker**: Resilient microservices communication
- **Request Routing**: Intelligent routing to backend services
- **Monitoring**: Health checks and metrics

**Base URL**: `http://localhost:8080` (development)
**Production URL**: `https://api.climatehealthmapper.com` (production)

## Authentication

### Overview
The API uses JWT (JSON Web Tokens) for authentication. All protected endpoints require a valid JWT token in the Authorization header.

### Public Endpoints (No Authentication Required)
- `POST /api/session/auth/login` - User login
- `POST /api/session/auth/register` - User registration
- `POST /api/session/auth/refresh` - Refresh JWT token
- `POST /api/session/auth/sso/**` - SSO authentication
- `GET /api/session/health` - Session service health check
- `GET /actuator/health` - Gateway health check
- `GET /actuator/prometheus` - Prometheus metrics

### Authentication Flow

#### 1. Login
```http
POST /api/session/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh-token-here",
  "expiresIn": 86400,
  "tokenType": "Bearer"
}
```

#### 2. Using the Token
Include the JWT token in the Authorization header for all protected requests:

```http
GET /api/integrator/climate/data
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 3. Token Refresh
When the token expires, use the refresh token to get a new one:

```http
POST /api/session/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh-token-here"
}
```

### Multi-Factor Authentication (MFA)

For sensitive operations, MFA validation may be required. If MFA is not validated, requests will return:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "error": "Forbidden",
  "message": "MFA validation required",
  "status": 403
}
```

To validate MFA, the JWT token must include the `mfa_validated: true` claim.

## Rate Limiting

### Overview
The API implements rate limiting to prevent abuse and ensure fair resource allocation.

### Rate Limits

| User Type | Rate Limit |
|-----------|------------|
| Unauthenticated (by IP) | 100 requests/hour |
| Authenticated User | 1000 requests/hour |
| Premium User | 5000 requests/hour |

### Rate Limit Headers

Every response includes rate limit information in the headers:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1640995200
```

- `X-RateLimit-Limit`: Maximum requests allowed in the current window
- `X-RateLimit-Remaining`: Remaining requests in the current window
- `X-RateLimit-Reset`: Unix timestamp when the limit resets

### Rate Limit Exceeded

When the rate limit is exceeded, the API returns:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640995200

{
  "error": "Rate Limit Exceeded",
  "message": "You have exceeded the rate limit of 1000 requests per hour. Please try again later.",
  "status": 429
}
```

## Error Handling

### Standard Error Response

All errors follow a consistent format:

```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "status": 400,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Common HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing or invalid authentication |
| 403 | Forbidden - Insufficient permissions or MFA required |
| 404 | Not Found |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error |
| 503 | Service Unavailable - Circuit breaker open |

### Circuit Breaker Errors

When a downstream service is unavailable and the circuit breaker is open:

```http
HTTP/1.1 503 Service Unavailable
Content-Type: application/json

{
  "error": "Service Unavailable",
  "service": "Climate Integrator Service",
  "message": "The climate data integration service is temporarily unavailable. Please try again later.",
  "status": 503,
  "timestamp": "2025-01-15T10:30:00Z",
  "recommendation": "Please try again in a few moments or contact support if the issue persists."
}
```

## Endpoints

### Health & Monitoring

#### Gateway Health Check
```http
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "gateway": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "integratorCircuitBreaker": "CLOSED",
        "visualizerCircuitBreaker": "CLOSED"
      }
    }
  }
}
```

#### Prometheus Metrics
```http
GET /actuator/prometheus
```

Returns Prometheus-formatted metrics for monitoring.

### Climate Integrator Service

#### Get Climate Data
```http
GET /api/integrator/climate/data?location={location}&startDate={startDate}&endDate={endDate}
Authorization: Bearer {token}
```

**Parameters**:
- `location` (string, required): Geographic location
- `startDate` (string, required): Start date in YYYY-MM-DD format
- `endDate` (string, required): End date in YYYY-MM-DD format

**Response**:
```json
{
  "location": "London",
  "dateRange": {
    "start": "2024-01-01",
    "end": "2024-12-31"
  },
  "data": [
    {
      "date": "2024-01-01",
      "temperature": 10.5,
      "humidity": 75,
      "precipitation": 2.3
    }
  ]
}
```

#### Integrate External Data Source
```http
POST /api/integrator/integrate
Authorization: Bearer {token}
Content-Type: application/json

{
  "source": "NOAA",
  "dataType": "temperature",
  "region": "North America"
}
```

### Climate Visualizer Service

#### Get Visualization
```http
GET /api/visualizer/charts/{chartType}?location={location}&timeRange={timeRange}
Authorization: Bearer {token}
```

**Parameters**:
- `chartType` (string, required): Type of chart (temperature, precipitation, etc.)
- `location` (string, required): Geographic location
- `timeRange` (string, required): Time range (1m, 3m, 6m, 1y, 5y)

#### Generate Report
```http
POST /api/visualizer/reports/generate
Authorization: Bearer {token}
Content-Type: application/json

{
  "reportType": "climate-health-assessment",
  "region": "Europe",
  "dateRange": {
    "start": "2024-01-01",
    "end": "2024-12-31"
  }
}
```

### User Session Service

#### User Login
```http
POST /api/session/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### User Registration
```http
POST /api/session/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

### LLM Service

#### Ask Question
```http
POST /api/llm/chat
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "What is the impact of climate change on respiratory health?",
  "context": "global"
}
```

**Response**:
```json
{
  "question": "What is the impact of climate change on respiratory health?",
  "answer": "Climate change significantly impacts respiratory health through...",
  "confidence": 0.92,
  "sources": [...]
}
```

#### Analyze Data
```http
POST /api/llm/analyze
Authorization: Bearer {token}
Content-Type: application/json

{
  "data": [...],
  "analysisType": "trend-prediction"
}
```

### Collaboration Service

#### Get Projects
```http
GET /api/collab/projects
Authorization: Bearer {token}
```

#### Create Project
```http
POST /api/collab/projects
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Climate Health Research 2025",
  "description": "Collaborative research on climate impact on public health",
  "members": ["user1@example.com", "user2@example.com"]
}
```

#### Share Document
```http
POST /api/collab/documents/share
Authorization: Bearer {token}
Content-Type: application/json

{
  "projectId": "project-123",
  "documentName": "Research Findings Q1 2025",
  "sharedWith": ["user3@example.com"]
}
```

## Response Headers

### Standard Headers

All responses include the following headers:

- `Content-Type`: `application/json` (for JSON responses)
- `X-Request-Id`: Unique request identifier for tracing
- `X-RateLimit-Limit`: Rate limit for the current user
- `X-RateLimit-Remaining`: Remaining requests in the current window
- `X-RateLimit-Reset`: Unix timestamp when the rate limit resets

### CORS Headers

For cross-origin requests:
- `Access-Control-Allow-Origin`: Allowed origins
- `Access-Control-Allow-Methods`: Allowed HTTP methods
- `Access-Control-Allow-Headers`: Allowed headers
- `Access-Control-Allow-Credentials`: Whether credentials are allowed

## Security

### Best Practices

1. **Always use HTTPS in production**
2. **Never expose JWT secrets**
3. **Rotate secrets regularly**
4. **Implement MFA for sensitive operations**
5. **Monitor rate limit violations**
6. **Keep dependencies updated**
7. **Review security logs regularly**

### JWT Token Security

- Tokens expire after 24 hours by default
- Use secure, random JWT secrets (minimum 256 bits)
- Store tokens securely (never in localStorage for web apps)
- Implement token refresh mechanism
- Validate token signatures

### CORS Configuration

By default, CORS is enabled for:
- `http://localhost:3000` (React development)
- `http://localhost:4200` (Angular development)

For production, configure appropriate origins via environment variables:

```bash
CORS_ALLOWED_ORIGINS=https://app.climatehealthmapper.com,https://admin.climatehealthmapper.com
```

## Support

For API support, please contact:
- Email: support@climatehealthmapper.com
- Documentation: https://docs.climatehealthmapper.com
- Issue Tracker: https://github.com/climatehealthmapper/issues
