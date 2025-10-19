"""
Tests for Health Predictor API
"""

import pytest
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from fastapi.testclient import TestClient
from health_predictor import app
from model import HealthRiskPredictor
from data_processor import DataProcessor
import torch
import numpy as np


@pytest.fixture
def client():
    """Test client fixture"""
    return TestClient(app)


@pytest.fixture
def sample_prediction_input():
    """Sample prediction input data"""
    return {
        "environmental": {
            "pm25": 35.5,
            "pm10": 50.0,
            "aqi": 75.0,
            "temperature": 28.5,
            "humidity": 65.0,
            "uv_index": 7.0,
            "ozone": 0.05,
            "no2": 25.0,
            "so2": 10.0,
            "co": 0.8,
            "wind_speed": 12.0,
            "precipitation": 0.0
        },
        "health": {
            "age": 45,
            "bmi": 26.5,
            "heart_rate": 72.0,
            "blood_pressure_systolic": 125.0,
            "blood_pressure_diastolic": 80.0,
            "has_asthma": 0,
            "has_copd": 0,
            "has_heart_disease": 0,
            "has_diabetes": 0,
            "smoker": 0,
            "exercise_frequency": 3
        },
        "genomic": {
            "gene_variant_1": 1,
            "gene_variant_2": 0,
            "gene_variant_3": 2,
            "gene_variant_4": 1,
            "gene_variant_5": 0,
            "genetic_risk_score": 0.35
        },
        "user_id": "test_user_123",
        "location_id": "test_location_456"
    }


class TestHealthPredictorAPI:
    """Test suite for Health Predictor API"""

    def test_root_endpoint(self, client):
        """Test root endpoint"""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "service" in data
        assert "version" in data
        assert "status" in data

    def test_health_endpoint(self, client):
        """Test health check endpoint"""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "timestamp" in data
        assert "model_loaded" in data
        assert "processor_loaded" in data
        assert "device" in data
        assert "version" in data

    def test_model_info_endpoint(self, client):
        """Test model info endpoint"""
        response = client.get("/model/info")
        # May return 503 if model not loaded
        assert response.status_code in [200, 503]

        if response.status_code == 200:
            data = response.json()
            assert "model_name" in data
            assert "model_version" in data
            assert "input_size" in data
            assert "output_size" in data
            assert "total_parameters" in data

    def test_predict_endpoint(self, client, sample_prediction_input):
        """Test prediction endpoint"""
        response = client.post("/predict", json=sample_prediction_input)

        # May return 503 if model not loaded
        if response.status_code == 503:
            pytest.skip("Model not loaded")

        assert response.status_code == 200
        data = response.json()

        # Check response structure
        assert "risks" in data
        assert "timestamp" in data
        assert "user_id" in data
        assert "location_id" in data
        assert "model_version" in data

        # Check risks
        risks = data["risks"]
        assert "asthma_risk" in risks
        assert "heatstroke_risk" in risks
        assert "cardiovascular_risk" in risks
        assert "respiratory_risk" in risks
        assert "overall_risk" in risks
        assert "risk_level" in risks

        # Check risk values are in valid range
        assert 0 <= risks["asthma_risk"] <= 1
        assert 0 <= risks["heatstroke_risk"] <= 1
        assert 0 <= risks["cardiovascular_risk"] <= 1
        assert 0 <= risks["respiratory_risk"] <= 1
        assert 0 <= risks["overall_risk"] <= 1

        # Check risk level is valid
        assert risks["risk_level"] in ["low", "moderate", "high", "critical"]

    def test_predict_with_missing_fields(self, client):
        """Test prediction with minimal data"""
        minimal_input = {
            "environmental": {
                "pm25": 35.5,
                "temperature": 28.5
            },
            "health": {
                "age": 45
            }
        }

        response = client.post("/predict", json=minimal_input)

        # Should still work with missing fields
        if response.status_code == 503:
            pytest.skip("Model not loaded")

        assert response.status_code == 200

    def test_predict_with_invalid_data(self, client):
        """Test prediction with invalid data"""
        invalid_input = {
            "environmental": {
                "pm25": -10,  # Invalid: negative
                "temperature": 1000  # Invalid: too high
            },
            "health": {
                "age": 200  # Invalid: too high
            }
        }

        response = client.post("/predict", json=invalid_input)
        assert response.status_code == 422  # Validation error

    def test_batch_predict_endpoint(self, client, sample_prediction_input):
        """Test batch prediction endpoint"""
        batch_input = {
            "predictions": [
                sample_prediction_input,
                sample_prediction_input,
                sample_prediction_input
            ]
        }

        response = client.post("/predict/batch", json=batch_input)

        if response.status_code == 503:
            pytest.skip("Model not loaded")

        assert response.status_code == 200
        data = response.json()

        assert "predictions" in data
        assert "total_count" in data
        assert "timestamp" in data
        assert data["total_count"] == 3
        assert len(data["predictions"]) == 3

    def test_batch_predict_too_large(self, client, sample_prediction_input):
        """Test batch prediction with too many inputs"""
        # Create a batch that exceeds max size
        batch_input = {
            "predictions": [sample_prediction_input] * 1001
        }

        response = client.post("/predict/batch", json=batch_input)
        assert response.status_code == 422  # Validation error


class TestHealthRiskPredictorModel:
    """Test suite for HealthRiskPredictor model"""

    @pytest.fixture
    def model(self):
        """Model fixture"""
        return HealthRiskPredictor(
            input_size=50,
            hidden_size=128,
            output_size=4,
            dropout=0.3
        )

    def test_model_initialization(self, model):
        """Test model initialization"""
        assert model.input_size == 50
        assert model.hidden_size == 128
        assert model.output_size == 4
        assert model.dropout_prob == 0.3
        assert model.count_parameters() > 0

    def test_model_forward(self, model):
        """Test model forward pass"""
        batch_size = 16
        x = torch.randn(batch_size, 50)

        output = model(x)

        assert output.shape == (batch_size, 4)
        assert torch.all(output >= 0) and torch.all(output <= 1)  # Sigmoid output

    def test_model_predict_proba(self, model):
        """Test model predict_proba method"""
        x = torch.randn(10, 50)

        proba = model.predict_proba(x)

        assert proba.shape == (10, 4)
        assert torch.all(proba >= 0) and torch.all(proba <= 1)

    def test_model_predict(self, model):
        """Test model predict method"""
        x = torch.randn(10, 50)

        predictions = model.predict(x, threshold=0.5)

        assert predictions.shape == (10, 4)
        assert torch.all((predictions == 0) | (predictions == 1))

    def test_model_save_load(self, model, tmp_path):
        """Test model save and load"""
        model_path = tmp_path / "test_model.pt"

        # Save model
        model.save_model(str(model_path))
        assert model_path.exists()

        # Load model
        loaded_model = HealthRiskPredictor.load_model(str(model_path))

        # Check if loaded model has same architecture
        assert loaded_model.input_size == model.input_size
        assert loaded_model.hidden_size == model.hidden_size
        assert loaded_model.output_size == model.output_size

        # Check if predictions are the same
        x = torch.randn(5, 50)
        pred1 = model.predict_proba(x)
        pred2 = loaded_model.predict_proba(x)

        assert torch.allclose(pred1, pred2, rtol=1e-4)


class TestDataProcessor:
    """Test suite for DataProcessor"""

    @pytest.fixture
    def processor(self):
        """Processor fixture"""
        return DataProcessor(
            scaler_type='standard',
            imputer_type='knn',
            feature_engineering=True
        )

    @pytest.fixture
    def sample_data(self):
        """Sample data fixture"""
        import pandas as pd
        np.random.seed(42)

        return pd.DataFrame({
            'pm25': np.random.uniform(0, 100, 100),
            'aqi': np.random.uniform(0, 200, 100),
            'temperature': np.random.uniform(-10, 45, 100),
            'humidity': np.random.uniform(20, 100, 100),
            'age': np.random.randint(1, 90, 100),
            'bmi': np.random.uniform(15, 40, 100),
        })

    def test_processor_initialization(self, processor):
        """Test processor initialization"""
        assert processor.scaler_type == 'standard'
        assert processor.imputer_type == 'knn'
        assert processor.feature_engineering == True
        assert processor.is_fitted == False

    def test_processor_fit_transform(self, processor, sample_data):
        """Test processor fit and transform"""
        X_transformed = processor.fit_transform(sample_data)

        assert processor.is_fitted == True
        assert X_transformed.shape[0] == sample_data.shape[0]
        assert X_transformed.shape[1] >= sample_data.shape[1]  # May have engineered features

    def test_processor_transform(self, processor, sample_data):
        """Test processor transform"""
        processor.fit(sample_data)
        X_transformed = processor.transform(sample_data)

        assert X_transformed.shape[0] == sample_data.shape[0]

    def test_processor_feature_engineering(self, processor, sample_data):
        """Test feature engineering"""
        processor.fit(sample_data)

        # Should have more features after engineering
        assert len(processor.fitted_feature_names) >= len(sample_data.columns)

    def test_processor_save_load(self, processor, sample_data, tmp_path):
        """Test processor save and load"""
        processor.fit(sample_data)

        processor_path = tmp_path / "test_processor.pkl"
        processor.save(str(processor_path))
        assert processor_path.exists()

        loaded_processor = DataProcessor.load(str(processor_path))

        assert loaded_processor.is_fitted == True
        assert loaded_processor.scaler_type == processor.scaler_type
        assert loaded_processor.feature_names == processor.feature_names

    def test_processor_prepare_input(self, processor, sample_data):
        """Test prepare_input method"""
        processor.fit(sample_data)

        input_dict = {
            'pm25': 35.5,
            'aqi': 75.0,
            'temperature': 28.5,
            'humidity': 65.0,
            'age': 45,
            'bmi': 26.5
        }

        df = processor.prepare_input(input_dict)

        assert len(df) == 1
        assert all(col in df.columns for col in processor.feature_names)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
