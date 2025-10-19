# ClimateHealthMapper - Quick Start Guide

This guide provides quick commands to get ClimateHealthMapper up and running.

## Local Development with Docker Compose

### Prerequisites
- Docker Desktop installed
- Docker Compose installed
- At least 8GB RAM available

### Start All Services
```bash
cd infra
docker-compose up -d
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
```

### Stop All Services
```bash
docker-compose down
```

### Rebuild and Restart
```bash
docker-compose down
docker-compose build
docker-compose up -d
```

### Access Services
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379

## Kubernetes Deployment

### Prerequisites
- Kubernetes cluster running
- kubectl configured
- At least 16GB RAM available across cluster

### Quick Deploy (All-in-One)
```bash
cd infra
chmod +x deploy.sh
./deploy.sh deploy
```

### Step-by-Step Deploy

#### 1. Create Secrets
```bash
cp kubernetes/secrets.yml.template kubernetes/secrets.yml
nano kubernetes/secrets.yml  # Edit and fill in values
```

#### 2. Deploy Infrastructure
```bash
# Deploy namespace
kubectl apply -f kubernetes/namespace.yml

# Deploy ConfigMap
kubectl apply -f kubernetes/configmap.yml

# Deploy Secrets
kubectl apply -f kubernetes/secrets.yml
```

#### 3. Deploy Database & Cache
```bash
kubectl apply -f kubernetes/postgres-statefulset.yml
kubectl apply -f kubernetes/redis-deployment.yml

# Wait for ready
kubectl wait --for=condition=ready pod -l app=postgres -n climate-health --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n climate-health --timeout=300s
```

#### 4. Deploy Services
```bash
kubectl apply -f kubernetes/api-gateway-deployment.yml
kubectl apply -f kubernetes/climate-integrator-deployment.yml
kubectl apply -f kubernetes/climate-visualizer-deployment.yml
kubectl apply -f kubernetes/user-session-deployment.yml
kubectl apply -f kubernetes/llm-service-deployment.yml
kubectl apply -f kubernetes/collaboration-service-deployment.yml
kubectl apply -f kubernetes/ai-model-deployment.yml
kubectl apply -f kubernetes/frontend-deployment.yml
```

#### 5. Deploy Ingress & Networking
```bash
kubectl apply -f kubernetes/ingress.yml
kubectl apply -f kubernetes/hpa.yml
kubectl apply -f kubernetes/network-policy.yml
```

### Verify Deployment
```bash
./deploy.sh status

# Or manually
kubectl get all -n climate-health
```

### Access Application
```bash
# Get ingress IP
kubectl get ingress -n climate-health

# Or port-forward for testing
kubectl port-forward svc/frontend 3000:80 -n climate-health
```

## Common Operations

### View Logs
```bash
# API Gateway
kubectl logs -f deployment/api-gateway -n climate-health

# All pods
kubectl logs -f -l app=api-gateway -n climate-health
```

### Scale Services
```bash
# Manual scaling
kubectl scale deployment api-gateway --replicas=5 -n climate-health

# Auto-scaling is already configured via HPA
```

### Restart Service
```bash
kubectl rollout restart deployment/api-gateway -n climate-health
```

### Database Access
```bash
# Docker Compose
docker exec -it climate-health-postgres psql -U climate_admin -d climate_health_db

# Kubernetes
kubectl exec -it postgres-0 -n climate-health -- psql -U climate_admin -d climate_health_db
```

### Redis CLI
```bash
# Docker Compose
docker exec -it climate-health-redis redis-cli -a dev_password

# Kubernetes
kubectl exec -it deployment/redis -n climate-health -- redis-cli -a $(kubectl get secret climate-health-secrets -n climate-health -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d)
```

### Update Configuration
```bash
# Edit ConfigMap
kubectl edit configmap climate-health-config -n climate-health

# Restart affected services
kubectl rollout restart deployment -n climate-health
```

### Monitor Resources
```bash
# CPU and Memory usage
kubectl top pods -n climate-health
kubectl top nodes

# HPA status
kubectl get hpa -n climate-health
```

## Troubleshooting

### Pod CrashLoopBackOff
```bash
# Check logs
kubectl logs <pod-name> -n climate-health --previous

# Describe pod
kubectl describe pod <pod-name> -n climate-health
```

### Service Unreachable
```bash
# Check endpoints
kubectl get endpoints -n climate-health

# Test service internally
kubectl run -it --rm debug --image=alpine --restart=Never -n climate-health -- sh
apk add curl
curl http://api-gateway:8080/health
```

### Database Connection Issues
```bash
# Check PostgreSQL is running
kubectl get pods -l app=postgres -n climate-health

# Test connection
kubectl run -it --rm psql-test --image=postgres:15-alpine --restart=Never -n climate-health -- psql -h postgres -U climate_admin -d climate_health_db
```

### Ingress Not Working
```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress configuration
kubectl describe ingress climate-health-ingress -n climate-health

# Check TLS certificates
kubectl get certificate -n climate-health
```

## CI/CD with GitHub Actions

### Setup
1. Fork the repository
2. Add secrets in GitHub repository settings:
   - `POSTGRES_USER`
   - `POSTGRES_PASSWORD`
   - `REDIS_PASSWORD`
   - `JWT_SECRET`
   - `NOAA_API_KEY`
   - `NASA_API_KEY`
   - `OPENWEATHER_API_KEY`
   - `KUBECONFIG` (base64 encoded)

3. Push to main branch to trigger deployment

### Manual Workflow Trigger
```bash
# In GitHub UI: Actions → CI/CD → Run workflow
```

## Monitoring

### Prometheus
```bash
# Port forward to Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n monitoring

# Access at http://localhost:9090
```

### Grafana
```bash
# Port forward to Grafana
kubectl port-forward svc/grafana 3000:3000 -n monitoring

# Access at http://localhost:3000
# Import dashboard from infra/grafana/dashboards/climate-health-dashboard.json
```

## Cleanup

### Docker Compose
```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v
```

### Kubernetes
```bash
# Using script
./deploy.sh cleanup

# Manual
kubectl delete namespace climate-health
```

## Performance Tuning

### Database Optimization
```bash
# Analyze database
kubectl exec postgres-0 -n climate-health -- psql -U climate_admin -d climate_health_db -c "ANALYZE;"

# Vacuum
kubectl exec postgres-0 -n climate-health -- psql -U climate_admin -d climate_health_db -c "VACUUM ANALYZE;"
```

### Cache Optimization
```bash
# Check Redis memory usage
kubectl exec deployment/redis -n climate-health -- redis-cli -a $(kubectl get secret climate-health-secrets -n climate-health -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d) INFO memory

# Clear cache if needed
kubectl exec deployment/redis -n climate-health -- redis-cli -a $(kubectl get secret climate-health-secrets -n climate-health -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d) FLUSHALL
```

## Security Checklist

- [ ] Change all default passwords in secrets.yml
- [ ] Configure TLS certificates for HTTPS
- [ ] Enable network policies
- [ ] Review and update CORS settings
- [ ] Set up RBAC for Kubernetes
- [ ] Enable audit logging
- [ ] Regular security scans with OWASP ZAP
- [ ] Keep images up to date

## Backup & Restore

### Database Backup
```bash
# Create backup
kubectl exec postgres-0 -n climate-health -- pg_dump -U climate_admin climate_health_db > backup-$(date +%Y%m%d).sql

# Restore backup
kubectl exec -i postgres-0 -n climate-health -- psql -U climate_admin -d climate_health_db < backup-20240101.sql
```

### Redis Backup
```bash
# Trigger save
kubectl exec deployment/redis -n climate-health -- redis-cli -a $(kubectl get secret climate-health-secrets -n climate-health -o jsonpath='{.data.REDIS_PASSWORD}' | base64 -d) SAVE

# Copy RDB file
kubectl cp climate-health/redis-0:/data/dump.rdb ./redis-backup.rdb
```

## Support & Resources

- Documentation: `infra/README.md`
- Issues: GitHub Issues
- Monitoring: Grafana dashboards
- Logs: `kubectl logs -f -l app=<service> -n climate-health`

## Next Steps

1. Configure DNS to point to your ingress IP
2. Set up SSL/TLS certificates
3. Configure external API keys
4. Set up monitoring alerts
5. Configure automated backups
6. Review and adjust resource limits
7. Set up log aggregation
8. Configure CI/CD pipelines
