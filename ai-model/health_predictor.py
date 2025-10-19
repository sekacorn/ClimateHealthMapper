"""
FastAPI Application for Health Risk Prediction
Provides REST API endpoints for health risk predictions
"""

import sys
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any
import asyncio
from datetime import datetime
import json

import torch
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException, BackgroundTasks, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, validator
import redis
from contextlib import asynccontextmanager

from config import settings
from model import HealthRiskPredictor
from data_processor import DataProcessor

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(settings.LOG_FILE) if settings.LOG_FILE else logging.StreamHandler(),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Global variables
model: Optional[HealthRiskPredictor] = None
processor: Optional[DataProcessor] = None
device: torch.device = None
redis_client: Optional[redis.Redis] = None


# Pydantic Models
class EnvironmentalData(BaseModel):
    """Environmental data input"""
    pm25: Optional[float] = Field(None, ge=0, le=500, description="PM2.5 level (μg/m³)")
    pm10: Optional[float] = Field(None, ge=0, le=1000, description="PM10 level (μg/m³)")
    aqi: Optional[float] = Field(None, ge=0, le=500, description="Air Quality Index")
    temperature: Optional[float] = Field(None, ge=-50, le=60, description="Temperature (°C)")
    humidity: Optional[float] = Field(None, ge=0, le=100, description="Humidity (%)")
    uv_index: Optional[float] = Field(None, ge=0, le=15, description="UV Index")
    ozone: Optional[float] = Field(None, ge=0, le=1, description="Ozone level (ppm)")
    no2: Optional[float] = Field(None, ge=0, le=200, description="NO2 level (ppb)")
    so2: Optional[float] = Field(None, ge=0, le=200, description="SO2 level (ppb)")
    co: Optional[float] = Field(None, ge=0, le=50, description="CO level (ppm)")
    wind_speed: Optional[float] = Field(None, ge=0, le=100, description="Wind speed (km/h)")
    precipitation: Optional[float] = Field(None, ge=0, le=500, description="Precipitation (mm)")


class HealthData(BaseModel):
    """Health profile data"""
    age: Optional[int] = Field(None, ge=0, le=120, description="Age in years")
    bmi: Optional[float] = Field(None, ge=10, le=60, description="Body Mass Index")
    heart_rate: Optional[float] = Field(None, ge=30, le=200, description="Heart rate (bpm)")
    blood_pressure_systolic: Optional[float] = Field(None, ge=60, le=250, description="Systolic BP (mmHg)")
    blood_pressure_diastolic: Optional[float] = Field(None, ge=40, le=150, description="Diastolic BP (mmHg)")
    has_asthma: Optional[int] = Field(0, ge=0, le=1, description="Has asthma (0/1)")
    has_copd: Optional[int] = Field(0, ge=0, le=1, description="Has COPD (0/1)")
    has_heart_disease: Optional[int] = Field(0, ge=0, le=1, description="Has heart disease (0/1)")
    has_diabetes: Optional[int] = Field(0, ge=0, le=1, description="Has diabetes (0/1)")
    smoker: Optional[int] = Field(0, ge=0, le=1, description="Is smoker (0/1)")
    exercise_frequency: Optional[int] = Field(0, ge=0, le=7, description="Exercise days per week")


class GenomicData(BaseModel):
    """Genomic data"""
    gene_variant_1: Optional[int] = Field(0, ge=0, le=2, description="Gene variant 1")
    gene_variant_2: Optional[int] = Field(0, ge=0, le=2, description="Gene variant 2")
    gene_variant_3: Optional[int] = Field(0, ge=0, le=2, description="Gene variant 3")
    gene_variant_4: Optional[int] = Field(0, ge=0, le=2, description="Gene variant 4")
    gene_variant_5: Optional[int] = Field(0, ge=0, le=2, description="Gene variant 5")
    genetic_risk_score: Optional[float] = Field(0, ge=0, le=1, description="Genetic risk score")


class PredictionInput(BaseModel):
    """Complete prediction input"""
    environmental: EnvironmentalData
    health: HealthData
    genomic: Optional[GenomicData] = None
    user_id: Optional[str] = None
    location_id: Optional[str] = None


class BatchPredictionInput(BaseModel):
    """Batch prediction input"""
    predictions: List[PredictionInput] = Field(..., max_items=1000)

    @validator('predictions')
    def validate_batch_size(cls, v):
        if len(v) > settings.MAX_BATCH_SIZE:
            raise ValueError(f"Batch size cannot exceed {settings.MAX_BATCH_SIZE}")
        return v


class HealthRiskOutput(BaseModel):
    """Health risk prediction output"""
    asthma_risk: float = Field(..., ge=0, le=1, description="Asthma risk score (0-1)")
    heatstroke_risk: float = Field(..., ge=0, le=1, description="Heatstroke risk score (0-1)")
    cardiovascular_risk: float = Field(..., ge=0, le=1, description="Cardiovascular risk score (0-1)")
    respiratory_risk: float = Field(..., ge=0, le=1, description="Respiratory risk score (0-1)")
    overall_risk: float = Field(..., ge=0, le=1, description="Overall risk score (0-1)")
    risk_level: str = Field(..., description="Risk level: low, moderate, high, critical")

    @validator('risk_level', always=True)
    def compute_risk_level(cls, v, values):
        """Compute risk level from overall risk"""
        overall_risk = values.get('overall_risk', 0)
        if overall_risk < 0.3:
            return "low"
        elif overall_risk < 0.5:
            return "moderate"
        elif overall_risk < 0.7:
            return "high"
        else:
            return "critical"


class PredictionOutput(BaseModel):
    """Complete prediction output"""
    risks: HealthRiskOutput
    timestamp: str
    user_id: Optional[str] = None
    location_id: Optional[str] = None
    model_version: str


class BatchPredictionOutput(BaseModel):
    """Batch prediction output"""
    predictions: List[PredictionOutput]
    total_count: int
    timestamp: str


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    timestamp: str
    model_loaded: bool
    processor_loaded: bool
    device: str
    version: str


class ModelInfo(BaseModel):
    """Model information response"""
    model_name: str
    model_version: str
    input_size: int
    output_size: int
    total_parameters: int
    device: str
    last_trained: Optional[str] = None


# Lifespan context manager
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    # Startup
    logger.info("Starting up AI Model Service...")
    await load_model_and_processor()
    await connect_redis()
    yield
    # Shutdown
    logger.info("Shutting down AI Model Service...")
    await disconnect_redis()


# Create FastAPI app
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="AI Model Service for ClimateHealthMapper - Health Risk Prediction",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Utility functions
async def load_model_and_processor():
    """Load ML model and data processor"""
    global model, processor, device

    try:
        # Set device
        device = torch.device('cuda' if torch.cuda.is_available() and settings.USE_CUDA else 'cpu')
        logger.info(f"Using device: {device}")

        # Load processor
        processor_path = Path(__file__).parent / 'data_processor.pkl'
        if processor_path.exists():
            processor = DataProcessor.load(str(processor_path))
            logger.info("Data processor loaded successfully")
        else:
            logger.warning(f"Data processor not found at {processor_path}")
            processor = None

        # Load model
        model_path = settings.model_file_path
        if model_path.exists():
            model = HealthRiskPredictor.load_model(str(model_path), device=device)
            logger.info("Model loaded successfully")
        else:
            logger.warning(f"Model not found at {model_path}")
            model = None

    except Exception as e:
        logger.error(f"Error loading model/processor: {e}")
        model = None
        processor = None


async def connect_redis():
    """Connect to Redis cache"""
    global redis_client

    try:
        redis_client = redis.Redis(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            db=settings.REDIS_DB,
            password=settings.REDIS_PASSWORD,
            decode_responses=True
        )
        redis_client.ping()
        logger.info("Connected to Redis successfully")
    except Exception as e:
        logger.warning(f"Could not connect to Redis: {e}")
        redis_client = None


async def disconnect_redis():
    """Disconnect from Redis"""
    global redis_client
    if redis_client:
        redis_client.close()
        logger.info("Disconnected from Redis")


def prepare_input_data(prediction_input: PredictionInput) -> Dict[str, Any]:
    """Prepare input data dictionary from prediction input"""
    data = {}

    # Environmental data
    if prediction_input.environmental:
        data.update(prediction_input.environmental.dict(exclude_none=True))

    # Health data
    if prediction_input.health:
        data.update(prediction_input.health.dict(exclude_none=True))

    # Genomic data
    if prediction_input.genomic:
        data.update(prediction_input.genomic.dict(exclude_none=True))

    return data


async def get_cached_prediction(cache_key: str) -> Optional[Dict]:
    """Get cached prediction from Redis"""
    if not redis_client:
        return None

    try:
        cached = redis_client.get(cache_key)
        if cached:
            logger.debug(f"Cache hit for key: {cache_key}")
            return json.loads(cached)
    except Exception as e:
        logger.error(f"Error reading from cache: {e}")

    return None


async def cache_prediction(cache_key: str, prediction: Dict):
    """Cache prediction in Redis"""
    if not redis_client:
        return

    try:
        redis_client.setex(
            cache_key,
            settings.CACHE_TTL,
            json.dumps(prediction)
        )
        logger.debug(f"Cached prediction for key: {cache_key}")
    except Exception as e:
        logger.error(f"Error writing to cache: {e}")


def generate_cache_key(data: Dict) -> str:
    """Generate cache key from input data"""
    # Sort keys and create hash
    import hashlib
    sorted_data = json.dumps(data, sort_keys=True)
    return f"prediction:{hashlib.md5(sorted_data.encode()).hexdigest()}"


# API Endpoints
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "status": "running"
    }


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy" if model and processor else "degraded",
        timestamp=datetime.utcnow().isoformat(),
        model_loaded=model is not None,
        processor_loaded=processor is not None,
        device=str(device),
        version=settings.APP_VERSION
    )


@app.get("/model/info", response_model=ModelInfo, tags=["Model"])
async def get_model_info():
    """Get model information"""
    if not model:
        raise HTTPException(status_code=503, detail="Model not loaded")

    model_info = model.get_model_info()

    return ModelInfo(
        model_name="HealthRiskPredictor",
        model_version=settings.APP_VERSION,
        input_size=model_info['input_size'],
        output_size=model_info['output_size'],
        total_parameters=model_info['total_parameters'],
        device=str(device),
        last_trained=None
    )


@app.post("/predict", response_model=PredictionOutput, tags=["Prediction"])
async def predict(prediction_input: PredictionInput):
    """
    Predict health risks for a single input

    Returns risk scores for:
    - Asthma
    - Heatstroke
    - Cardiovascular disease
    - Respiratory disease
    """
    if not model or not processor:
        raise HTTPException(status_code=503, detail="Model or processor not loaded")

    try:
        # Prepare input data
        input_data = prepare_input_data(prediction_input)

        # Check cache
        cache_key = generate_cache_key(input_data)
        cached_result = await get_cached_prediction(cache_key)

        if cached_result:
            return PredictionOutput(**cached_result)

        # Prepare dataframe
        df = processor.prepare_input(input_data)

        # Process data
        X = processor.transform(df)

        # Convert to tensor
        X_tensor = torch.FloatTensor(X).to(device)

        # Predict
        with torch.no_grad():
            predictions = model.predict_proba(X_tensor)

        # Convert to numpy
        risks = predictions.cpu().numpy()[0]

        # Create output
        health_risks = HealthRiskOutput(
            asthma_risk=float(risks[0]),
            heatstroke_risk=float(risks[1]),
            cardiovascular_risk=float(risks[2]),
            respiratory_risk=float(risks[3]),
            overall_risk=float(risks.mean())
        )

        result = PredictionOutput(
            risks=health_risks,
            timestamp=datetime.utcnow().isoformat(),
            user_id=prediction_input.user_id,
            location_id=prediction_input.location_id,
            model_version=settings.APP_VERSION
        )

        # Cache result
        await cache_prediction(cache_key, result.dict())

        logger.info(f"Prediction completed for user_id={prediction_input.user_id}")

        return result

    except Exception as e:
        logger.error(f"Error during prediction: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/predict/batch", response_model=BatchPredictionOutput, tags=["Prediction"])
async def predict_batch(batch_input: BatchPredictionInput):
    """
    Predict health risks for multiple inputs

    Supports batch processing for up to 1000 predictions
    """
    if not model or not processor:
        raise HTTPException(status_code=503, detail="Model or processor not loaded")

    try:
        predictions = []

        # Prepare all inputs
        input_data_list = [prepare_input_data(p) for p in batch_input.predictions]

        # Create dataframe
        df = pd.DataFrame(input_data_list)

        # Ensure all features exist
        for feature in processor.feature_names:
            if feature not in df.columns:
                df[feature] = np.nan

        df = df[processor.feature_names]

        # Process data
        X = processor.transform(df)

        # Convert to tensor
        X_tensor = torch.FloatTensor(X).to(device)

        # Predict
        with torch.no_grad():
            batch_predictions = model.predict_proba(X_tensor)

        # Convert to numpy
        risks_array = batch_predictions.cpu().numpy()

        # Create outputs
        for i, (risks, pred_input) in enumerate(zip(risks_array, batch_input.predictions)):
            health_risks = HealthRiskOutput(
                asthma_risk=float(risks[0]),
                heatstroke_risk=float(risks[1]),
                cardiovascular_risk=float(risks[2]),
                respiratory_risk=float(risks[3]),
                overall_risk=float(risks.mean())
            )

            predictions.append(PredictionOutput(
                risks=health_risks,
                timestamp=datetime.utcnow().isoformat(),
                user_id=pred_input.user_id,
                location_id=pred_input.location_id,
                model_version=settings.APP_VERSION
            ))

        logger.info(f"Batch prediction completed for {len(predictions)} inputs")

        return BatchPredictionOutput(
            predictions=predictions,
            total_count=len(predictions),
            timestamp=datetime.utcnow().isoformat()
        )

    except Exception as e:
        logger.error(f"Error during batch prediction: {e}")
        raise HTTPException(status_code=500, detail=f"Batch prediction failed: {str(e)}")


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": str(exc) if settings.DEBUG else "An error occurred"
        }
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "health_predictor:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
        workers=settings.NUM_WORKERS if not settings.DEBUG else 1,
        log_level=settings.LOG_LEVEL.lower()
    )
