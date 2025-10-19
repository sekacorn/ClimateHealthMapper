# ClimateHealthMapper Infrastructure Configuration

This directory contains the complete infrastructure configuration for the ClimateHealthMapper application, including NGINX reverse proxy, Kubernetes manifests, CI/CD pipelines, and monitoring dashboards.

## Directory Structure

```
infra/
├── nginx/
│   └── default.conf                          # NGINX reverse proxy configuration
├── kubernetes/
│   ├── namespace.yml                         # Kubernetes namespace
│   ├── configmap.yml                         # ConfigMap with service URLs
│   ├── secrets.yml.template                  # Secret template (DO NOT commit actual secrets)
│   ├── postgres-statefulset.yml              # PostgreSQL database with 50Gi PVC
│   ├── redis-deployment.yml                  # Redis cache deployment
│   ├── api-gateway-deployment.yml            # API Gateway (3 replicas)
│   ├── climate-integrator-deployment.yml     # Climate Integrator (2 replicas)
│   ├── climate-visualizer-deployment.yml     # Climate Visualizer (2 replicas)
│   ├── user-session-deployment.yml           # User Session Service (3 replicas)
│   ├── llm-service-deployment.yml            # LLM Service (2 replicas)
│   ├── collaboration-service-deployment.yml  # Collaboration Service (2 replicas)
│   ├── ai-model-deployment.yml               # AI Model Service with GPU (1 replica)
│   ├── frontend-deployment.yml               # React Frontend (3 replicas)
│   ├── ingress.yml                           # Ingress with TLS configuration
│   ├── hpa.yml                               # HorizontalPodAutoscaler for all services
│   └── network-policy.yml                    # Network security policies
├── prometheus/
│   └── prometheus.yml                        # Prometheus monitoring configuration
└── grafana/
    └── dashboards/
        └── climate-health-dashboard.json     # Grafana dashboard

.github/
└── workflows/
    └── ci.yml                                 # GitHub Actions CI/CD pipeline
```

## NGINX Configuration

**File:** `infra/nginx/default.conf`

The NGINX reverse proxy provides:
- SSL/TLS termination (TLSv1.2 and TLSv1.3)
- Rate limiting (100 req/min for API, 200 req/min for general)
- CORS headers for cross-origin requests
- Gzip compression for static assets
- Security headers (CSP, X-Frame-Options, HSTS, etc.)
- WebSocket upgrade support for collaboration service

**Routes:**
- `/` → frontend:80
- `/api` → api-gateway:8080
- `/ws` → collaboration-service:8085 (WebSocket)

## Kubernetes Manifests

### Namespace
**File:** `infra/kubernetes/namespace.yml`

Creates the `climate-health` namespace for all application resources.

### ConfigMap
**File:** `infra/kubernetes/configmap.yml`

Contains configuration for:
- Service URLs
- Database and Redis connection settings
- External API endpoints (NOAA, NASA, WHO, OpenWeather)
- AI model configuration
- Monitoring settings

### Secrets Template
**File:** `infra/kubernetes/secrets.yml.template`

Template for creating secrets. Contains placeholders for:
- Database credentials
- Redis password
- JWT secret key
- External API keys (NOAA, NASA, OpenWeather)
- AWS/GCP credentials
- OAuth credentials
- SMTP settings

**IMPORTANT:** Copy this to `secrets.yml`, fill in actual values, and apply. Never commit actual secrets!

### Database & Cache

#### PostgreSQL StatefulSet
**File:** `infra/kubernetes/postgres-statefulset.yml`

- Image: postgres:15-alpine
- Storage: 50Gi PersistentVolume
- Resources: 512Mi-2Gi RAM, 500m-2000m CPU
- Health checks: liveness and readiness probes
- Custom PostgreSQL configuration for performance tuning

#### Redis Deployment
**File:** `infra/kubernetes/redis-deployment.yml`

- Image: redis:7-alpine
- Storage: 10Gi PersistentVolume
- Resources: 128Mi-512Mi RAM, 100m-500m CPU
- AOF persistence enabled
- LRU eviction policy

### Service Deployments

All services include:
- Health checks (liveness, readiness, startup probes)
- Resource limits and requests
- Prometheus metrics scraping annotations
- Environment variables from ConfigMap and Secrets

#### API Gateway
**File:** `infra/kubernetes/api-gateway-deployment.yml`
- Replicas: 3
- Resources: 256Mi-512Mi RAM, 200m-1000m CPU
- Port: 8080
- Pod anti-affinity for high availability

#### Climate Integrator
**File:** `infra/kubernetes/climate-integrator-deployment.yml`
- Replicas: 2
- Resources: 256Mi-512Mi RAM, 200m-1000m CPU
- Port: 8081
- Integrates with external climate APIs

#### Climate Visualizer
**File:** `infra/kubernetes/climate-visualizer-deployment.yml`
- Replicas: 2
- Resources: 256Mi-512Mi RAM, 200m-1000m CPU
- Port: 8082

#### User Session Service
**File:** `infra/kubernetes/user-session-deployment.yml`
- Replicas: 3
- Resources: 256Mi-512Mi RAM, 200m-1000m CPU
- Port: 8083
- JWT authentication
- OAuth support

#### LLM Service
**File:** `infra/kubernetes/llm-service-deployment.yml`
- Replicas: 2
- Resources: 512Mi-1Gi RAM, 500m-2000m CPU
- Port: 8084
- Python-based service

#### Collaboration Service
**File:** `infra/kubernetes/collaboration-service-deployment.yml`
- Replicas: 2
- Resources: 256Mi-512Mi RAM, 200m-1000m CPU
- Port: 8085
- WebSocket support

#### AI Model Service
**File:** `infra/kubernetes/ai-model-deployment.yml`
- Replicas: 1
- Resources: 2Gi-8Gi RAM, 1000m-4000m CPU, 1 GPU
- Port: 8086
- GPU support (nvidia.com/gpu: 1)
- Model storage: 100Gi PersistentVolume
- Node selector for GPU nodes

#### Frontend
**File:** `infra/kubernetes/frontend-deployment.yml`
- Replicas: 3
- Resources: 128Mi-256Mi RAM, 100m-500m CPU
- Port: 80
- NGINX serving static React app

### Ingress
**File:** `infra/kubernetes/ingress.yml`

- TLS/SSL support with cert-manager
- Routes for API, WebSocket, and frontend
- CORS and rate limiting annotations
- Domain: climatehealthmapper.com

### Horizontal Pod Autoscaling
**File:** `infra/kubernetes/hpa.yml`

Autoscaling configuration for all services:
- API Gateway: 3-10 pods, 70% CPU target
- Climate Integrator: 2-8 pods, 70% CPU target
- Climate Visualizer: 2-8 pods, 70% CPU target
- User Session: 3-10 pods, 60% CPU target
- LLM Service: 2-6 pods, 75% CPU target
- Collaboration Service: 2-8 pods, 70% CPU target
- Frontend: 3-10 pods, 50% CPU target

### Network Policies
**File:** `infra/kubernetes/network-policy.yml`

Network security policies:
- Default deny all traffic
- Explicit allow rules for each service
- Database and Redis only accessible from authorized services
- External API access only where needed

## CI/CD Pipeline

**File:** `.github/workflows/ci.yml`

GitHub Actions workflow with:

### Linting
- Frontend: ESLint, Prettier
- Java services: Checkstyle, SpotBugs
- Python services: Flake8, Black, isort, pylint
- Node.js service: ESLint

### Testing
- Frontend: Jest with coverage
- Java services: JUnit with JaCoCo coverage
- Python services: Pytest with coverage
- Node.js service: Jest with coverage
- Coverage reports to Codecov

### Security
- OWASP ZAP baseline security scan

### Build & Deploy
- Docker build and push to GitHub Container Registry
- Kubernetes deployment to cluster
- Deployment verification

## Monitoring

### Prometheus
**File:** `infra/prometheus/prometheus.yml`

Scrapes metrics from:
- All microservices (ports 8080-8086)
- PostgreSQL and Redis
- Kubernetes nodes and pods
- Node Exporter, kube-state-metrics, cAdvisor

### Grafana Dashboard
**File:** `infra/grafana/dashboards/climate-health-dashboard.json`

Dashboard panels:
- Request rate by service
- Response time (95th percentile)
- CPU usage by pod
- Memory usage by pod
- Service availability
- Error rate (5xx)
- Active pods
- Database query rate
- Redis connected clients

## Deployment Instructions

### Prerequisites
- Kubernetes cluster (1.24+)
- kubectl configured
- cert-manager for TLS certificates
- NGINX Ingress Controller
- Prometheus & Grafana (optional)

### Step 1: Create Namespace
```bash
kubectl apply -f infra/kubernetes/namespace.yml
```

### Step 2: Create Secrets
```bash
# Copy the template
cp infra/kubernetes/secrets.yml.template infra/kubernetes/secrets.yml

# Edit and fill in actual values
nano infra/kubernetes/secrets.yml

# Apply secrets
kubectl apply -f infra/kubernetes/secrets.yml

# Delete the file (or add to .gitignore)
rm infra/kubernetes/secrets.yml
```

### Step 3: Create ConfigMap
```bash
kubectl apply -f infra/kubernetes/configmap.yml
```

### Step 4: Deploy Database & Cache
```bash
kubectl apply -f infra/kubernetes/postgres-statefulset.yml
kubectl apply -f infra/kubernetes/redis-deployment.yml

# Wait for them to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n climate-health --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n climate-health --timeout=300s
```

### Step 5: Deploy Services
```bash
kubectl apply -f infra/kubernetes/api-gateway-deployment.yml
kubectl apply -f infra/kubernetes/climate-integrator-deployment.yml
kubectl apply -f infra/kubernetes/climate-visualizer-deployment.yml
kubectl apply -f infra/kubernetes/user-session-deployment.yml
kubectl apply -f infra/kubernetes/llm-service-deployment.yml
kubectl apply -f infra/kubernetes/collaboration-service-deployment.yml
kubectl apply -f infra/kubernetes/ai-model-deployment.yml
kubectl apply -f infra/kubernetes/frontend-deployment.yml
```

### Step 6: Deploy Ingress
```bash
kubectl apply -f infra/kubernetes/ingress.yml
```

### Step 7: Deploy Autoscaling
```bash
kubectl apply -f infra/kubernetes/hpa.yml
```

### Step 8: Apply Network Policies
```bash
kubectl apply -f infra/kubernetes/network-policy.yml
```

### Step 9: Deploy Monitoring (Optional)
```bash
# Deploy Prometheus
kubectl apply -f infra/prometheus/prometheus.yml

# Import Grafana dashboard
# Upload climate-health-dashboard.json to Grafana UI
```

## Verify Deployment

```bash
# Check all pods
kubectl get pods -n climate-health

# Check services
kubectl get svc -n climate-health

# Check ingress
kubectl get ingress -n climate-health

# Check HPA
kubectl get hpa -n climate-health

# View logs
kubectl logs -f deployment/api-gateway -n climate-health
```

## NGINX Deployment

The NGINX configuration can be used in two ways:

### Option 1: As Kubernetes Ingress Controller
Already configured in `infra/kubernetes/ingress.yml`

### Option 2: As Standalone Reverse Proxy
```bash
# Copy config
cp infra/nginx/default.conf /etc/nginx/conf.d/

# Test config
nginx -t

# Reload
nginx -s reload
```

## Security Notes

1. **Secrets Management**: Never commit actual secrets to version control
2. **TLS Certificates**: Use cert-manager or provide your own certificates
3. **Network Policies**: Review and adjust based on your security requirements
4. **Resource Limits**: Adjust based on your workload
5. **RBAC**: Configure Kubernetes RBAC for production

## Scaling

### Manual Scaling
```bash
kubectl scale deployment api-gateway --replicas=5 -n climate-health
```

### Auto Scaling
HPA is already configured. Adjust targets in `infra/kubernetes/hpa.yml`

## Troubleshooting

### Pod not starting
```bash
kubectl describe pod <pod-name> -n climate-health
kubectl logs <pod-name> -n climate-health
```

### Service not accessible
```bash
kubectl get endpoints -n climate-health
kubectl port-forward svc/api-gateway 8080:8080 -n climate-health
```

### Database connection issues
```bash
kubectl exec -it postgres-0 -n climate-health -- psql -U climate_admin -d climate_health_db
```

## Maintenance

### Database Backup
```bash
kubectl exec postgres-0 -n climate-health -- pg_dump -U climate_admin climate_health_db > backup.sql
```

### Update Secrets
```bash
kubectl delete secret climate-health-secrets -n climate-health
kubectl apply -f infra/kubernetes/secrets.yml
kubectl rollout restart deployment -n climate-health
```

### Rolling Update
```bash
kubectl set image deployment/api-gateway api-gateway=climate-health/api-gateway:v2.0 -n climate-health
kubectl rollout status deployment/api-gateway -n climate-health
```

## Support

For issues or questions:
- Check the logs: `kubectl logs -f <pod-name> -n climate-health`
- Review metrics: Grafana dashboard
- Check events: `kubectl get events -n climate-health --sort-by='.lastTimestamp'`
