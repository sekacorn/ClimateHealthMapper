#!/bin/bash

# ClimateHealthMapper Deployment Script
# This script deploys the complete ClimateHealthMapper infrastructure to Kubernetes

set -e

NAMESPACE="climate-health"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$SCRIPT_DIR/kubernetes"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please configure kubectl."
        exit 1
    fi

    log_info "Prerequisites check passed"
}

# Create namespace
create_namespace() {
    log_info "Creating namespace: $NAMESPACE"
    kubectl apply -f "$K8S_DIR/namespace.yml"
}

# Create secrets
create_secrets() {
    log_info "Checking for secrets..."

    if [ ! -f "$K8S_DIR/secrets.yml" ]; then
        log_warn "secrets.yml not found. Using template..."
        log_error "Please create secrets.yml from secrets.yml.template and fill in actual values"
        log_error "Run: cp $K8S_DIR/secrets.yml.template $K8S_DIR/secrets.yml"
        log_error "Then edit the file and rerun this script"
        exit 1
    fi

    log_info "Applying secrets..."
    kubectl apply -f "$K8S_DIR/secrets.yml"
}

# Create configmap
create_configmap() {
    log_info "Creating ConfigMap..."
    kubectl apply -f "$K8S_DIR/configmap.yml"
}

# Deploy database
deploy_database() {
    log_info "Deploying PostgreSQL..."
    kubectl apply -f "$K8S_DIR/postgres-statefulset.yml"

    log_info "Waiting for PostgreSQL to be ready..."
    kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s || {
        log_error "PostgreSQL failed to start"
        kubectl logs -l app=postgres -n $NAMESPACE --tail=50
        exit 1
    }

    log_info "PostgreSQL is ready"
}

# Deploy cache
deploy_cache() {
    log_info "Deploying Redis..."
    kubectl apply -f "$K8S_DIR/redis-deployment.yml"

    log_info "Waiting for Redis to be ready..."
    kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=300s || {
        log_error "Redis failed to start"
        kubectl logs -l app=redis -n $NAMESPACE --tail=50
        exit 1
    }

    log_info "Redis is ready"
}

# Deploy services
deploy_services() {
    log_info "Deploying microservices..."

    local services=(
        "api-gateway"
        "climate-integrator"
        "climate-visualizer"
        "user-session"
        "llm-service"
        "collaboration-service"
        "ai-model"
        "frontend"
    )

    for service in "${services[@]}"; do
        log_info "Deploying $service..."
        kubectl apply -f "$K8S_DIR/${service}-deployment.yml"
    done

    log_info "Waiting for services to be ready..."
    for service in "${services[@]}"; do
        log_info "Waiting for $service..."
        kubectl wait --for=condition=available deployment/$service -n $NAMESPACE --timeout=300s || {
            log_warn "$service deployment timed out, checking status..."
            kubectl get pods -l app=$service -n $NAMESPACE
            kubectl describe deployment/$service -n $NAMESPACE
        }
    done

    log_info "All services are deployed"
}

# Deploy ingress
deploy_ingress() {
    log_info "Deploying Ingress..."
    kubectl apply -f "$K8S_DIR/ingress.yml"
}

# Deploy autoscaling
deploy_autoscaling() {
    log_info "Deploying HorizontalPodAutoscaler..."
    kubectl apply -f "$K8S_DIR/hpa.yml"
}

# Apply network policies
apply_network_policies() {
    log_info "Applying network policies..."
    kubectl apply -f "$K8S_DIR/network-policy.yml"
}

# Show status
show_status() {
    log_info "Deployment Status:"
    echo ""

    log_info "Pods:"
    kubectl get pods -n $NAMESPACE
    echo ""

    log_info "Services:"
    kubectl get svc -n $NAMESPACE
    echo ""

    log_info "Ingress:"
    kubectl get ingress -n $NAMESPACE
    echo ""

    log_info "HPA:"
    kubectl get hpa -n $NAMESPACE
    echo ""

    log_info "PVC:"
    kubectl get pvc -n $NAMESPACE
    echo ""
}

# Cleanup function
cleanup() {
    log_warn "Cleaning up ClimateHealthMapper deployment..."

    read -p "Are you sure you want to delete all resources in namespace $NAMESPACE? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Cleanup cancelled"
        exit 0
    fi

    log_info "Deleting namespace $NAMESPACE..."
    kubectl delete namespace $NAMESPACE

    log_info "Cleanup complete"
}

# Main deployment flow
deploy_all() {
    log_info "Starting ClimateHealthMapper deployment..."

    check_prerequisites
    create_namespace
    create_secrets
    create_configmap
    deploy_database
    deploy_cache
    deploy_services
    deploy_ingress
    deploy_autoscaling
    apply_network_policies

    log_info "Deployment complete!"
    echo ""
    show_status

    log_info "Access the application at: https://climatehealthmapper.com"
}

# Parse command line arguments
case "${1:-}" in
    deploy)
        deploy_all
        ;;
    status)
        show_status
        ;;
    cleanup)
        cleanup
        ;;
    database)
        deploy_database
        ;;
    cache)
        deploy_cache
        ;;
    services)
        deploy_services
        ;;
    ingress)
        deploy_ingress
        ;;
    hpa)
        deploy_autoscaling
        ;;
    network)
        apply_network_policies
        ;;
    *)
        echo "ClimateHealthMapper Deployment Script"
        echo ""
        echo "Usage: $0 {deploy|status|cleanup|database|cache|services|ingress|hpa|network}"
        echo ""
        echo "Commands:"
        echo "  deploy      - Deploy complete infrastructure"
        echo "  status      - Show deployment status"
        echo "  cleanup     - Delete all resources"
        echo "  database    - Deploy PostgreSQL only"
        echo "  cache       - Deploy Redis only"
        echo "  services    - Deploy all microservices"
        echo "  ingress     - Deploy ingress"
        echo "  hpa         - Deploy autoscaling"
        echo "  network     - Apply network policies"
        echo ""
        exit 1
        ;;
esac
