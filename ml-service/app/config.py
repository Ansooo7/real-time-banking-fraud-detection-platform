import os
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Banking Fraud Detection & ML Risk Inference Service"
    VERSION: str = "1.0.0"
    ENVIRONMENT: str = os.getenv("ENVIRONMENT", "production")
    PORT: int = int(os.getenv("PORT", 8000))
    MODEL_PATH: str = os.getenv("MODEL_PATH", "models/pipeline.joblib")
    METRICS_PATH: str = os.getenv("METRICS_PATH", "models/metrics.json")
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
