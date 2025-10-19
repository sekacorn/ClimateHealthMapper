# ClimateHealth LLM Service

AI-powered natural language processing service with MBTI-tailored responses for ClimateHealthMapper.

## Features

- **Multi-Provider LLM Support**: Hugging Face, OpenAI, and xAI integration
- **MBTI Personality Tailoring**: Customized responses for all 16 MBTI types
- **Natural Language Queries**: Process climate health questions in natural language
- **Intelligent Troubleshooting**: Automated error analysis and resolution suggestions
- **Response Caching**: Redis-based caching for improved performance
- **Rate Limiting**: Built-in rate limiting to prevent abuse
- **Comprehensive Logging**: Detailed error and query logging

## MBTI Support

The service provides tailored responses for all 16 MBTI personality types:

### Analysts
- **INTJ (Architect)**: Analytical, detailed, strategic insights
- **INTP (Logician)**: Logical, in-depth, theoretical frameworks
- **ENTJ (Commander)**: Strategic, results-focused, actionable plans
- **ENTP (Debater)**: Witty, exploratory, innovative ideas

### Diplomats
- **INFJ (Advocate)**: Empathetic, holistic, meaningful insights
- **INFP (Mediator)**: Creative, value-driven, authentic recommendations
- **ENFJ (Protagonist)**: Inspirational, visionary, community-focused
- **ENFP (Campaigner)**: Creative, enthusiastic, exciting possibilities

### Sentinels
- **ISTJ (Logistician)**: Structured, precise, proven methods
- **ISFJ (Defender)**: Nurturing, practical, caring guidance
- **ESTJ (Executive)**: Direct, results-driven, efficient solutions
- **ESFJ (Consul)**: Supportive, community-focused, collaborative approaches

### Explorers
- **ISTP (Virtuoso)**: Concise, problem-solving, hands-on solutions
- **ISFP (Adventurer)**: Gentle, encouraging, harmonious approaches
- **ESTP (Entrepreneur)**: Actionable, quick, immediate impact
- **ESFP (Entertainer)**: Lively, action-oriented, engaging content

## API Endpoints

### Query Processing
```
POST /api/llm/query
```
Process natural language query with MBTI-tailored response

**Request Body**:
```json
{
  "userId": "user123",
  "queryText": "What are the health risks of air pollution?",
  "mbtiType": "INTJ",
  "location": "New York",
  "sessionId": "session456",
  "preferredProvider": "huggingface"
}
```

**Response**:
```json
{
  "success": true,
  "queryId": 1,
  "responseText": "Based on current data...",
  "mbtiType": "INTJ",
  "provider": "huggingface",
  "processingTimeMs": 1234,
  "cached": false,
  "timestamp": "2025-10-18T12:00:00"
}
```

### Troubleshooting
```
POST /api/llm/troubleshoot
```
Analyze errors and provide troubleshooting suggestions

**Request Body**:
```json
{
  "errorType": "DatabaseConnectionError",
  "serviceName": "data-service",
  "errorMessage": "Connection timeout",
  "stackTrace": "...",
  "userId": "user123"
}
```

**Response**:
```json
{
  "success": true,
  "errorId": 1,
  "severity": "CRITICAL",
  "suggestions": [
    "Check database connection settings",
    "Verify PostgreSQL service is running"
  ],
  "relatedDocs": ["https://docs.spring.io/..."],
  "estimatedResolutionTime": "15-30 minutes",
  "isRecurring": false,
  "similarErrorsCount": 0
}
```

### Query History
```
GET /api/llm/history/{userId}?page=0&size=20
```
Retrieve user's query history

### Health Check
```
GET /api/llm/health
```
Check service and provider health status

### MBTI Insights
```
GET /api/llm/mbti/{type}/insights
```
Get personality insights for MBTI type

## Configuration

### Environment Variables

```bash
# LLM Providers
HUGGINGFACE_API_KEY=your-api-key
OPENAI_API_KEY=your-api-key
XAI_API_KEY=your-api-key

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/climate_llm
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

### Application Configuration

Edit `src/main/resources/application.yml`:

```yaml
llm:
  provider: huggingface
  huggingface:
    api-key: ${HUGGINGFACE_API_KEY}
    model: mistralai/Mistral-7B-Instruct-v0.2
  rate-limit:
    requests-per-minute: 60
```

## Running the Service

### Local Development

```bash
# Install dependencies
mvn clean install

# Run the service
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker

```bash
# Build image
docker build -t climate-llm-service .

# Run container
docker run -p 8084:8084 \
  -e HUGGINGFACE_API_KEY=your-key \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/climate_llm \
  climate-llm-service
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=MbtiPromptServiceTest

# Run with coverage
mvn clean verify
```

## Database Setup

```sql
-- Create database
CREATE DATABASE climate_llm;

-- Tables are auto-created via JPA
-- See entities: QueryContext, LLMResponse, ErrorLog
```

## LLM Providers

### Hugging Face
- Free tier available
- Multiple open-source models
- Best for: Cost-effective deployments

### OpenAI
- High-quality responses
- GPT-3.5 and GPT-4 support
- Best for: Production quality responses

### xAI
- Grok model support
- Advanced reasoning capabilities
- Best for: Complex queries

## Rate Limiting

- Default: 60 requests/minute per user
- Configurable via `application.yml`
- Redis-based implementation
- Returns 429 when limit exceeded

## Caching

- Redis-based response caching
- 1-hour TTL by default
- Cache key: `query:{mbtiType}:{queryHash}:{locationHash}`
- Automatic cache invalidation

## Monitoring

Access metrics at:
- Health: `http://localhost:8084/api/llm/health`
- Metrics: `http://localhost:8084/actuator/metrics`
- Prometheus: `http://localhost:8084/actuator/prometheus`

## Architecture

```
Controller Layer (LLMController)
    ↓
Service Layer (LLMService, MbtiPromptService, TroubleshootingService)
    ↓
Repository Layer (QueryContextRepository, ErrorLogRepository)
    ↓
Database (PostgreSQL) + Cache (Redis)
```

## Error Handling

All errors are logged and can be queried via the troubleshooting endpoint. The service provides:
- Automatic error categorization
- Contextual troubleshooting suggestions
- Related documentation links
- Resolution time estimates

## Security

- Input validation on all endpoints
- Rate limiting to prevent abuse
- API key encryption (use environment variables)
- Non-root Docker user
- SQL injection prevention via JPA

## License

Part of the ClimateHealthMapper project.

## Support

For issues and questions, please refer to the main ClimateHealthMapper documentation.
