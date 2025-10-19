"""
Model Training Script
Loads data from PostgreSQL, trains PyTorch model with cross-validation
"""

import sys
import logging
import argparse
from pathlib import Path
from typing import Tuple, List, Dict
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
from sklearn.model_selection import train_test_split, KFold
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score
import psycopg2
from psycopg2.extras import RealDictCursor

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


class EarlyStopping:
    """Early stopping to prevent overfitting"""

    def __init__(self, patience: int = 10, min_delta: float = 0.0, mode: str = 'min'):
        """
        Args:
            patience: Number of epochs to wait before stopping
            min_delta: Minimum change to qualify as improvement
            mode: 'min' for loss, 'max' for accuracy
        """
        self.patience = patience
        self.min_delta = min_delta
        self.mode = mode
        self.counter = 0
        self.best_score = None
        self.early_stop = False
        self.best_model_state = None

    def __call__(self, score: float, model: nn.Module) -> bool:
        """
        Check if training should stop

        Args:
            score: Current score (loss or accuracy)
            model: Current model

        Returns:
            True if should stop, False otherwise
        """
        if self.best_score is None:
            self.best_score = score
            self.best_model_state = model.state_dict().copy()
        elif self._is_improvement(score):
            self.best_score = score
            self.best_model_state = model.state_dict().copy()
            self.counter = 0
        else:
            self.counter += 1
            if self.counter >= self.patience:
                self.early_stop = True
                logger.info(f"Early stopping triggered after {self.counter} epochs without improvement")

        return self.early_stop

    def _is_improvement(self, score: float) -> bool:
        """Check if score is an improvement"""
        if self.mode == 'min':
            return score < self.best_score - self.min_delta
        else:
            return score > self.best_score + self.min_delta

    def load_best_model(self, model: nn.Module):
        """Load best model state"""
        if self.best_model_state is not None:
            model.load_state_dict(self.best_model_state)


def load_data_from_db() -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Load training data from PostgreSQL database

    Returns:
        Tuple of (features DataFrame, targets DataFrame)
    """
    logger.info("Loading data from PostgreSQL...")

    try:
        conn = psycopg2.connect(
            host=settings.DB_HOST,
            port=settings.DB_PORT,
            dbname=settings.DB_NAME,
            user=settings.DB_USER,
            password=settings.DB_PASSWORD
        )

        # Load environmental data
        query_env = """
            SELECT
                user_id, location_id, timestamp,
                pm25, pm10, aqi, temperature, humidity, uv_index,
                ozone, no2, so2, co, wind_speed, precipitation
            FROM environmental_data
            WHERE timestamp > NOW() - INTERVAL '1 year'
        """
        df_env = pd.read_sql(query_env, conn)

        # Load health data
        query_health = """
            SELECT
                user_id, age, bmi, heart_rate,
                blood_pressure_systolic, blood_pressure_diastolic,
                has_asthma, has_copd, has_heart_disease, has_diabetes,
                smoker, exercise_frequency
            FROM user_health_profiles
        """
        df_health = pd.read_sql(query_health, conn)

        # Load genomic data
        query_genomic = """
            SELECT
                user_id, gene_variant_1, gene_variant_2, gene_variant_3,
                gene_variant_4, gene_variant_5, genetic_risk_score
            FROM user_genomic_data
        """
        df_genomic = pd.read_sql(query_genomic, conn)

        # Load health outcomes (targets)
        query_outcomes = """
            SELECT
                user_id, location_id, timestamp,
                asthma_risk, heatstroke_risk, cardiovascular_risk, respiratory_risk
            FROM health_outcomes
            WHERE timestamp > NOW() - INTERVAL '1 year'
        """
        df_outcomes = pd.read_sql(query_outcomes, conn)

        conn.close()

        # Merge datasets
        df = df_env.merge(df_health, on='user_id', how='inner')
        df = df.merge(df_genomic, on='user_id', how='left')
        df = df.merge(df_outcomes, on=['user_id', 'location_id', 'timestamp'], how='inner')

        logger.info(f"Loaded {len(df)} records from database")

        # Separate features and targets
        feature_cols = (
            DataProcessor.ENVIRONMENTAL_FEATURES +
            DataProcessor.HEALTH_FEATURES +
            DataProcessor.GENOMIC_FEATURES
        )
        target_cols = DataProcessor.TARGET_CONDITIONS

        # Filter to available columns
        feature_cols = [c for c in feature_cols if c in df.columns]
        target_cols = [c for c in target_cols if c in df.columns]

        X = df[feature_cols]
        y = df[target_cols]

        logger.info(f"Features shape: {X.shape}, Targets shape: {y.shape}")

        return X, y

    except Exception as e:
        logger.error(f"Error loading data from database: {e}")
        # Return dummy data for testing
        logger.warning("Generating dummy data for testing...")
        return generate_dummy_data()


def generate_dummy_data(n_samples: int = 1000) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Generate dummy data for testing

    Args:
        n_samples: Number of samples to generate

    Returns:
        Tuple of (features DataFrame, targets DataFrame)
    """
    np.random.seed(42)

    # Generate features
    data = {
        'pm25': np.random.uniform(0, 100, n_samples),
        'pm10': np.random.uniform(0, 200, n_samples),
        'aqi': np.random.uniform(0, 200, n_samples),
        'temperature': np.random.uniform(-10, 45, n_samples),
        'humidity': np.random.uniform(20, 100, n_samples),
        'uv_index': np.random.uniform(0, 11, n_samples),
        'ozone': np.random.uniform(0, 0.2, n_samples),
        'no2': np.random.uniform(0, 100, n_samples),
        'so2': np.random.uniform(0, 50, n_samples),
        'co': np.random.uniform(0, 10, n_samples),
        'wind_speed': np.random.uniform(0, 30, n_samples),
        'precipitation': np.random.uniform(0, 50, n_samples),
        'age': np.random.randint(1, 90, n_samples),
        'bmi': np.random.uniform(15, 40, n_samples),
        'heart_rate': np.random.uniform(60, 100, n_samples),
        'blood_pressure_systolic': np.random.uniform(90, 180, n_samples),
        'blood_pressure_diastolic': np.random.uniform(60, 120, n_samples),
        'has_asthma': np.random.randint(0, 2, n_samples),
        'has_copd': np.random.randint(0, 2, n_samples),
        'has_heart_disease': np.random.randint(0, 2, n_samples),
        'has_diabetes': np.random.randint(0, 2, n_samples),
        'smoker': np.random.randint(0, 2, n_samples),
        'exercise_frequency': np.random.randint(0, 7, n_samples),
        'gene_variant_1': np.random.randint(0, 3, n_samples),
        'gene_variant_2': np.random.randint(0, 3, n_samples),
        'gene_variant_3': np.random.randint(0, 3, n_samples),
        'gene_variant_4': np.random.randint(0, 3, n_samples),
        'gene_variant_5': np.random.randint(0, 3, n_samples),
        'genetic_risk_score': np.random.uniform(0, 1, n_samples),
    }

    X = pd.DataFrame(data)

    # Generate targets (correlated with features)
    targets = {
        'asthma_risk': (
            0.3 * (X['pm25'] / 100) +
            0.2 * X['has_asthma'] +
            0.1 * (X['age'] < 18).astype(int) +
            np.random.uniform(0, 0.2, n_samples)
        ).clip(0, 1),
        'heatstroke_risk': (
            0.4 * (X['temperature'] / 45) +
            0.2 * (X['age'] > 65).astype(int) +
            0.1 * (X['humidity'] / 100) +
            np.random.uniform(0, 0.2, n_samples)
        ).clip(0, 1),
        'cardiovascular_risk': (
            0.3 * X['has_heart_disease'] +
            0.2 * (X['age'] / 90) +
            0.1 * (X['pm25'] / 100) +
            0.1 * X['smoker'] +
            np.random.uniform(0, 0.2, n_samples)
        ).clip(0, 1),
        'respiratory_risk': (
            0.3 * (X['aqi'] / 200) +
            0.2 * (X['has_copd'] + X['has_asthma']) +
            0.1 * X['smoker'] +
            np.random.uniform(0, 0.2, n_samples)
        ).clip(0, 1),
    }

    y = pd.DataFrame(targets)

    return X, y


def train_epoch(
    model: nn.Module,
    train_loader: DataLoader,
    criterion: nn.Module,
    optimizer: optim.Optimizer,
    device: torch.device
) -> float:
    """Train for one epoch"""
    model.train()
    total_loss = 0.0

    for batch_X, batch_y in train_loader:
        batch_X, batch_y = batch_X.to(device), batch_y.to(device)

        optimizer.zero_grad()
        outputs = model(batch_X)
        loss = criterion(outputs, batch_y)
        loss.backward()
        optimizer.step()

        total_loss += loss.item()

    return total_loss / len(train_loader)


def validate_epoch(
    model: nn.Module,
    val_loader: DataLoader,
    criterion: nn.Module,
    device: torch.device
) -> Tuple[float, Dict[str, float]]:
    """Validate for one epoch"""
    model.eval()
    total_loss = 0.0
    all_preds = []
    all_targets = []

    with torch.no_grad():
        for batch_X, batch_y in val_loader:
            batch_X, batch_y = batch_X.to(device), batch_y.to(device)

            outputs = model(batch_X)
            loss = criterion(outputs, batch_y)

            total_loss += loss.item()
            all_preds.append(outputs.cpu().numpy())
            all_targets.append(batch_y.cpu().numpy())

    avg_loss = total_loss / len(val_loader)

    # Calculate metrics
    all_preds = np.vstack(all_preds)
    all_targets = np.vstack(all_targets)

    metrics = {
        'loss': avg_loss,
        'auc': roc_auc_score(all_targets, all_preds, average='macro'),
    }

    return avg_loss, metrics


def train_model(
    model: nn.Module,
    train_loader: DataLoader,
    val_loader: DataLoader,
    device: torch.device,
    num_epochs: int = 100,
    learning_rate: float = 0.001,
    patience: int = 10
) -> Tuple[nn.Module, List[Dict]]:
    """
    Train the model

    Returns:
        Tuple of (trained model, training history)
    """
    criterion = nn.BCELoss()
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode='min', patience=5, factor=0.5)
    early_stopping = EarlyStopping(patience=patience, mode='min')

    history = []

    logger.info(f"Starting training for {num_epochs} epochs...")

    for epoch in range(num_epochs):
        train_loss = train_epoch(model, train_loader, criterion, optimizer, device)
        val_loss, val_metrics = validate_epoch(model, val_loader, criterion, device)

        scheduler.step(val_loss)

        history.append({
            'epoch': epoch + 1,
            'train_loss': train_loss,
            'val_loss': val_loss,
            **val_metrics
        })

        if (epoch + 1) % 10 == 0:
            logger.info(
                f"Epoch {epoch + 1}/{num_epochs} - "
                f"Train Loss: {train_loss:.4f}, Val Loss: {val_loss:.4f}, "
                f"Val AUC: {val_metrics['auc']:.4f}"
            )

        if early_stopping(val_loss, model):
            logger.info(f"Early stopping at epoch {epoch + 1}")
            break

    # Load best model
    early_stopping.load_best_model(model)

    return model, history


def cross_validate(
    X: np.ndarray,
    y: np.ndarray,
    n_folds: int = 5,
    device: torch.device = None
) -> List[Dict]:
    """
    Perform k-fold cross-validation

    Returns:
        List of fold results
    """
    if device is None:
        device = torch.device('cuda' if torch.cuda.is_available() and settings.USE_CUDA else 'cpu')

    kfold = KFold(n_splits=n_folds, shuffle=True, random_state=42)
    fold_results = []

    logger.info(f"Starting {n_folds}-fold cross-validation...")

    for fold, (train_idx, val_idx) in enumerate(kfold.split(X)):
        logger.info(f"Training fold {fold + 1}/{n_folds}...")

        # Split data
        X_train, X_val = X[train_idx], X[val_idx]
        y_train, y_val = y[train_idx], y[val_idx]

        # Create datasets and loaders
        train_dataset = TensorDataset(
            torch.FloatTensor(X_train),
            torch.FloatTensor(y_train)
        )
        val_dataset = TensorDataset(
            torch.FloatTensor(X_val),
            torch.FloatTensor(y_val)
        )

        train_loader = DataLoader(train_dataset, batch_size=settings.BATCH_SIZE, shuffle=True)
        val_loader = DataLoader(val_dataset, batch_size=settings.BATCH_SIZE, shuffle=False)

        # Create and train model
        model = HealthRiskPredictor(
            input_size=X.shape[1],
            hidden_size=settings.MODEL_HIDDEN_SIZE,
            output_size=y.shape[1],
            dropout=settings.MODEL_DROPOUT
        ).to(device)

        model, history = train_model(
            model, train_loader, val_loader, device,
            num_epochs=settings.NUM_EPOCHS,
            learning_rate=settings.LEARNING_RATE,
            patience=settings.EARLY_STOPPING_PATIENCE
        )

        # Evaluate
        _, metrics = validate_epoch(model, val_loader, nn.BCELoss(), device)

        fold_results.append({
            'fold': fold + 1,
            'metrics': metrics,
            'history': history
        })

        logger.info(f"Fold {fold + 1} - Val AUC: {metrics['auc']:.4f}")

    # Calculate average metrics
    avg_auc = np.mean([r['metrics']['auc'] for r in fold_results])
    logger.info(f"Average CV AUC: {avg_auc:.4f}")

    return fold_results


def main(args):
    """Main training function"""

    # Set device
    device = torch.device('cuda' if torch.cuda.is_available() and settings.USE_CUDA else 'cpu')
    logger.info(f"Using device: {device}")

    # Load data
    X, y = load_data_from_db()

    # Initialize data processor
    processor = DataProcessor(
        scaler_type='standard',
        imputer_type='knn',
        feature_engineering=True
    )

    # Fit and transform data
    X_processed = processor.fit_transform(X, y)
    y_processed = y.values

    logger.info(f"Processed data shape: X={X_processed.shape}, y={y_processed.shape}")

    # Save processor
    processor_path = Path(__file__).parent / 'data_processor.pkl'
    processor.save(str(processor_path))

    if args.cross_validate:
        # Perform cross-validation
        cv_results = cross_validate(
            X_processed, y_processed,
            n_folds=settings.CV_FOLDS,
            device=device
        )

    # Train final model on all data
    logger.info("Training final model on all data...")

    # Split data
    X_train, X_val, y_train, y_val = train_test_split(
        X_processed, y_processed,
        test_size=settings.VALIDATION_SPLIT,
        random_state=42
    )

    # Create datasets and loaders
    train_dataset = TensorDataset(
        torch.FloatTensor(X_train),
        torch.FloatTensor(y_train)
    )
    val_dataset = TensorDataset(
        torch.FloatTensor(X_val),
        torch.FloatTensor(y_val)
    )

    train_loader = DataLoader(train_dataset, batch_size=settings.BATCH_SIZE, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=settings.BATCH_SIZE, shuffle=False)

    # Create model
    model = HealthRiskPredictor(
        input_size=X_processed.shape[1],
        hidden_size=settings.MODEL_HIDDEN_SIZE,
        output_size=y_processed.shape[1],
        dropout=settings.MODEL_DROPOUT
    ).to(device)

    logger.info(f"Model: {model.get_model_info()}")

    # Train model
    model, history = train_model(
        model, train_loader, val_loader, device,
        num_epochs=settings.NUM_EPOCHS,
        learning_rate=settings.LEARNING_RATE,
        patience=settings.EARLY_STOPPING_PATIENCE
    )

    # Save model
    model_path = settings.model_file_path
    model.save_model(str(model_path))

    logger.info("Training completed successfully!")
    logger.info(f"Model saved to: {model_path}")
    logger.info(f"Data processor saved to: {processor_path}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Train Health Risk Prediction Model')
    parser.add_argument('--cross-validate', action='store_true', help='Perform cross-validation')
    args = parser.parse_args()

    main(args)
