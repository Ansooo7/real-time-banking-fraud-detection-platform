import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import (
    TransactionFeatures,
    PredictionResult,
    ModelMetricsResponse,
    HealthResponse
)
from app.service import ml_service

logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s [%(levelname)s] [%(name)s] %(message)s"
)
logger = logging.getLogger("fraud_ml_service")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing Real-Time Banking Fraud ML Service...")
    ml_service.load_model()
    yield
    logger.info("Shutting down ML Service.")


app = FastAPI(
    title="Real-Time Banking Fraud ML Risk Inference Engine",
    description=(
        "Microservice providing real-time Machine Learning fraud risk evaluation "
        "and explainability factors for high-throughput UK banking transactions."
    ),
    version=settings.VERSION,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS middleware for direct frontend analytics access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthResponse, tags=["System Health"])
async def health_check():
    """Liveness and readiness check for container orchestrators."""
    return HealthResponse(
        status="UP",
        version=settings.VERSION,
        model_loaded=(ml_service.model_pipeline is not None),
        model_name=ml_service.model_name
    )


@app.post(
    "/api/v1/predict",
    response_model=PredictionResult,
    status_code=status.HTTP_200_OK,
    tags=["Fraud Inference"]
)
async def predict_fraud_risk(features: TransactionFeatures):
    """
    Score a single transaction against the ML model pipeline.
    Returns fraud probability, scaled 0-100 risk score, and factor breakdown.
    """
    try:
        return ml_service.predict(features)
    except Exception as e:
        logger.error(f"Inference execution failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference execution failed: {str(e)}"
        )


@app.get(
    "/api/v1/model/info",
    response_model=ModelMetricsResponse,
    tags=["Model Governance & Metrics"]
)
async def get_model_info():
    """
    Retrieve model governance metadata, training metrics (PR-AUC, ROC-AUC, F1),
    and feature importances.
    """
    if not ml_service.metrics:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Model metrics metadata not available. Please trigger model training."
        )
    return ModelMetricsResponse(
        best_model=ml_service.metrics.get("best_model", "XGBoost"),
        dataset_metadata=ml_service.metrics.get("dataset_metadata", {}),
        comparison_results=ml_service.metrics.get("comparison_results", {}),
        feature_importances=ml_service.metrics.get("feature_importances", {})
    )
