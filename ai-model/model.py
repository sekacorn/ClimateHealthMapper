"""
PyTorch Neural Network Model for Health Risk Prediction
Multi-layer perceptron with dropout for predicting health risks
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
from typing import Dict, List, Tuple
import logging

logger = logging.getLogger(__name__)


class HealthRiskPredictor(nn.Module):
    """
    Multi-layer Perceptron for health risk prediction

    Input features include:
    - Environmental data: PM2.5, AQI, temperature, humidity, UV index, etc.
    - Health data: age, pre-existing conditions, medications, etc.
    - Genomic data: genetic markers, variants, etc.

    Output:
    - Risk scores for various health conditions (asthma, heatstroke, cardiovascular, respiratory)
    """

    def __init__(
        self,
        input_size: int = 50,
        hidden_size: int = 128,
        output_size: int = 4,
        dropout: float = 0.3,
        num_hidden_layers: int = 3
    ):
        """
        Initialize the neural network

        Args:
            input_size: Number of input features
            hidden_size: Size of hidden layers
            output_size: Number of output predictions (health conditions)
            dropout: Dropout probability for regularization
            num_hidden_layers: Number of hidden layers
        """
        super(HealthRiskPredictor, self).__init__()

        self.input_size = input_size
        self.hidden_size = hidden_size
        self.output_size = output_size
        self.dropout_prob = dropout
        self.num_hidden_layers = num_hidden_layers

        # Input layer
        self.input_layer = nn.Linear(input_size, hidden_size)
        self.input_bn = nn.BatchNorm1d(hidden_size)

        # Hidden layers
        self.hidden_layers = nn.ModuleList()
        self.hidden_bns = nn.ModuleList()

        for _ in range(num_hidden_layers):
            self.hidden_layers.append(nn.Linear(hidden_size, hidden_size))
            self.hidden_bns.append(nn.BatchNorm1d(hidden_size))

        # Output layer
        self.output_layer = nn.Linear(hidden_size, output_size)

        # Dropout
        self.dropout = nn.Dropout(dropout)

        # Initialize weights
        self._initialize_weights()

        logger.info(f"Initialized HealthRiskPredictor with {self.count_parameters()} parameters")

    def _initialize_weights(self):
        """Initialize network weights using Xavier initialization"""
        for module in self.modules():
            if isinstance(module, nn.Linear):
                nn.init.xavier_uniform_(module.weight)
                if module.bias is not None:
                    nn.init.constant_(module.bias, 0.0)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Forward pass through the network

        Args:
            x: Input tensor of shape (batch_size, input_size)

        Returns:
            Output tensor of shape (batch_size, output_size) with risk scores
        """
        # Input layer
        x = self.input_layer(x)
        x = self.input_bn(x)
        x = F.relu(x)
        x = self.dropout(x)

        # Hidden layers
        for hidden_layer, hidden_bn in zip(self.hidden_layers, self.hidden_bns):
            residual = x
            x = hidden_layer(x)
            x = hidden_bn(x)
            x = F.relu(x)
            x = self.dropout(x)
            # Residual connection
            x = x + residual

        # Output layer with sigmoid activation for probability scores
        x = self.output_layer(x)
        x = torch.sigmoid(x)

        return x

    def predict_proba(self, x: torch.Tensor) -> torch.Tensor:
        """
        Predict risk probabilities

        Args:
            x: Input tensor

        Returns:
            Risk probabilities for each condition
        """
        self.eval()
        with torch.no_grad():
            return self.forward(x)

    def predict(self, x: torch.Tensor, threshold: float = 0.5) -> torch.Tensor:
        """
        Predict binary risk labels

        Args:
            x: Input tensor
            threshold: Classification threshold

        Returns:
            Binary predictions (0 or 1) for each condition
        """
        proba = self.predict_proba(x)
        return (proba >= threshold).long()

    def count_parameters(self) -> int:
        """Count total number of trainable parameters"""
        return sum(p.numel() for p in self.parameters() if p.requires_grad)

    def get_model_info(self) -> Dict:
        """Get model information"""
        return {
            "input_size": self.input_size,
            "hidden_size": self.hidden_size,
            "output_size": self.output_size,
            "num_hidden_layers": self.num_hidden_layers,
            "dropout": self.dropout_prob,
            "total_parameters": self.count_parameters(),
            "trainable_parameters": sum(p.numel() for p in self.parameters() if p.requires_grad),
        }

    def save_model(self, filepath: str):
        """Save model state dictionary"""
        torch.save({
            'model_state_dict': self.state_dict(),
            'model_config': {
                'input_size': self.input_size,
                'hidden_size': self.hidden_size,
                'output_size': self.output_size,
                'dropout': self.dropout_prob,
                'num_hidden_layers': self.num_hidden_layers,
            }
        }, filepath)
        logger.info(f"Model saved to {filepath}")

    @classmethod
    def load_model(cls, filepath: str, device: torch.device = None) -> 'HealthRiskPredictor':
        """
        Load model from file

        Args:
            filepath: Path to model file
            device: Device to load model on

        Returns:
            Loaded model instance
        """
        if device is None:
            device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

        checkpoint = torch.load(filepath, map_location=device)
        config = checkpoint['model_config']

        model = cls(
            input_size=config['input_size'],
            hidden_size=config['hidden_size'],
            output_size=config['output_size'],
            dropout=config['dropout'],
            num_hidden_layers=config['num_hidden_layers']
        )

        model.load_state_dict(checkpoint['model_state_dict'])
        model.to(device)
        model.eval()

        logger.info(f"Model loaded from {filepath}")
        return model


class EnsembleHealthRiskPredictor:
    """
    Ensemble of multiple HealthRiskPredictor models for improved predictions
    """

    def __init__(self, models: List[HealthRiskPredictor]):
        """
        Initialize ensemble

        Args:
            models: List of trained HealthRiskPredictor models
        """
        self.models = models
        self.num_models = len(models)

        logger.info(f"Initialized ensemble with {self.num_models} models")

    def predict_proba(self, x: torch.Tensor) -> torch.Tensor:
        """
        Predict using ensemble (average predictions)

        Args:
            x: Input tensor

        Returns:
            Average risk probabilities across all models
        """
        predictions = []
        for model in self.models:
            predictions.append(model.predict_proba(x))

        # Average predictions
        ensemble_pred = torch.stack(predictions).mean(dim=0)
        return ensemble_pred

    def predict(self, x: torch.Tensor, threshold: float = 0.5) -> torch.Tensor:
        """
        Predict binary labels using ensemble

        Args:
            x: Input tensor
            threshold: Classification threshold

        Returns:
            Binary predictions
        """
        proba = self.predict_proba(x)
        return (proba >= threshold).long()
