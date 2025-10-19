"""
Configuration module for AI Model Service
Loads environment variables and provides configuration settings
"""

import os
import multiprocessing
from pathlib import Path
from typing import Optional
from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""

    # Application Settings
    APP_NAME: str = "ClimateHealthMapper AI Model Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = Field(default=False, env="DEBUG")
    HOST: str = Field(default="0.0.0.0", env="HOST")
    PORT: int = Field(default=8001, env="PORT")

    # Model Settings
    MODEL_PATH: str = Field(default="model.pt", env="MODEL_PATH")
    MODEL_INPUT_SIZE: int = Field(default=50, env="MODEL_INPUT_SIZE")
    MODEL_HIDDEN_SIZE: int = Field(default=128, env="MODEL_HIDDEN_SIZE")
    MODEL_OUTPUT_SIZE: int = Field(default=4, env="MODEL_OUTPUT_SIZE")
    MODEL_DROPOUT: float = Field(default=0.3, env="MODEL_DROPOUT")
    BATCH_SIZE: int = Field(default=32, env="BATCH_SIZE")
    MAX_BATCH_SIZE: int = Field(default=1000, env="MAX_BATCH_SIZE")

    # Database Settings
    DB_HOST: str = Field(default="localhost", env="DB_HOST")
    DB_PORT: int = Field(default=5432, env="DB_PORT")
    DB_NAME: str = Field(default="climatehealthmapper", env="DB_NAME")
    DB_USER: str = Field(default="postgres", env="DB_USER")
    DB_PASSWORD: str = Field(default="postgres", env="DB_PASSWORD")

    # Redis Settings
    REDIS_HOST: str = Field(default="localhost", env="REDIS_HOST")
    REDIS_PORT: int = Field(default=6379, env="REDIS_PORT")
    REDIS_DB: int = Field(default=0, env="REDIS_DB")
    REDIS_PASSWORD: Optional[str] = Field(default=None, env="REDIS_PASSWORD")
    CACHE_TTL: int = Field(default=3600, env="CACHE_TTL")  # 1 hour

    # Training Settings
    LEARNING_RATE: float = Field(default=0.001, env="LEARNING_RATE")
    NUM_EPOCHS: int = Field(default=100, env="NUM_EPOCHS")
    EARLY_STOPPING_PATIENCE: int = Field(default=10, env="EARLY_STOPPING_PATIENCE")
    VALIDATION_SPLIT: float = Field(default=0.2, env="VALIDATION_SPLIT")
    CV_FOLDS: int = Field(default=5, env="CV_FOLDS")

    # Performance Settings
    USE_CUDA: bool = Field(default=True, env="USE_CUDA")
    NUM_WORKERS: int = Field(default=None, env="NUM_WORKERS")
    USE_MULTIPROCESSING: bool = Field(default=None, env="USE_MULTIPROCESSING")

    # Logging
    LOG_LEVEL: str = Field(default="INFO", env="LOG_LEVEL")
    LOG_FILE: Optional[str] = Field(default="ai_model.log", env="LOG_FILE")

    class Config:
        env_file = ".env"
        case_sensitive = True

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        # Auto-configure multiprocessing based on system resources
        if self.NUM_WORKERS is None:
            cpu_count = multiprocessing.cpu_count()
            self.NUM_WORKERS = max(1, cpu_count - 1) if cpu_count > 4 else 1

        if self.USE_MULTIPROCESSING is None:
            cpu_count = multiprocessing.cpu_count()
            # Enable multiprocessing if CPU cores > 4 and estimated memory > 8GB
            # Simple heuristic: check if we have enough cores
            self.USE_MULTIPROCESSING = cpu_count > 4

    @property
    def database_url(self) -> str:
        """Generate PostgreSQL database URL"""
        return f"postgresql://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"

    @property
    def redis_url(self) -> str:
        """Generate Redis URL"""
        if self.REDIS_PASSWORD:
            return f"redis://:{self.REDIS_PASSWORD}@{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"
        return f"redis://{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"

    @property
    def model_file_path(self) -> Path:
        """Get absolute path to model file"""
        return Path(__file__).parent / self.MODEL_PATH


# Global settings instance
settings = Settings()
