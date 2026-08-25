import pytest
import pandas as pd
from data.generate_dataset import generate_synthetic_data, haversine_distance_km


def test_haversine_distance():
    # London (51.5074, -0.1278) to Manchester (53.4808, -2.2426) is approx 260 km
    dist = haversine_distance_km(51.5074, -0.1278, 53.4808, -2.2426)
    assert 240 < dist < 280


def test_synthetic_data_generation_shape_and_columns():
    df = generate_synthetic_data(num_samples=1000, seed=123)
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 1000
    
    expected_cols = [
        "transaction_id", "customer_id", "timestamp", "amount", "avg_amount_30d",
        "mcc", "channel", "device_fingerprint", "is_new_device", "is_night_time",
        "tx_count_1h", "tx_count_24h", "geo_distance_km", "is_fraud"
    ]
    for col in expected_cols:
        assert col in df.columns, f"Missing required column: {col}"


def test_fraud_imbalance_ratio():
    df = generate_synthetic_data(num_samples=2000, seed=456)
    fraud_count = df["is_fraud"].sum()
    fraud_pct = (fraud_count / len(df)) * 100
    # Expected realistic fraud rate between 0.5% and 3.0%
    assert 0.5 <= fraud_pct <= 3.0
