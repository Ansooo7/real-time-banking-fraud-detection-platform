import os
import pytest
from models.train import train_and_evaluate


def test_train_and_evaluate_pipeline(tmp_path):
    output_dir = str(tmp_path / "models")
    data_path = str(tmp_path / "data" / "test_transactions.csv")
    
    pipeline, metrics = train_and_evaluate(data_path=data_path, output_dir=output_dir)
    
    assert pipeline is not None
    assert os.path.exists(os.path.join(output_dir, "pipeline.joblib"))
    assert os.path.exists(os.path.join(output_dir, "metrics.json"))
    
    # Check that PR-AUC and ROC-AUC were computed
    assert "comparison_results" in metrics
    for model_name, model_metrics in metrics["comparison_results"].items():
        assert "pr_auc" in model_metrics
        assert "roc_auc" in model_metrics
        assert "f1_score" in model_metrics
        assert model_metrics["pr_auc"] > 0.0
        assert model_metrics["roc_auc"] > 0.5
