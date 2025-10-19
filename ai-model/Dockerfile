# Multi-stage Docker build for AI Model Service
# Supports both CPU and GPU deployments

# Stage 1: Base image with Python
FROM python:3.11-slim as base

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Stage 2: Dependencies
FROM base as dependencies

# Copy requirements
COPY requirements.txt .

# Install Python dependencies
RUN pip install --upgrade pip && \
    pip install -r requirements.txt

# Stage 3: GPU Support (optional)
FROM dependencies as gpu-support

# Install CUDA-enabled PyTorch (if needed)
# Uncomment for GPU support
# RUN pip install torch==2.1.0+cu118 -f https://download.pytorch.org/whl/torch_stable.html

# Stage 4: Application
FROM dependencies as application

# Copy application code
COPY . .

# Create directories
RUN mkdir -p /app/logs /app/models

# Create non-root user
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Default command
CMD ["python", "-m", "uvicorn", "health_predictor:app", "--host", "0.0.0.0", "--port", "8001"]

# Alternative commands for different scenarios:
# Training: CMD ["python", "train_model.py"]
# Multi-worker: CMD ["python", "-m", "uvicorn", "health_predictor:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "4"]
# GPU: CMD ["python", "-m", "uvicorn", "health_predictor:app", "--host", "0.0.0.0", "--port", "8001"]
