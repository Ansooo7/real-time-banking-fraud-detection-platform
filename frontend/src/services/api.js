import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const ML_BASE_URL = import.meta.env.VITE_ML_API_BASE_URL || 'http://localhost:8000';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

// Attach JWT token to every request if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authService = {
  login: async (username, password) => {
    try {
      const res = await api.post('/api/v1/auth/login', { username, password });
      if (res.data?.data?.token) {
        localStorage.setItem('token', res.data.data.token);
        localStorage.setItem('user', JSON.stringify(res.data.data));
      }
      return res.data;
    } catch (err) {
      // Mock fallback for quick offline demo
      const mockUser = {
        token: 'mock-jwt-token-analyst-lead',
        username: username || 'analyst',
        fullName: 'Sarah Jenkins (Lead Fraud Analyst)',
        role: 'ROLE_FRAUD_ANALYST',
        email: 'sarah.analyst@ukbank.co.uk',
      };
      localStorage.setItem('token', mockUser.token);
      localStorage.setItem('user', JSON.stringify(mockUser));
      return { success: true, data: mockUser };
    }
  },
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
  getCurrentUser: () => {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
};

export const analyticsService = {
  getSummary: async () => {
    try {
      const res = await api.get('/api/v1/fraud/analytics/summary');
      return res.data.data;
    } catch (err) {
      return getMockAnalyticsSummary();
    }
  },
  getModelPerformance: async () => {
    try {
      const res = await api.get('/api/v1/fraud/analytics/model-performance');
      return res.data.data;
    } catch (err) {
      return getMockModelPerformance();
    }
  }
};

export const transactionService = {
  getTransactions: async (page = 0, size = 20, status = null) => {
    try {
      const params = { page, size };
      if (status) params.status = status;
      const res = await api.get('/api/v1/transactions', { params });
      return res.data.data;
    } catch (err) {
      return getMockTransactions(page, size, status);
    }
  },
  createTransaction: async (data) => {
    const idempotencyKey = crypto.randomUUID();
    const correlationId = 'corr-' + Math.random().toString(36).substring(2, 9);
    try {
      const res = await api.post('/api/v1/transactions', data, {
        headers: {
          'Idempotency-Key': idempotencyKey,
          'X-Correlation-Id': correlationId,
        }
      });
      return res.data;
    } catch (err) {
      // Simulate transaction scoring locally if backend is unavailable
      return simulateTransactionScore(data, idempotencyKey);
    }
  }
};

export const fraudAlertService = {
  getAlerts: async (page = 0, size = 20, status = null) => {
    try {
      const params = { page, size };
      if (status) params.status = status;
      const res = await api.get('/api/v1/fraud/alerts', { params });
      return res.data.data;
    } catch (err) {
      return getMockAlerts(page, size, status);
    }
  },
  submitDecision: async (alertId, decision, notes) => {
    try {
      const res = await api.post(`/api/v1/fraud/alerts/${alertId}/decision`, { decision, notes });
      return res.data.data;
    } catch (err) {
      return { id: alertId, status: decision, analystNotes: notes, resolvedAt: new Date().toISOString() };
    }
  }
};

export const customerRiskService = {
  getProfile: async (customerId) => {
    try {
      const res = await api.get(`/api/v1/fraud/risk-profiles/${customerId}`);
      return res.data.data;
    } catch (err) {
      return getMockCustomerProfile(customerId);
    }
  }
};

// ==========================================
// MOCK DATA GENERATORS (FinTech Realistic)
// ==========================================

function getMockAnalyticsSummary() {
  return {
    totalTransactions24h: 18450,
    approvedCount: 17920,
    reviewCount: 380,
    blockedCount: 150,
    fraudRatePercent: 1.48,
    totalVolumeGbp: 4892400.50,
    totalBlockedAmountGbp: 312500.00,
    openAlertsCount: 42,
    channelDistribution: {
      "MOBILE_APP": 10147,
      "ONLINE_BANKING": 4612,
      "POS": 2767,
      "ATM": 924
    },
    riskTierDistribution: {
      "LOW_RISK_0_30": 17920,
      "MEDIUM_RISK_31_70": 380,
      "HIGH_RISK_71_100": 150
    }
  };
}

function getMockModelPerformance() {
  return {
    best_model: "XGBoost Classifier (Optimized scale_pos_weight)",
    dataset_metadata: {
      total_samples: 50000,
      fraud_samples: 742,
      fraud_percentage: 1.48,
      imbalance_ratio: 66.4
    },
    comparison_results: {
      "Logistic_Regression": {
        "pr_auc": 0.7214,
        "roc_auc": 0.8842,
        "f1_score": 0.6912,
        "precision": 0.6450,
        "recall": 0.7440,
        "confusion_matrix": { "true_negatives": 9780, "false_positives": 72, "false_negatives": 38, "true_positives": 110 }
      },
      "Random_Forest": {
        "pr_auc": 0.8640,
        "roc_auc": 0.9580,
        "f1_score": 0.8320,
        "precision": 0.8120,
        "recall": 0.8530,
        "confusion_matrix": { "true_negatives": 9822, "false_positives": 30, "false_negatives": 22, "true_positives": 126 }
      },
      "XGBoost": {
        "pr_auc": 0.9125,
        "roc_auc": 0.9790,
        "f1_score": 0.8875,
        "precision": 0.8710,
        "recall": 0.9050,
        "confusion_matrix": { "true_negatives": 9834, "false_positives": 18, "false_negatives": 14, "true_positives": 134 }
      }
    },
    feature_importances: {
      "amount_to_avg_ratio": 0.284,
      "geo_distance_km": 0.218,
      "tx_count_1h": 0.165,
      "merchant_risk_base": 0.142,
      "is_new_device": 0.089,
      "is_night_time": 0.062,
      "amount": 0.040
    }
  };
}

function getMockTransactions(page, size, status) {
  const all = [
    { id: "tx-9901", customerName: "Oliver Twist", amount: 4850.00, currency: "GBP", channel: "ONLINE_BANKING", merchantName: "Binance UK Crypto Exchange", merchantCategory: "Crypto/Quasi-Cash", status: "BLOCKED", riskScore: 88, decisionReason: "High risk: Untrusted device, Night hour 03:15, Crypto MCC", createdAt: new Date(Date.now() - 1000 * 60 * 2).toISOString() },
    { id: "tx-9902", customerName: "Arthur Shelby", amount: 1250.00, currency: "GBP", channel: "MOBILE_APP", merchantName: "Currys PC World", merchantCategory: "Electronics", status: "REVIEW", riskScore: 58, decisionReason: "Elevated risk: Amount 4.2x 30d baseline, Geo hop 180km", createdAt: new Date(Date.now() - 1000 * 60 * 7).toISOString() },
    { id: "tx-9903", customerName: "Emma Watson", amount: 42.50, currency: "GBP", channel: "POS", merchantName: "Sainsbury's Supermarkets", merchantCategory: "Groceries", status: "APPROVED", riskScore: 8, decisionReason: "Approved: Consistent spending baseline", createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString() },
    { id: "tx-9904", customerName: "James Bond", amount: 350.00, currency: "GBP", channel: "MOBILE_APP", merchantName: "Bet365 Online", merchantCategory: "Gambling/Betting", status: "REVIEW", riskScore: 62, decisionReason: "Review: High velocity (5 tx in 1h), Gambling MCC", createdAt: new Date(Date.now() - 1000 * 60 * 18).toISOString() },
    { id: "tx-9905", customerName: "Oliver Twist", amount: 15.00, currency: "GBP", channel: "MOBILE_APP", merchantName: "Transport for London (TfL)", merchantCategory: "Transit", status: "APPROVED", riskScore: 4, decisionReason: "Approved: Trusted device & normal location", createdAt: new Date(Date.now() - 1000 * 60 * 25).toISOString() },
  ];
  return {
    content: status ? all.filter(t => t.status === status) : all,
    totalElements: all.length,
    totalPages: 1,
    pageNumber: page,
    pageSize: size,
    last: true
  };
}

function getMockAlerts(page, size, status) {
  const alerts = [
    {
      id: "fa-101",
      transactionId: "tx-9901",
      customerName: "Oliver Twist",
      customerNumber: "CUST-UK-1001",
      amount: 4850.00,
      currency: "GBP",
      merchantName: "Binance UK Crypto Exchange",
      ruleScore: 85,
      mlScore: 92,
      compositeRiskScore: 88,
      triggeredRules: '["RULE_AMOUNT_SPIKE_4X_HISTORICAL", "RULE_HIGH_RISK_MCC_CRYPTO", "RULE_NEW_UNTRUSTED_DEVICE", "RULE_UNUSUAL_NIGHT_TIME_HOURS"]',
      mlFeatureContributions: '{"amount_ratio_factor": 0.95, "geo_distance_factor": 0.88, "new_device_flag": true}',
      status: "PENDING_REVIEW",
      createdAt: new Date(Date.now() - 1000 * 60 * 2).toISOString()
    },
    {
      id: "fa-102",
      transactionId: "tx-9902",
      customerName: "Arthur Shelby",
      customerNumber: "CUST-UK-1004",
      amount: 1250.00,
      currency: "GBP",
      merchantName: "Currys PC World",
      ruleScore: 60,
      mlScore: 56,
      compositeRiskScore: 58,
      triggeredRules: '["RULE_AMOUNT_SPIKE_4X_HISTORICAL", "RULE_GEOGRAPHIC_DEVIATION"]',
      mlFeatureContributions: '{"amount_ratio_factor": 0.42, "geo_distance_factor": 0.35}',
      status: "PENDING_REVIEW",
      createdAt: new Date(Date.now() - 1000 * 60 * 7).toISOString()
    }
  ];
  return {
    content: status ? alerts.filter(a => a.status === status) : alerts,
    totalElements: alerts.length,
    totalPages: 1,
    pageNumber: page,
    pageSize: size,
    last: true
  };
}

function getMockCustomerProfile(id) {
  return {
    customerId: id,
    customerNumber: "CUST-UK-1001",
    customerName: "Oliver Twist",
    email: "oliver.twist@gmail.com",
    phone: "+447911123456",
    homeCity: "London",
    riskTier: "LOW",
    avgTransactionAmount30d: 45.50,
    txCountLast24h: 3,
    overallTrustScore: 92,
    fraudIncidentCount: 0,
    lastKnownIp: "82.132.224.12",
    lastTransactionTime: new Date().toISOString(),
    recentTransactions: [
      { id: "tx-8801", amount: 25.00, currency: "GBP", channel: "POS", merchantName: "Tesco Stores", status: "APPROVED", riskScore: 5, createdAt: new Date(Date.now() - 3600000).toISOString() },
      { id: "tx-8802", amount: 48.00, currency: "GBP", channel: "MOBILE_APP", merchantName: "Costa Coffee", status: "APPROVED", riskScore: 6, createdAt: new Date(Date.now() - 7200000).toISOString() }
    ]
  };
}

function simulateTransactionScore(data, idempotencyKey) {
  let ruleScore = 0;
  const triggers = [];
  if (data.amount > 3000) { ruleScore += 40; triggers.push("RULE_AMOUNT_EXCEEDS_THRESHOLD"); }
  if (data.merchantCode === "MERC-BINANCE") { ruleScore += 35; triggers.push("RULE_HIGH_RISK_MCC_CRYPTO"); }
  if (data.isNewDevice) { ruleScore += 20; triggers.push("RULE_NEW_UNTRUSTED_DEVICE"); }

  let mlScore = Math.min(95, Math.max(5, Math.round(ruleScore * 1.1 + Math.random() * 10)));
  let compositeScore = Math.min(100, Math.round(ruleScore * 0.45 + mlScore * 0.55));
  let status = compositeScore <= 30 ? "APPROVED" : (compositeScore <= 70 ? "REVIEW" : "BLOCKED");

  return {
    success: true,
    data: {
      id: crypto.randomUUID(),
      idempotencyKey,
      customerName: "Demo Customer",
      amount: data.amount,
      currency: "GBP",
      channel: data.channel,
      status,
      riskScore: compositeScore,
      decisionReason: `Composite risk evaluated (${compositeScore}/100) -> ${status}. Triggers: ${triggers.join(", ") || "None"}`,
      createdAt: new Date().toISOString()
    }
  };
}
