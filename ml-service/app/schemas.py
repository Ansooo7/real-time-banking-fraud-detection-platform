from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field


class TransactionFeatures(BaseModel):
    transaction_id: Optional[str] = Field(None, description="UUID of the transaction")
    customer_id: Optional[str] = Field(None, description="UUID of the customer")
    amount: float = Field(..., gt=0, description="Transaction amount in GBP")
    avg_amount_30d: float = Field(..., ge=0, description="Customer 30-day historical average amount")
    amount_to_avg_ratio: Optional[float] = Field(None, description="Calculated ratio of amount to historical average")
    merchant_risk_base: int = Field(10, ge=0, le=100, description="Base risk score of the merchant category")
    geo_distance_km: float = Field(0.0, ge=0, description="Distance in KM from previous transaction")
    tx_count_1h: int = Field(1, ge=0, description="Velocity: transactions performed in the last 1 hour")
    tx_count_24h: int = Field(1, ge=0, description="Velocity: transactions performed in the last 24 hours")
    hour_of_day: Optional[int] = Field(12, ge=0, le=23, description="Hour of the day (0-23)")
    is_new_device: int = Field(0, ge=0, le=1, description="1 if untrusted/new device, else 0")
    is_night_time: Optional[int] = Field(None, ge=0, le=1, description="1 if transaction between 01:00 and 05:00")
    channel: str = Field("MOBILE_APP", description="Transaction channel: MOBILE_APP, ONLINE_BANKING, POS, ATM")


class PredictionResult(BaseModel):
    transaction_id: Optional[str]
    fraud_probability: float = Field(..., ge=0.0, le=1.0, description="Estimated fraud probability [0.0 - 1.0]")
    risk_score: int = Field(..., ge=0, le=100, description="Scaled risk score [0 - 100]")
    model_name: str
    model_version: str
    inference_time_ms: float
    risk_factors: Dict[str, Any]


class ModelMetricsResponse(BaseModel):
    best_model: str
    dataset_metadata: Dict[str, Any]
    comparison_results: Dict[str, Any]
    feature_importances: Dict[str, float]


class HealthResponse(BaseModel):
    status: str
    version: str
    model_loaded: bool
    model_name: Optional[str]
