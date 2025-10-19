# Collaboration Service

Real-time collaboration microservice for ClimateHealthMapper with MBTI-tailored features.

## Features

- Real-time collaboration sessions via WebSocket
- Session management (create, join, leave, close)
- User action tracking and history
- MBTI-personalized collaboration features for all 16 types
- Redis caching for high-performance real-time features
- PostgreSQL persistence for session data

## MBTI Collaboration Features

The service provides tailored collaboration enhancements for all 16 MBTI personality types:

### Analysts (NT)
- **ENTJ (Commander)**: Leadership tools, strategic view, goal tracking
- **INTJ (Architect)**: Systematic planning, pattern recognition, long-term planning
- **ENTP (Debater)**: Innovation mode, alternative solutions, brainstorming
- **INTP (Logician)**: Deep analysis, logical frameworks, causal relationships

### Diplomats (NF)
- **ENFJ (Protagonist)**: Team dynamics, mentoring, community impact
- **INFJ (Advocate)**: Empathy features, human impact, values alignment
- **ENFP (Campaigner)**: Opportunity finder, creative exploration, inspiration
- **INFP (Mediator)**: Values-driven, reflection tools, authenticity

### Sentinels (SJ)
- **ESTJ (Executive)**: Task management, process optimization, efficiency
- **ISTJ (Logistician)**: Data validation, historical comparison, accuracy
- **ESFJ (Consul)**: Group support, community needs, harmony monitoring
- **ISFJ (Defender)**: Protective care, vulnerability assessment, safety

### Explorers (SP)
- **ESTP (Entrepreneur)**: Real-time updates, quick actions, live feed
- **ISTP (Virtuoso)**: Technical analysis, problem-solving, diagnostics
- **ESFP (Entertainer)**: Visual storytelling, social sharing, engagement
- **ISFP (Adventurer)**: Aesthetic customization, creative expression, sensory experience

## API Endpoints

### REST Endpoints

- `POST /api/collab/session/create` - Create new session
- `POST /api/collab/session/{sessionId}/join` - Join session
- `POST /api/collab/session/{sessionId}/leave` - Leave session
- `GET /api/collab/session/{sessionId}` - Get session details
- `GET /api/collab/sessions/active` - List active sessions
- `GET /api/collab/sessions/user/{userId}` - Get user sessions
- `PUT /api/collab/session/{sessionId}/settings` - Update session
- `POST /api/collab/session/{sessionId}/close` - Close session
- `GET /api/collab/session/{sessionId}/history` - Get action history

### WebSocket Endpoints

Connect to: `ws://localhost:8085/ws/collab`

Actions:
- `/app/collab/{sessionId}/zoom` - Zoom actions
- `/app/collab/{sessionId}/pan` - Pan actions
- `/app/collab/{sessionId}/filter` - Filter changes
- `/app/collab/{sessionId}/annotate` - Annotations
- `/app/collab/{sessionId}/share` - Share view
- `/app/collab/{sessionId}/cursor` - Cursor movement
- `/app/collab/{sessionId}/comment` - Comments
- `/app/collab/{sessionId}/highlight` - Highlights
- `/app/collab/{sessionId}/layer` - Layer toggles
- `/app/collab/{sessionId}/marker` - Markers
- `/app/collab/{sessionId}/presence` - Presence updates

Subscribe to:
- `/topic/collab/{sessionId}` - Session broadcasts
- `/topic/collab/{sessionId}/cursors` - Cursor updates
- `/topic/collab/{sessionId}/presence` - Presence updates
- `/user/queue/collab/{sessionId}/mbti` - Personal MBTI notifications

## Configuration

Environment variables:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=collaboration_db
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Server
SERVER_PORT=8085

# Logging
LOG_LEVEL=DEBUG
SHOW_SQL=false

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

# WebSocket
WS_ALLOWED_ORIGINS=*
```

## Running the Service

### Using Maven
```bash
mvn spring-boot:run
```

### Using Docker
```bash
docker build -t collaboration-service .
docker run -p 8085:8085 collaboration-service
```

## Testing

Run tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn clean test jacoco:report
```

## Health Check

Access health endpoint:
```
http://localhost:8085/actuator/health
```

## Architecture

- **Controllers**: REST and WebSocket endpoints
- **Services**: Business logic and MBTI enhancements
- **Repositories**: JPA data access
- **Models**: JPA entities for sessions, participants, actions
- **DTOs**: Request/response objects
- **Config**: WebSocket and application configuration

## Dependencies

- Spring Boot 3.2.0
- Spring WebSocket
- Spring Data JPA
- PostgreSQL
- Redis
- Jackson
- Lombok
- JUnit 5
