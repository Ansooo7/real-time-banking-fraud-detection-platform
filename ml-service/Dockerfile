FROM python:3.11-slim

WORKDIR /app

# Install system dependencies (including curl for healthcheck and build tools)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code and scripts
COPY . .

# Generate dataset and train default model during build if not pre-baked
RUN python data/generate_dataset.py --samples 15000 --output data/synthetic_transactions.csv \
    && python models/train.py --data data/synthetic_transactions.csv --output models/

EXPOSE 8000

ENV PORT=8000
ENV MODEL_PATH=/app/models/pipeline.joblib
ENV METRICS_PATH=/app/models/metrics.json

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
