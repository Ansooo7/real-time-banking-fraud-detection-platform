#!/usr/bin/env python3
"""
Machine Learning Training Pipeline for Real-Time Banking Fraud Detection.
Addresses high class imbalance (1.5% fraud rate) and compares:
- Logistic Regression (Baseline)
- Random Forest Classifier (Balanced Subsample)
- XGBoost / Gradient Boosting Classifier (Cost-Sensitive Weighted)

Outputs model evaluation metrics (PR-AUC, ROC-AUC, F1, Confusion Matrix)
and serializes the best performing model pipeline.
"""

import argparse
import json
import os
import sys
import numpy as np
import pandas as pd
import joblib

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    roc_auc_score,
    average_precision_score,
    f1_score,
    precision_score,
    recall_score
)

# Attempt XGBoost import; if not present, fallback cleanly to GradientBoostingClassifier
try:
    from xgboost import XGBClassifier
    HAS_XGB = True
except ImportError:
    HAS_XGB = False


def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """Derive temporal cyclical features and transformations."""
    df_feat = df.copy()
    
    # Cyclical hour encoding (24 hour periodicity)
    df_feat["hour_sin"] = np.sin(2 * np.pi * df_feat["hour_of_day"] / 24.0)
    df_feat["hour_cos"] = np.cos(2 * np.pi * df_feat["hour_of_day"] / 24.0)
    
    # Ensure amount ratio is clean
    if "amount_to_avg_ratio" not in df_feat.columns:
        df_feat["amount_to_avg_ratio"] = df_feat["amount"] / np.maximum(df_feat["avg_amount_30d"], 1.0)
        
    return df_feat


def train_and_evaluate(data_path: str, output_dir: str):
    """Run end-to-end model training, imbalance handling, comparison and export."""
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"[*] Loading dataset from {data_path}...")
    if not os.path.exists(data_path):
        print(f"[!] Data file {data_path} not found. Triggering synthetic generator...")
        sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
        from data.generate_dataset import generate_synthetic_data
        df = generate_synthetic_data(num_samples=25000)
        os.makedirs(os.path.dirname(data_path), exist_ok=True)
        df.to_csv(data_path, index=False)
    else:
        df = pd.read_csv(data_path)
        
    df = engineer_features(df)
    
    # Feature columns definition
    numeric_features = [
        "amount",
        "avg_amount_30d",
        "amount_to_avg_ratio",
        "merchant_risk_base",
        "geo_distance_km",
        "tx_count_1h",
        "tx_count_24h",
        "hour_sin",
        "hour_cos",
        "is_new_device",
        "is_night_time"
    ]
    
    categorical_features = ["channel"]
    
    feature_cols = numeric_features + categorical_features
    target_col = "is_fraud"
    
    X = df[feature_cols]
    y = df[target_col]
    
    fraud_count = int(y.sum())
    total_count = len(y)
    fraud_rate = (fraud_count / total_count) * 100
    imbalance_ratio = (total_count - fraud_count) / max(fraud_count, 1)
    
    print(f"[*] Dataset summary: {total_count} total records, {fraud_count} fraud ({fraud_rate:.2f}%)")
    print(f"[*] Class Imbalance Ratio: {imbalance_ratio:.1f}:1 (Legitimate : Fraud)")
    print("[*] Note: In financial fraud, accuracy is a misleading metric (a dummy model predicting all 0 has >98.5% accuracy).")
    print("[*] We prioritize Precision-Recall AUC (PR-AUC), Fraud Recall, and F1-Score to minimize financial loss.")

    # Stratified train-test split (80% train, 20% test)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )
    
    # Preprocessor definition
    preprocessor = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), numeric_features),
            ("cat", OneHotEncoder(handle_unknown="ignore"), categorical_features)
        ]
    )
    
    # Define models to compare
    models_to_test = {
        "Logistic_Regression": LogisticRegression(
            class_weight="balanced", 
            max_iter=1000, 
            random_state=42
        ),
        "Random_Forest": RandomForestClassifier(
            n_estimators=100, 
            max_depth=12, 
            class_weight="balanced_subsample", 
            random_state=42, 
            n_jobs=-1
        )
    }
    
    if HAS_XGB:
        models_to_test["XGBoost"] = XGBClassifier(
            n_estimators=120,
            max_depth=5,
            learning_rate=0.08,
            scale_pos_weight=imbalance_ratio,
            random_state=42,
            eval_metric="aucpr",
            n_jobs=-1
        )
    else:
        models_to_test["Gradient_Boosting"] = GradientBoostingClassifier(
            n_estimators=120,
            max_depth=5,
            learning_rate=0.08,
            random_state=42
        )
        
    results = {}
    best_model_name = None
    best_pr_auc = -1.0
    best_pipeline = None

    print("\n" + "="*80)
    print("MODEL BENCHMARKING ON IMBALANCED FRAUD DATA")
    print("="*80)

    for name, clf in models_to_test.items():
        print(f"\n[+] Training {name}...")
        pipeline = Pipeline(steps=[
            ("preprocessor", preprocessor),
            ("classifier", clf)
        ])
        
        pipeline.fit(X_train, y_train)
        
        # Predictions and probabilities
        y_pred = pipeline.predict(X_test)
        y_proba = pipeline.predict_proba(X_test)[:, 1]
        
        # Calculate key metrics
        roc_auc = roc_auc_score(y_test, y_proba)
        pr_auc = average_precision_score(y_test, y_proba)
        f1 = f1_score(y_test, y_pred)
        precision = precision_score(y_test, y_pred, zero_division=0)
        recall = recall_score(y_test, y_pred, zero_division=0)
        cm = confusion_matrix(y_test, y_pred).tolist()
        
        print(f"    - ROC-AUC Score:      {roc_auc:.4f}")
        print(f"    - PR-AUC Score:       {pr_auc:.4f} (Key Financial Metric)")
        print(f"    - Fraud Recall:       {recall:.4f} (% fraud captured)")
        print(f"    - Fraud Precision:    {precision:.4f} (% alerts accurate)")
        print(f"    - F1-Score:           {f1:.4f}")
        print(f"    - Confusion Matrix:   TN={cm[0][0]}, FP={cm[0][1]}, FN={cm[1][0]}, TP={cm[1][1]}")

        results[name] = {
            "model_name": name,
            "roc_auc": round(float(roc_auc), 4),
            "pr_auc": round(float(pr_auc), 4),
            "f1_score": round(float(f1), 4),
            "precision": round(float(precision), 4),
            "recall": round(float(recall), 4),
            "confusion_matrix": {
                "true_negatives": cm[0][0],
                "false_positives": cm[0][1],
                "false_negatives": cm[1][0],
                "true_positives": cm[1][1]
            }
        }
        
        # Select best model based on PR-AUC
        if pr_auc > best_pr_auc:
            best_pr_auc = pr_auc
            best_model_name = name
            best_pipeline = pipeline

    print("\n" + "="*80)
    print(f"[OK] WINNING MODEL: {best_model_name} with PR-AUC = {best_pr_auc:.4f}")
    print("="*80)

    # Save best model pipeline
    model_output_path = os.path.join(output_dir, "pipeline.joblib")
    joblib.dump(best_pipeline, model_output_path)
    print(f"[OK] Best pipeline serialized to {model_output_path}")

    # Extract feature names & importances if tree-based
    feature_importance_map = {}
    try:
        clf_step = best_pipeline.named_steps["classifier"]
        if hasattr(clf_step, "feature_importances_"):
            # Get transformed feature names
            cat_encoder = best_pipeline.named_steps["preprocessor"].named_transformers_["cat"]
            encoded_cat_names = list(cat_encoder.get_feature_names_out(categorical_features))
            all_feature_names = numeric_features + encoded_cat_names
            importances = clf_step.feature_importances_
            feature_importance_map = {
                feat: round(float(imp), 4) for feat, imp in zip(all_feature_names, importances)
            }
            # Sort by importance
            feature_importance_map = dict(sorted(feature_importance_map.items(), key=lambda x: x[1], reverse=True))
    except Exception as e:
        print(f"[!] Could not extract feature importances: {e}")

    # Save summary metrics JSON
    metrics_summary = {
        "best_model": best_model_name,
        "features": feature_cols,
        "numeric_features": numeric_features,
        "categorical_features": categorical_features,
        "comparison_results": results,
        "feature_importances": feature_importance_map,
        "dataset_metadata": {
            "total_samples": total_count,
            "fraud_samples": fraud_count,
            "fraud_percentage": round(fraud_rate, 2),
            "imbalance_ratio": round(imbalance_ratio, 2)
        }
    }
    
    metrics_output_path = os.path.join(output_dir, "metrics.json")
    with open(metrics_output_path, "w") as f:
        json.dump(metrics_summary, f, indent=2)
    print(f"[OK] Training metrics saved to {metrics_output_path}")

    return best_pipeline, metrics_summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train and evaluate fraud detection ML models.")
    parser.add_argument("--data", type=str, default="data/synthetic_transactions.csv", help="Path to input dataset CSV")
    parser.add_argument("--output", type=str, default="models/", help="Output directory for saved models and metrics")
    args = parser.parse_args()
    
    train_and_evaluate(args.data, args.output)
