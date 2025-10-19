# ClimateHealthMapper AI Model Service

AI-powered health risk prediction service for the ClimateHealthMapper platform. Uses deep learning to predict health risks based on environmental conditions, personal health data, and genomic information.

## Features

- **Multi-input Health Risk Prediction**: Combines environmental, health, and genomic data
- **PyTorch Neural Network**: Deep learning model with residual connections and dropout
- **FastAPI REST API**: High-performance async API endpoints
- **Batch Processing**: Support for bulk predictions (up to 1000 at once)
- **Redis Caching**: Fast response times with intelligent caching
- **GPU Support**: CUDA-enabled for accelerated inference
- **Multiprocessing**: Automatic scaling based on system resources
- **Cross-validation**: Robust model training with k-fold CV
- **Comprehensive Testing**: Full test suite with pytest

## Predicted Health Risks

The model predicts risk scores (0-1) for:

1. **Asthma Risk**: Based on air quality, pollen, and personal history
2. **Heatstroke Risk**: Based on temperature, humidity, age, and health status
3. **Cardiovascular Risk**: Based on pollution, stress factors, and heart health
4. **Respiratory Risk**: Based on air quality, smoking status, and lung health

## Architecture

### Model Architecture

```
Input Layer (50 features)
    ↓
Dense + BatchNorm + ReLU + Dropout
    ↓
Hidden Layers (3x128 units) with Residual Connections
    ↓
Output Layer (4 risk scores)
    ↓
Sigmoid Activation (0-1 probabilities)
```

### Input Features

**Environmental Data (12 features)**:
- PM2.5, PM10, AQI
- Temperature, Humidity, UV Index
- Ozone, NO2, SO2, CO
- Wind Speed, Precipitation

**Health Data (11 features)**:
- Age, BMI, Heart Rate
- Blood Pressure (Systolic/Diastolic)
- Pre-existing Conditions (Asthma, COPD, Heart Disease, Diabetes)
- Lifestyle (Smoker, Exercise Frequency)

**Genomic Data (6 features)**:
- 5 Gene Variants
- Genetic Risk Score

**Engineered Features (20+ features)**:
- Heat Index, Air Quality Composite
- Age Interactions, Comorbidity Count
- Environmental Exposure Score
- And more...

## Installation

### Requirements

- Python 3.11+
- PostgreSQL 13+
- Redis 6+
- CUDA 11.8+ (optional, for GPU support)

### Setup

1. **Clone the repository**:
```bash
cd ai-model
```

2. **Create virtual environment**:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. **Install dependencies**:
```bash
pip install -r requirements.txt
```

4. **Configure environment**:
```bash
cp .env.example .env
# Edit .env with your settings
```

5. **Train the model** (optional):
```bash
python train_model.py --cross-validate
```

## Usage

### Start the API Server

```bash
python health_predictor.py
```

Or with uvicorn directly:
```bash
uvicorn health_predictor:app --host 0.0.0.0 --port 8001 --workers 4
```

### Docker Deployment

**Build the image**:
```bash
docker build -t climatehealthmapper-ai:latest .
```

**Run the container**:
```bash
docker run -d \
  -p 8001:8001 \
  --env-file .env \
  --name climatehealthmapper-ai \
  climatehealthmapper-ai:latest
```

**With GPU support**:
```bash
docker run -d \
  -p 8001:8001 \
  --gpus all \
  --env-file .env \
  --name climatehealthmapper-ai \
  climatehealthmapper-ai:latest
```

## API Endpoints

### Health Check
```http
GET /health
```

Response:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00",
  "model_loaded": true,
  "processor_loaded": true,
  "device": "cuda",
  "version": "1.0.0"
}
```

### Model Info
```http
GET /model/info
```

Response:
```json
{
  "model_name": "HealthRiskPredictor",
  "model_version": "1.0.0",
  "input_size": 50,
  "output_size": 4,
  "total_parameters": 85000,
  "device": "cuda"
}
```

### Single Prediction
```http
POST /predict
Content-Type: application/json

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

Response:
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
  "timestamp": "2024-01-15T10:35:00",
  "user_id": "user123",
  "location_id": "loc456",
  "model_version": "1.0.0"
}
```

### Batch Prediction
```http
POST /predict/batch
Content-Type: application/json

{
  "predictions": [
    { /* prediction input 1 */ },
    { /* prediction input 2 */ },
    { /* prediction input 3 */ }
  ]
}
```

## Model Training

### Train from Database

```bash
python train_model.py --cross-validate
```

This will:
1. Load data from PostgreSQL
2. Perform data preprocessing and feature engineering
3. Train model with 5-fold cross-validation
4. Save trained model to `model.pt`
5. Save data processor to `data_processor.pkl`

### Training Configuration

Edit `.env` or set environment variables:

```bash
LEARNING_RATE=0.001
NUM_EPOCHS=100
EARLY_STOPPING_PATIENCE=10
VALIDATION_SPLIT=0.2
CV_FOLDS=5
BATCH_SIZE=32
```

## Testing

Run the test suite:

```bash
pytest tests/ -v
```

Run with coverage:

```bash
pytest tests/ --cov=. --cov-report=html
```

## Performance

### System Requirements

**Minimum**:
- CPU: 2 cores
- RAM: 4GB
- Disk: 2GB

**Recommended**:
- CPU: 8 cores
- RAM: 16GB
- GPU: NVIDIA GPU with 4GB+ VRAM
- Disk: 10GB SSD

### Benchmarks

- **Single Prediction**: <50ms (CPU), <10ms (GPU)
- **Batch Prediction (100)**: <200ms (CPU), <50ms (GPU)
- **Throughput**: 2000+ requests/sec (multi-worker)

### Optimization

The service automatically:
- Enables multiprocessing if CPU cores > 4
- Uses GPU if CUDA is available
- Caches predictions in Redis
- Batches database queries

## Monitoring

### Logs

Logs are written to `ai_model.log` and stdout:

```bash
tail -f ai_model.log
```

### Metrics

The API exposes standard metrics at `/health`:
- Model load status
- Device information
- Uptime
- Version

## Troubleshooting

### Model not loading

Check that `model.pt` and `data_processor.pkl` exist:
```bash
ls -la *.pt *.pkl
```

If missing, train the model:
```bash
python train_model.py
```

### Database connection errors

Verify PostgreSQL is running and credentials are correct:
```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME
```

### Redis connection errors

Verify Redis is running:
```bash
redis-cli ping
```

### GPU not detected

Check CUDA installation:
```bash
python -c "import torch; print(torch.cuda.is_available())"
```

## Development

### Project Structure

```
ai-model/
├── health_predictor.py     # FastAPI application
├── model.py                # PyTorch model
├── data_processor.py       # Data preprocessing
├── train_model.py          # Training script
├── config.py               # Configuration
├── requirements.txt        # Dependencies
├── Dockerfile              # Docker image
├── .env.example            # Environment template
├── tests/                  # Test suite
│   └── test_health_predictor.py
└── README.md               # Documentation
```

### Adding New Features

1. Add feature to `DataProcessor.ENVIRONMENTAL_FEATURES`, `HEALTH_FEATURES`, or `GENOMIC_FEATURES`
2. Update feature engineering in `_engineer_features()`
3. Retrain model with new features
4. Update API documentation

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and add tests
4. Run test suite
5. Submit pull request

## License

Copyright (c) 2024 ClimateHealthMapper

## Support

For issues and questions:
- GitHub Issues: [link]
- Email: support@climatehealthmapper.com
- Documentation: [link]

## Acknowledgments

- PyTorch for deep learning framework
- FastAPI for web framework
- scikit-learn for preprocessing tools
- Redis for caching
- PostgreSQL for data storage
