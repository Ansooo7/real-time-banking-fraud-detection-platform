import json
import logging
import os
import time
from typing import Dict, Any, Tuple
import joblib
import numpy as np
import pandas as pd

from app.config import settings
from app.schemas import TransactionFeatures, PredictionResult

logger = logging.getLogger(__name__)


class FraudMLService:
    def __init__(self):
        self.model_pipeline = None
        self.metrics = {}
        self.model_name = "Unknown"
        self.load_model()

    def load_model(self):
        """Load trained pipeline and metrics metadata."""
        if os.path.exists(settings.MODEL_PATH):
            try:
                self.model_pipeline = joblib.load(settings.MODEL_PATH)
                logger.info(f"Loaded ML Model pipeline from {settings.MODEL_PATH}")
            except Exception as e:
                logger.error(f"Error loading model from {settings.MODEL_PATH}: {e}")
                self.model_pipeline = None
        else:
            logger.warning(f"Model file {settings.MODEL_PATH} does not exist yet.")

        if os.path.exists(settings.METRICS_PATH):
            try:
                with open(settings.METRICS_PATH, "r") as f:
                    self.metrics = json.load(f)
                    self.model_name = self.metrics.get("best_model", "XGBoost/RandomForest")
                logger.info(f"Loaded ML metrics from {settings.METRICS_PATH}")
            except Exception as e:
                logger.error(f"Error loading metrics from {settings.METRICS_PATH}: {e}")
        else:
            self.model_name = "Fallback_Heuristic_Model"

    def predict(self, req: TransactionFeatures) -> PredictionResult:
        """Run ML inference for incoming transaction feature vector."""
        start_time = time.perf_counter()

        # Compute derived features
        hour = req.hour_of_day if req.hour_of_day is not None else 12
        hour_sin = float(np.sin(2 * np.pi * hour / 24.0))
        hour_cos = float(np.cos(2 * np.pi * hour / 24.0))
        
        ratio = req.amount_to_avg_ratio
        if ratio is None:
            ratio = float(req.amount / max(req.avg_amount_30d, 1.0))
            
        is_night = req.is_night_time
        if is_night is None:
            is_night = 1 if (hour >= 1 and hour <= 5) else 0

        # Create 1-row DataFrame matching the training schema
        input_data = {
            "amount": [req.amount],
            "avg_amount_30d": [req.avg_amount_30d],
            "amount_to_avg_ratio": [ratio],
            "merchant_risk_base": [req.merchant_risk_base],
            "geo_distance_km": [req.geo_distance_km],
            "tx_count_1h": [req.tx_count_1h],
            "tx_count_24h": [req.tx_count_24h],
            "hour_sin": [hour_sin],
            "hour_cos": [hour_cos],
            "is_new_device": [req.is_new_device],
            "is_night_time": [is_night],
            "channel": [req.channel]
        }
        
        df_input = pd.DataFrame(input_data)

        if self.model_pipeline is not None:
            try:
                proba = float(self.model_pipeline.predict_proba(df_input)[0, 1])
            except Exception as e:
                logger.error(f"Inference error in model pipeline: {e}. Falling back to baseline heuristic.")
                proba = self._heuristic_fallback(req, ratio, is_night)
        else:
            proba = self._heuristic_fallback(req, ratio, is_night)

        # Scale probability to integer risk score 0 - 100
        risk_score = int(np.clip(round(proba * 100), 0, 100))

        # Risk factor explainability breakdown
        risk_factors = {
            "amount_ratio_factor": round(min(ratio / 10.0, 1.0), 3),
            "velocity_1h_factor": round(min(req.tx_count_1h / 5.0, 1.0), 3),
            "geo_distance_factor": round(min(req.geo_distance_km / 1000.0, 1.0), 3),
            "merchant_risk_factor": round(req.merchant_risk_base / 100.0, 3),
            "new_device_flag": bool(req.is_new_device == 1),
            "night_hour_flag": bool(is_night == 1)
        }

        inference_time_ms = round((time.perf_counter() - start_time) * 1000.0, 2)

        return PredictionResult(
            transaction_id=req.transaction_id,
            fraud_probability=round(proba, 4),
            risk_score=risk_score,
            model_name=self.model_name,
            model_version=settings.VERSION,
            inference_time_ms=inference_time_ms,
            risk_factors=risk_factors
        )

    def _heuristic_fallback(self, req: TransactionFeatures, ratio: float, is_night: int) -> float:
        """Robust statistical fallback if model file is not mounted."""
        score = 0.05
        if ratio > 5.0:
            score += 0.35
        elif ratio > 2.5:
            score += 0.15
            
        if req.tx_count_1h >= 4:
            score += 0.30
        elif req.tx_count_1h >= 2:
            score += 0.10
            
        if req.geo_distance_km > 500:
            score += 0.35
            
        if req.merchant_risk_base >= 60:
            score += 0.20
            
        if req.is_new_device == 1 and is_night == 1:
            score += 0.25
            
        return float(min(score, 0.99))


ml_service = FraudMLService()
