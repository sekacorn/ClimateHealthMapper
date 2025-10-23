# ClimateHealthMapper

An AI-powered, interactive 3D platform for visualizing and analyzing the intersection of climate conditions and public health risks. ClimateHealthMapper integrates real-time environmental data, genomic information, and health records to predict and map health risks across geographic regions.

## Overview

ClimateHealthMapper combines cutting-edge technologies to provide researchers, healthcare professionals, and policymakers with actionable insights into how climate change affects human health. The platform features interactive 3D visualizations, AI-driven risk predictions, and collaborative tools for multi-user analysis.

## Key Features

### Core Capabilities
- **Interactive 3D Visualization**: Real-time 3D globe visualization using Three.js and React Three Fiber
- **AI Health Risk Prediction**: Deep learning models predict asthma, heatstroke, cardiovascular, and respiratory risks
- **Multi-Source Data Integration**: Combines environmental, health, genomic, and geospatial data
- **Real-Time Collaboration**: Multi-user sessions with WebSocket support
- **LLM-Powered Insights**: Natural language querying and automated troubleshooting
- **Advanced Security**: JWT authentication, MFA support, SSO integration, and role-based access control

### Data Sources
- **Environmental**: NOAA, NASA, OpenWeather (temperature, humidity, air quality, UV index)
- **Health**: FHIR-compliant health records, patient demographics, medical history
- **Genomic**: BioPython integration for genetic risk analysis
- **Geospatial**: Interactive mapping with location-based risk assessment

### Analysis Tools
- Batch data upload and processing (CSV support)
- Real-time risk scoring (0-1 scale)
- Historical trend analysis
- Predictive modeling with confidence intervals
- Export capabilities (JSON, CSV, PDF reports)

## Technology Stack

### Frontend
- **Framework**: React 18.2 with Vite
- **3D Graphics**: Three.js, React Three Fiber, Drei
- **UI/Styling**: TailwindCSS, Lucide React icons
- **State Management**: Zustand
- **Data Visualization**: Plotly.js
- **API Communication**: Axios, Socket.io client

### Backend Services (Microservices Architecture)
- **API Gateway**: Spring Boot (Port 8080) - Request routing, rate limiting, authentication
- **Climate Integrator**: Spring Boot (Port 8081) - Environmental data aggregation
- **Climate Visualizer**: Spring Boot (Port 8082) - Visualization and export services
- **User Session Service**: Spring Boot (Port 8083) - Authentication, MFA, SSO
- **LLM Service**: Spring Boot (Port 8084) - Natural language processing, MBTI-based insights
- **Collaboration Service**: Spring Boot (Port 8085) - WebSocket collaboration, real-time sync
- **AI Model Service**: Python FastAPI (Port 8000) - Deep learning health predictions

### AI/ML
- **Framework**: PyTorch 2.1
- **Architecture**: Deep neural network with residual connections
- **Features**: 50+ input features (environmental, health, genomic)
- **Outputs**: 4 health risk scores (asthma, heatstroke, cardiovascular, respiratory)
- **Performance**: <50ms prediction time (CPU), <10ms (GPU)

### Infrastructure
- **Database**: PostgreSQL 15 with 50Gi storage
- **Cache**: Redis 7 with AOF persistence
- **Container Orchestration**: Docker Compose, Kubernetes
- **Reverse Proxy**: NGINX with SSL/TLS, rate limiting
- **Monitoring**: Prometheus + Grafana
- **CI/CD**: GitHub Actions

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      NGINX Reverse Proxy                        │
│            (SSL/TLS, Rate Limiting, CORS, Security)             │
└───────────┬──────────────────┬──────────────────┬───────────────┘
            │                  │                  │
    ┌───────▼────────┐ ┌──────▼─────────┐ ┌─────▼──────────┐
    │   Frontend     │ │  API Gateway   │ │  WebSocket     │
    │  (React/Vite)  │ │ (Spring Boot)  │ │ Collaboration  │
    │   Port 3000    │ │   Port 8080    │ │   Port 8085    │
    └────────────────┘ └───────┬────────┘ └────────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
    ┌────▼──────┐    ┌─────────▼──────┐    ┌────────▼────────┐
    │  Climate  │    │    Climate     │    │  User Session   │
    │Integrator │    │  Visualizer    │    │    Service      │
    │Port 8081  │    │   Port 8082    │    │   Port 8083     │
    └────┬──────┘    └────────┬───────┘    └────────┬────────┘
         │                    │                      │
         └────────────────────┼──────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
    ┌────▼────────┐    ┌──────▼──────┐    ┌───────▼───────┐
    │ LLM Service │    │ AI Model    │    │ Collaboration │
    │  Port 8084  │    │  Service    │    │   Service     │
    │             │    │  Port 8000  │    │  Port 8085    │
    └─────────────┘    └──────┬──────┘    └───────────────┘
                              │
         ┌────────────────────┴────────────────────┐
         │                                         │
    ┌────▼─────────┐                      ┌────────▼──────┐
    │  PostgreSQL  │                      │     Redis     │
    │  Port 5432   │                      │   Port 6379   │
    │   Database   │                      │     Cache     │
    └──────────────┘                      └───────────────┘
```

## Getting Started

### Prerequisites
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Node.js**: 18+ (for local frontend development)
- **Java**: 17+ (for local backend development)
- **Python**: 3.11+ (for AI model development)
- **PostgreSQL**: 15+ (if running locally)
- **Redis**: 7+ (if running locally)

### Quick Start with Docker Compose

1. **Clone the repository**:
```bash
git clone https://github.com/yourusername/ClimateHealthMapper.git
cd ClimateHealthMapper
```

2. **Set environment variables**:
```bash
# Set your LLM API key
export LLM_API_KEY=your-api-key-here
```

3. **Start all services**:
```bash
docker-compose up -d
```

4. **Access the application**:
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- AI Model Service: http://localhost:8000

5. **View logs**:
```bash
docker-compose logs -f
```

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React web application |
| NGINX | 80, 443 | Reverse proxy |
| API Gateway | 8080 | Main API entry point |
| Climate Integrator | 8081 | Environmental data service |
| Climate Visualizer | 8082 | Visualization service |
| User Session | 8083 | Authentication service |
| LLM Service | 8084 | Natural language processing |
| Collaboration | 8085 | WebSocket collaboration |
| AI Model | 8000 | Health prediction API |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |

## Development

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at http://localhost:5173 with hot reload.

### Backend Development

Each microservice can be run independently:

```bash
cd backend/api-gateway
./mvnw spring-boot:run
```

### AI Model Development

```bash
cd ai-model
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python health_predictor.py
```

### Running Tests

**Frontend**:
```bash
cd frontend
npm test
npm run test:ui  # Vitest UI
```

**Backend**:
```bash
cd backend/api-gateway
./mvnw test
```

**AI Model**:
```bash
cd ai-model
pytest tests/ -v --cov
```

## Deployment

### Docker Compose (Development/Staging)

See [Quick Start](#quick-start-with-docker-compose) above.

### Kubernetes (Production)

Comprehensive Kubernetes deployment with autoscaling, monitoring, and network policies.

See [infra/README.md](infra/README.md) for detailed instructions.

**Quick deploy**:
```bash
# Create namespace and secrets
kubectl apply -f infra/kubernetes/namespace.yml
kubectl apply -f infra/kubernetes/secrets.yml

# Deploy infrastructure
kubectl apply -f infra/kubernetes/configmap.yml
kubectl apply -f infra/kubernetes/postgres-statefulset.yml
kubectl apply -f infra/kubernetes/redis-deployment.yml

# Deploy services
kubectl apply -f infra/kubernetes/
```

## API Documentation

### Health Prediction API

**Endpoint**: `POST http://localhost:8000/predict`

**Request**:
```json
{
  "environmental": {
    "pm25": 35.5,
    "aqi": 75.0,
    "temperature": 28.5,
    "humidity": 65.0
  },
  "health": {
    "age": 45,
    "bmi": 26.5,
    "has_asthma": 0,
    "smoker": 0
  },
  "genomic": {
    "genetic_risk_score": 0.35
  },
  "user_id": "user123",
  "location_id": "loc456"
}
```

**Response**:
```json
{
  "risks": {
    "asthma_risk": 0.23,
    "heatstroke_risk": 0.45,
    "cardiovascular_risk": 0.31,
    "respiratory_risk": 0.28,
    "overall_risk": 0.32,
    "risk_level": "moderate"
  },
  "timestamp": "2025-10-23T10:35:00",
  "model_version": "1.0.0"
}
```

### API Gateway

**Base URL**: `http://localhost:8080/api`

**Authentication**: Include JWT token in Authorization header
```
Authorization: Bearer <your-jwt-token>
```

**Postman Collections**:
- API Gateway: [backend/api-gateway/postman-collection.json](backend/api-gateway/postman-collection.json)
- User Session: [backend/user-session/postman_collection.json](backend/user-session/postman_collection.json)

## Features in Detail

### AI Health Risk Prediction

The AI model uses a deep neural network trained on environmental, health, and genomic data:

- **Input Features**: 50+ features including PM2.5, temperature, humidity, age, BMI, genetic markers
- **Architecture**: Multi-layer perceptron with residual connections and dropout
- **Output**: 4 risk scores (0-1): asthma, heatstroke, cardiovascular, respiratory
- **Performance**:
  - Prediction time: <50ms (CPU), <10ms (GPU)
  - Throughput: 2000+ requests/sec with multi-worker setup

See [ai-model/README.md](ai-model/README.md) for detailed documentation.

### Real-Time Collaboration

Multiple users can collaborate on the same visualization:
- Shared cursor positions
- Synchronized view states
- Live data updates
- MBTI-based collaboration insights
- Session management with QR codes

### LLM Integration

Natural language interface for data querying:
- Context-aware responses
- Query history
- Automatic troubleshooting
- MBTI personality-based prompt optimization
- Error log analysis

### Data Security

- JWT-based authentication
- Multi-factor authentication (MFA)
- Single Sign-On (SSO) support
- Role-based access control (RBAC)
- Audit logging
- Rate limiting
- CORS protection
- SQL injection prevention

## Configuration

### Environment Variables

**Backend Services**:
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/climatehealth
SPRING_DATASOURCE_USERNAME=climateuser
SPRING_DATASOURCE_PASSWORD=climatepass
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

**AI Model**:
```bash
PYTHON_ENV=production
DATABASE_URL=postgresql://climateuser:climatepass@postgres:5432/climatehealth
```

**Frontend**:
```bash
REACT_APP_API_GATEWAY_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8085
```

## Monitoring

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus` (Spring Boot) or `/metrics` (FastAPI).

**Metrics collected**:
- Request rate and latency
- CPU and memory usage
- Database query performance
- Cache hit/miss rates
- Error rates

### Grafana Dashboard

Import the pre-built dashboard from [infra/grafana/dashboards/climate-health-dashboard.json](infra/grafana/dashboards/climate-health-dashboard.json).

**Dashboard panels**:
- Request rate by service
- Response time (p95)
- Resource usage
- Service availability
- Error rates
- Database and cache metrics

## Troubleshooting

### Common Issues

**Services not starting**:
```bash
docker-compose logs <service-name>
docker-compose restart <service-name>
```

**Database connection errors**:
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Connect to database
docker-compose exec postgres psql -U climateuser -d climatehealth
```

**AI model not loading**:
```bash
cd ai-model
python train_model.py  # Train model if not present
```

**Port conflicts**:
```bash
# Check if ports are in use
netstat -an | grep 8080
# or
lsof -i :8080
```

## Contributing

We welcome contributions! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- **Frontend**: ESLint + Prettier
- **Backend**: Google Java Style Guide
- **Python**: PEP 8 (enforced by Black, Flake8)

### Testing Requirements

All PRs must include tests and maintain >80% code coverage.

## License

This software is available under a dual licensing model:

### Non-Profit License (Free)
Free for non-profit organizations, educational institutions, research institutions, and individuals using for non-commercial purposes.

### Commercial License (4% Revenue Share)
For-profit entities must obtain a commercial license and pay 4% of gross revenue generated from the use of this software.

See [LICENSE](LICENSE) for full terms.

**For commercial licensing inquiries**: sekacorn@gmail.com

## Support

### Documentation
- [AI Model Service](ai-model/README.md)
- [Infrastructure Setup](infra/README.md)
- [Quick Start Guide](infra/QUICK_START.md)

### Issues
For bug reports and feature requests, please use [GitHub Issues](https://github.com/yourusername/ClimateHealthMapper/issues).

### Contact
- **Email**: sekacorn@gmail.com
- **Project Maintainer**: sekacorn

## Acknowledgments

This project integrates data and services from:
- NOAA (National Oceanic and Atmospheric Administration)
- NASA (National Aeronautics and Space Administration)
- WHO (World Health Organization)
- OpenWeather API
- FHIR (Fast Healthcare Interoperability Resources)

Built with:
- React + Vite
- Spring Boot
- PyTorch + FastAPI
- Three.js
- PostgreSQL + Redis
- Docker + Kubernetes

## Roadmap

- [ ] Mobile app (React Native)
- [ ] Additional ML models (disease outbreak prediction)
- [ ] Integration with more health data sources
- [ ] Enhanced genomic analysis
- [ ] Real-time satellite data integration
- [ ] Advanced collaboration features (video/audio)
- [ ] Multi-language support
- [ ] Offline mode with data sync

## Project Status

**Current Version**: 1.0.0

**Status**: Active Development

**Last Updated**: October 2025

---

**ClimateHealthMapper** - Mapping the Future of Climate and Health

Copyright (c) 2025 sekacorn (sekacorn@gmail.com)
