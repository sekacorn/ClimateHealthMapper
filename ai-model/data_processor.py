"""
Data Preprocessing Module
Handles feature engineering, normalization, and missing value handling
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Tuple, Optional, Any
from sklearn.preprocessing import StandardScaler, RobustScaler
from sklearn.impute import SimpleImputer, KNNImputer
import logging
import pickle
from pathlib import Path

logger = logging.getLogger(__name__)


class DataProcessor:
    """
    Data preprocessing pipeline for health risk prediction
    Handles feature engineering, normalization, and missing values
    """

    # Feature categories
    ENVIRONMENTAL_FEATURES = [
        'pm25', 'pm10', 'aqi', 'temperature', 'humidity', 'uv_index',
        'ozone', 'no2', 'so2', 'co', 'wind_speed', 'precipitation'
    ]

    HEALTH_FEATURES = [
        'age', 'bmi', 'heart_rate', 'blood_pressure_systolic',
        'blood_pressure_diastolic', 'has_asthma', 'has_copd',
        'has_heart_disease', 'has_diabetes', 'smoker', 'exercise_frequency'
    ]

    GENOMIC_FEATURES = [
        'gene_variant_1', 'gene_variant_2', 'gene_variant_3',
        'gene_variant_4', 'gene_variant_5', 'genetic_risk_score'
    ]

    # Target variables (health conditions)
    TARGET_CONDITIONS = [
        'asthma_risk', 'heatstroke_risk', 'cardiovascular_risk', 'respiratory_risk'
    ]

    def __init__(
        self,
        scaler_type: str = 'standard',
        imputer_type: str = 'knn',
        feature_engineering: bool = True
    ):
        """
        Initialize data processor

        Args:
            scaler_type: Type of scaler ('standard' or 'robust')
            imputer_type: Type of imputer ('simple' or 'knn')
            feature_engineering: Whether to perform feature engineering
        """
        self.scaler_type = scaler_type
        self.imputer_type = imputer_type
        self.feature_engineering = feature_engineering

        # Initialize scalers and imputers
        if scaler_type == 'standard':
            self.scaler = StandardScaler()
        elif scaler_type == 'robust':
            self.scaler = RobustScaler()
        else:
            raise ValueError(f"Unknown scaler type: {scaler_type}")

        if imputer_type == 'simple':
            self.imputer = SimpleImputer(strategy='median')
        elif imputer_type == 'knn':
            self.imputer = KNNImputer(n_neighbors=5)
        else:
            raise ValueError(f"Unknown imputer type: {imputer_type}")

        self.is_fitted = False
        self.feature_names = None
        self.target_names = None

        logger.info(f"Initialized DataProcessor with {scaler_type} scaler and {imputer_type} imputer")

    def _engineer_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Create engineered features from raw data

        Args:
            df: Input dataframe

        Returns:
            Dataframe with engineered features
        """
        df = df.copy()

        # Environmental feature interactions
        if 'pm25' in df.columns and 'aqi' in df.columns:
            df['pm25_aqi_interaction'] = df['pm25'] * df['aqi']

        if 'temperature' in df.columns and 'humidity' in df.columns:
            # Heat index approximation
            df['heat_index'] = df['temperature'] + 0.5 * df['humidity']

        if 'temperature' in df.columns:
            df['temp_squared'] = df['temperature'] ** 2
            df['extreme_heat'] = (df['temperature'] > 35).astype(int)
            df['extreme_cold'] = (df['temperature'] < 0).astype(int)

        # Air quality composite score
        pollutants = ['pm25', 'pm10', 'ozone', 'no2', 'so2', 'co']
        available_pollutants = [p for p in pollutants if p in df.columns]
        if len(available_pollutants) > 0:
            df['air_quality_composite'] = df[available_pollutants].mean(axis=1)

        # Health risk factors
        if 'age' in df.columns:
            df['age_squared'] = df['age'] ** 2
            df['elderly'] = (df['age'] >= 65).astype(int)
            df['child'] = (df['age'] < 18).astype(int)

        if 'bmi' in df.columns:
            df['obese'] = (df['bmi'] >= 30).astype(int)
            df['underweight'] = (df['bmi'] < 18.5).astype(int)

        if 'blood_pressure_systolic' in df.columns and 'blood_pressure_diastolic' in df.columns:
            df['hypertension'] = (
                (df['blood_pressure_systolic'] >= 140) |
                (df['blood_pressure_diastolic'] >= 90)
            ).astype(int)
            df['pulse_pressure'] = df['blood_pressure_systolic'] - df['blood_pressure_diastolic']

        # Comorbidity count
        comorbidity_cols = ['has_asthma', 'has_copd', 'has_heart_disease', 'has_diabetes']
        available_comorbidities = [c for c in comorbidity_cols if c in df.columns]
        if len(available_comorbidities) > 0:
            df['comorbidity_count'] = df[available_comorbidities].sum(axis=1)

        # Lifestyle risk
        if 'smoker' in df.columns and 'exercise_frequency' in df.columns:
            df['lifestyle_risk'] = df['smoker'] - df['exercise_frequency'] / 7

        # Environmental exposure score
        if 'pm25' in df.columns and 'age' in df.columns:
            df['exposure_vulnerability'] = df['pm25'] * (df['age'] / 100)

        logger.debug(f"Engineered {len(df.columns) - len(self.feature_names) if self.feature_names else 0} new features")

        return df

    def fit(self, X: pd.DataFrame, y: Optional[pd.DataFrame] = None) -> 'DataProcessor':
        """
        Fit the data processor on training data

        Args:
            X: Feature dataframe
            y: Target dataframe (optional)

        Returns:
            Fitted processor instance
        """
        logger.info("Fitting data processor...")

        # Store original feature names
        self.feature_names = list(X.columns)

        # Feature engineering
        if self.feature_engineering:
            X = self._engineer_features(X)

        # Store final feature names
        self.fitted_feature_names = list(X.columns)

        # Fit imputer
        X_imputed = self.imputer.fit_transform(X)

        # Fit scaler
        self.scaler.fit(X_imputed)

        if y is not None:
            self.target_names = list(y.columns)

        self.is_fitted = True
        logger.info(f"Data processor fitted on {len(self.fitted_feature_names)} features")

        return self

    def transform(self, X: pd.DataFrame) -> np.ndarray:
        """
        Transform data using fitted processor

        Args:
            X: Feature dataframe

        Returns:
            Transformed numpy array
        """
        if not self.is_fitted:
            raise ValueError("DataProcessor must be fitted before transform")

        # Feature engineering
        if self.feature_engineering:
            X = self._engineer_features(X)

        # Ensure all features are present
        missing_features = set(self.fitted_feature_names) - set(X.columns)
        if missing_features:
            logger.warning(f"Missing features: {missing_features}. Filling with zeros.")
            for feature in missing_features:
                X[feature] = 0

        # Ensure correct order
        X = X[self.fitted_feature_names]

        # Impute missing values
        X_imputed = self.imputer.transform(X)

        # Scale features
        X_scaled = self.scaler.transform(X_imputed)

        return X_scaled

    def fit_transform(self, X: pd.DataFrame, y: Optional[pd.DataFrame] = None) -> np.ndarray:
        """
        Fit processor and transform data

        Args:
            X: Feature dataframe
            y: Target dataframe (optional)

        Returns:
            Transformed numpy array
        """
        return self.fit(X, y).transform(X)

    def inverse_transform(self, X: np.ndarray) -> np.ndarray:
        """
        Inverse transform scaled data back to original scale

        Args:
            X: Scaled numpy array

        Returns:
            Original scale numpy array
        """
        if not self.is_fitted:
            raise ValueError("DataProcessor must be fitted before inverse_transform")

        return self.scaler.inverse_transform(X)

    def save(self, filepath: str):
        """Save processor to file"""
        with open(filepath, 'wb') as f:
            pickle.dump({
                'scaler': self.scaler,
                'imputer': self.imputer,
                'scaler_type': self.scaler_type,
                'imputer_type': self.imputer_type,
                'feature_engineering': self.feature_engineering,
                'is_fitted': self.is_fitted,
                'feature_names': self.feature_names,
                'fitted_feature_names': self.fitted_feature_names,
                'target_names': self.target_names,
            }, f)
        logger.info(f"Data processor saved to {filepath}")

    @classmethod
    def load(cls, filepath: str) -> 'DataProcessor':
        """
        Load processor from file

        Args:
            filepath: Path to processor file

        Returns:
            Loaded processor instance
        """
        with open(filepath, 'rb') as f:
            data = pickle.load(f)

        processor = cls(
            scaler_type=data['scaler_type'],
            imputer_type=data['imputer_type'],
            feature_engineering=data['feature_engineering']
        )

        processor.scaler = data['scaler']
        processor.imputer = data['imputer']
        processor.is_fitted = data['is_fitted']
        processor.feature_names = data['feature_names']
        processor.fitted_feature_names = data['fitted_feature_names']
        processor.target_names = data['target_names']

        logger.info(f"Data processor loaded from {filepath}")
        return processor

    def get_feature_names(self) -> List[str]:
        """Get list of feature names after transformation"""
        if not self.is_fitted:
            raise ValueError("DataProcessor must be fitted first")
        return self.fitted_feature_names

    def prepare_input(self, data: Dict[str, Any]) -> pd.DataFrame:
        """
        Prepare input dictionary for prediction

        Args:
            data: Dictionary with feature values

        Returns:
            Prepared dataframe
        """
        # Convert to dataframe
        df = pd.DataFrame([data])

        # Ensure all expected features exist
        for feature in self.feature_names:
            if feature not in df.columns:
                df[feature] = np.nan

        return df[self.feature_names]
