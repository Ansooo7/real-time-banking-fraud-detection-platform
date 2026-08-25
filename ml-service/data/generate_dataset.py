#!/usr/bin/env python3
"""
Synthetic Banking Fraud Dataset Generator
Targeted for UK Banking & Financial Crime Analytics
Generates realistic customer transaction histories with realistic ~1.5% fraud distribution
and multi-vector fraud typologies (Account Takeover, Velocity Bursts, Impossible Travel, High-Risk MCCs).
"""

import argparse
import math
import os
import random
import uuid
from datetime import datetime, timedelta
import numpy as np
import pandas as pd

UK_CITIES = [
    {"city": "London", "lat": 51.5074, "lon": -0.1278, "weight": 0.45},
    {"city": "Manchester", "lat": 53.4808, "lon": -2.2426, "weight": 0.12},
    {"city": "Birmingham", "lat": 52.4862, "lon": -1.8904, "weight": 0.10},
    {"city": "Edinburgh", "lat": 55.9533, "lon": -3.1883, "weight": 0.08},
    {"city": "Leeds", "lat": 53.8008, "lon": -1.5491, "weight": 0.07},
    {"city": "Bristol", "lat": 51.4545, "lon": -2.5879, "weight": 0.06},
    {"city": "Cardiff", "lat": 51.4816, "lon": -3.1791, "weight": 0.04},
    {"city": "Belfast", "lat": 54.5973, "lon": -5.9301, "weight": 0.04},
    {"city": "Newcastle", "lat": 54.9783, "lon": -1.6178, "weight": 0.04},
]

OVERSEAS_FRAUD_LOCATIONS = [
    {"city": "Lagos", "lat": 6.5244, "lon": 3.3792},
    {"city": "Bucharest", "lat": 44.4268, "lon": 26.1025},
    {"city": "Moscow", "lat": 55.7558, "lon": 37.6173},
    {"city": "New York", "lat": 40.7128, "lon": -74.0060},
    {"city": "Tokyo", "lat": 35.6762, "lon": 139.6503},
]

MERCHANTS = [
    {"name": "Tesco Stores UK", "mcc": "5411", "category": "Groceries", "risk_base": 5, "weight": 0.25},
    {"name": "Sainsbury's Supermarkets", "mcc": "5411", "category": "Groceries", "risk_base": 5, "weight": 0.15},
    {"name": "Amazon UK Retail", "mcc": "5311", "category": "Online Marketplace", "risk_base": 10, "weight": 0.20},
    {"name": "Transport for London (TfL)", "mcc": "4111", "category": "Transit", "risk_base": 2, "weight": 0.10},
    {"name": "Costa Coffee", "mcc": "5812", "category": "Restaurants/Cafe", "risk_base": 4, "weight": 0.08},
    {"name": "Shell Petrol UK", "mcc": "5541", "category": "Fuel", "risk_base": 8, "weight": 0.06},
    {"name": "Currys PC World", "mcc": "5732", "category": "Electronics", "risk_base": 25, "weight": 0.05},
    {"name": "Bet365 Online", "mcc": "7995", "category": "Gambling/Betting", "risk_base": 65, "weight": 0.03},
    {"name": "Binance UK Crypto Exchange", "mcc": "6051", "category": "Crypto/Quasi-Cash", "risk_base": 75, "weight": 0.02},
    {"name": "Western Union Wire", "mcc": "4829", "category": "Wire Transfer", "risk_base": 80, "weight": 0.01},
]

CHANNELS = ["MOBILE_APP", "ONLINE_BANKING", "POS", "ATM"]
CHANNEL_WEIGHTS = [0.55, 0.25, 0.15, 0.05]

DEVICE_TYPES = ["MOBILE_IOS", "MOBILE_ANDROID", "WEB_BROWSER_CHROME", "WEB_BROWSER_SAFARI"]


def haversine_distance_km(lat1, lon1, lat2, lon2):
    """Calculate geographical distance between two coordinate pairs in kilometers."""
    r = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2.0) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2.0) ** 2
    c = 2.0 * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))
    return r * c


def generate_synthetic_data(num_samples: int = 50000, seed: int = 42) -> pd.DataFrame:
    """Generate realistic UK banking transactions with synthetic fraud vectors."""
    random.seed(seed)
    np.random.seed(seed)

    print(f"[*] Initializing synthetic data generation for {num_samples} records...")

    # 1. Create a pool of customers
    num_customers = max(500, num_samples // 40)
    customers = []
    city_names = [c["city"] for c in UK_CITIES]
    city_weights = np.array([c["weight"] for c in UK_CITIES], dtype=np.float64)
    city_weights = city_weights / np.sum(city_weights)

    for i in range(num_customers):
        chosen_city = np.random.choice(UK_CITIES, p=city_weights)
        c_id = str(uuid.uuid4())
        primary_device = f"dev_fp_{i}_{random.randint(1000, 9999)}"
        avg_amount = float(np.random.lognormal(mean=3.2, sigma=0.6))  # Typical median ~£25-£45
        avg_amount = round(max(5.0, min(avg_amount, 500.0)), 2)

        customers.append({
            "customer_id": c_id,
            "home_city": chosen_city["city"],
            "home_lat": chosen_city["lat"] + np.random.normal(0, 0.02),
            "home_lon": chosen_city["lon"] + np.random.normal(0, 0.02),
            "primary_device": primary_device,
            "avg_amount_30d": avg_amount,
            "risk_tier": np.random.choice(["LOW", "MEDIUM", "HIGH"], p=[0.85, 0.12, 0.03]),
            "last_tx_time": datetime(2026, 7, 1, 0, 0, 0) + timedelta(minutes=random.randint(0, 5000)),
            "last_lat": chosen_city["lat"],
            "last_lon": chosen_city["lon"],
        })

    # 2. Build Transaction timeline
    transactions = []
    start_time = datetime(2026, 7, 1, 6, 0, 0)
    current_time = start_time

    merchant_names = [m["name"] for m in MERCHANTS]
    merchant_weights = np.array([m["weight"] for m in MERCHANTS], dtype=np.float64)
    merchant_weights = merchant_weights / np.sum(merchant_weights)
    merchant_map = {m["name"]: m for m in MERCHANTS}

    for i in range(num_samples):
        cust = random.choice(customers)
        current_time += timedelta(seconds=random.randint(15, 300))

        # Determine if this sample will be a synthetic fraud case (~1.5% fraud rate)
        is_fraud = 1 if (random.random() < 0.015) else 0

        channel = np.random.choice(CHANNELS, p=CHANNEL_WEIGHTS)
        device_type = random.choice(DEVICE_TYPES)

        if not is_fraud:
            # NORMAL LEGITIMATE TRANSACTION BEHAVIOUR
            m_chosen = np.random.choice(merchant_names, p=merchant_weights)
            m_info = merchant_map[m_chosen]

            # Amount fluctuates around customer's baseline average
            amount = float(np.random.normal(loc=cust["avg_amount_30d"], scale=cust["avg_amount_30d"] * 0.35))
            amount = round(max(2.50, amount), 2)

            # Location close to home city
            lat = cust["home_lat"] + np.random.normal(0, 0.015)
            lon = cust["home_lon"] + np.random.normal(0, 0.015)
            device_fingerprint = cust["primary_device"]
            is_new_device = 0
            is_night_time = 1 if (current_time.hour >= 1 and current_time.hour <= 5) else 0

            # Velocity in past 24h for normal user: 1 to 4 tx/day
            tx_count_24h = random.randint(1, 4)
            tx_count_1h = 1 if random.random() < 0.8 else 2

        else:
            # FRAUDULENT TRANSACTION INJECTION (Multi-typology)
            fraud_type = random.choice([
                "AMOUNT_SPIKE", 
                "IMPOSSIBLE_TRAVEL", 
                "VELOCITY_BURST", 
                "HIGH_RISK_MCC_NEW_DEVICE", 
                "NIGHT_ACCOUNT_TAKEOVER"
            ])

            if fraud_type == "AMOUNT_SPIKE":
                # 6x - 20x the typical customer average
                amount = round(cust["avg_amount_30d"] * random.uniform(6.0, 22.0) + random.uniform(500, 3500), 2)
                m_chosen = random.choice(["Currys PC World", "Amazon UK Retail", "Western Union Wire"])
                m_info = merchant_map[m_chosen]
                lat = cust["home_lat"] + np.random.normal(0, 0.05)
                lon = cust["home_lon"] + np.random.normal(0, 0.05)
                device_fingerprint = cust["primary_device"] if random.random() < 0.5 else f"untrusted_dev_{uuid.uuid4().hex[:8]}"
                is_new_device = 1 if device_fingerprint != cust["primary_device"] else 0
                tx_count_24h = random.randint(2, 6)
                tx_count_1h = 1

            elif fraud_type == "IMPOSSIBLE_TRAVEL":
                # Foreign IP / location suddenly appearing thousands of kilometers away in minutes
                overseas = random.choice(OVERSEAS_FRAUD_LOCATIONS)
                amount = round(cust["avg_amount_30d"] * random.uniform(2.5, 8.0) + 120.0, 2)
                m_chosen = random.choice(["Amazon UK Retail", "Binance UK Crypto Exchange", "Currys PC World"])
                m_info = merchant_map[m_chosen]
                lat = overseas["lat"] + np.random.normal(0, 0.05)
                lon = overseas["lon"] + np.random.normal(0, 0.05)
                device_fingerprint = f"foreign_dev_{uuid.uuid4().hex[:8]}"
                is_new_device = 1
                tx_count_24h = random.randint(3, 8)
                tx_count_1h = random.randint(1, 3)

            elif fraud_type == "VELOCITY_BURST":
                # 5+ transactions in 1 hour
                amount = round(random.uniform(80.0, 450.0), 2)
                m_chosen = random.choice(["Bet365 Online", "Binance UK Crypto Exchange", "Tesco Stores UK"])
                m_info = merchant_map[m_chosen]
                lat = cust["home_lat"]
                lon = cust["home_lon"]
                device_fingerprint = cust["primary_device"]
                is_new_device = 0
                tx_count_24h = random.randint(8, 18)
                tx_count_1h = random.randint(4, 9)

            elif fraud_type == "HIGH_RISK_MCC_NEW_DEVICE":
                amount = round(random.uniform(400.0, 2800.0), 2)
                m_chosen = random.choice(["Binance UK Crypto Exchange", "Bet365 Online", "Western Union Wire"])
                m_info = merchant_map[m_chosen]
                lat = cust["home_lat"] + np.random.normal(0, 0.1)
                lon = cust["home_lon"] + np.random.normal(0, 0.1)
                device_fingerprint = f"hacker_dev_{uuid.uuid4().hex[:8]}"
                is_new_device = 1
                tx_count_24h = random.randint(3, 7)
                tx_count_1h = random.randint(2, 4)

            else:  # NIGHT_ACCOUNT_TAKEOVER
                amount = round(cust["avg_amount_30d"] * random.uniform(4.0, 15.0) + 350.0, 2)
                m_chosen = random.choice(["Currys PC World", "Western Union Wire", "Binance UK Crypto Exchange"])
                m_info = merchant_map[m_chosen]
                lat = cust["home_lat"] + np.random.normal(0, 0.2)
                lon = cust["home_lon"] + np.random.normal(0, 0.2)
                device_fingerprint = f"midnight_dev_{uuid.uuid4().hex[:8]}"
                is_new_device = 1
                current_time = current_time.replace(hour=random.choice([1, 2, 3, 4]))
                tx_count_24h = random.randint(4, 9)
                tx_count_1h = random.randint(2, 5)

            is_night_time = 1 if (current_time.hour >= 1 and current_time.hour <= 5) else 0

        # Compute derived behavioral engineering features
        geo_distance_km = round(haversine_distance_km(cust["last_lat"], cust["last_lon"], lat, lon), 2)
        amount_to_avg_ratio = round(amount / max(cust["avg_amount_30d"], 1.0), 3)

        # Update customer state for temporal tracking
        cust["last_lat"] = lat
        cust["last_lon"] = lon
        cust["last_tx_time"] = current_time

        transactions.append({
            "transaction_id": str(uuid.uuid4()),
            "customer_id": cust["customer_id"],
            "timestamp": current_time.isoformat(),
            "hour_of_day": current_time.hour,
            "day_of_week": current_time.weekday(),
            "amount": amount,
            "avg_amount_30d": cust["avg_amount_30d"],
            "amount_to_avg_ratio": amount_to_avg_ratio,
            "mcc": m_info["mcc"],
            "merchant_name": m_info["name"],
            "merchant_risk_base": m_info["risk_base"],
            "channel": channel,
            "device_type": device_type,
            "device_fingerprint": device_fingerprint,
            "is_new_device": is_new_device,
            "is_night_time": is_night_time,
            "tx_count_1h": tx_count_1h,
            "tx_count_24h": tx_count_24h,
            "geo_distance_km": geo_distance_km,
            "latitude": lat,
            "longitude": lon,
            "is_fraud": is_fraud,
        })

    df = pd.DataFrame(transactions)
    fraud_count = df["is_fraud"].sum()
    fraud_ratio = (fraud_count / len(df)) * 100

    print(f"[OK] Generated {len(df)} transactions.")
    print(f"[OK] Fraud cases: {fraud_count} ({fraud_ratio:.2f}% of dataset - realistic banking distribution)")
    return df


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate synthetic banking fraud dataset.")
    parser.add_argument("--samples", type=int, default=50000, help="Number of transaction samples to generate")
    parser.add_argument("--output", type=str, default="data/synthetic_transactions.csv", help="Output CSV path")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility")
    args = parser.parse_args()

    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    dataset = generate_synthetic_data(num_samples=args.samples, seed=args.seed)
    dataset.to_csv(args.output, index=False)
    print(f"[OK] Saved synthetic dataset to {args.output}")
