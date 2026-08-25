import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "model_loaded" in data


def test_predict_normal_transaction():
    payload = {
        "amount": 25.50,
        "avg_amount_30d": 30.00,
        "amount_to_avg_ratio": 0.85,
        "merchant_risk_base": 5,
        "geo_distance_km": 2.1,
        "tx_count_1h": 1,
        "tx_count_24h": 2,
        "hour_of_day": 14,
        "is_new_device": 0,
        "channel": "MOBILE_APP"
    }
    response = client.post("/api/v1/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["fraud_probability"] <= 1.0
    assert 0 <= data["risk_score"] <= 100
    assert data["risk_score"] < 40  # Normal transaction should have low risk score
    assert "risk_factors" in data


def test_predict_high_risk_transaction():
    payload = {
        "amount": 4800.00,
        "avg_amount_30d": 45.00,
        "amount_to_avg_ratio": 106.6,
        "merchant_risk_base": 75,
        "geo_distance_km": 4500.0,
        "tx_count_1h": 5,
        "tx_count_24h": 9,
        "hour_of_day": 3,
        "is_new_device": 1,
        "channel": "ONLINE_BANKING"
    }
    response = client.post("/api/v1/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_score"] >= 65  # Obvious fraud attack should have high risk score
