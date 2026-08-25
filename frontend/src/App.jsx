import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import MetricCards from './components/MetricCards';
import TransactionStream from './components/TransactionStream';
import FraudAlertsQueue from './components/FraudAlertsQueue';
import ModelPerformance from './components/ModelPerformance';
import CustomerRiskView from './components/CustomerRiskView';
import TransactionSimulator from './components/TransactionSimulator';
import {
  authService,
  analyticsService,
  transactionService,
  fraudAlertService,
} from './services/api';
import { ShieldAlert, LogIn, Lock } from 'lucide-react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';

export default function App() {
  const [user, setUser] = useState(authService.getCurrentUser());
  const [activeTab, setActiveTab] = useState('overview');
  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [modelInfo, setModelInfo] = useState(null);
  const [selectedTx, setSelectedTx] = useState(null);

  // Login form state for initial authentication
  const [username, setUsername] = useState('analyst');
  const [password, setPassword] = useState('Password123!');
  const [loginLoading, setLoginLoading] = useState(false);

  useEffect(() => {
    if (user) {
      loadAllData();
      const interval = setInterval(loadAllData, 12000); // 12s live polling
      return () => clearInterval(interval);
    }
  }, [user]);

  const loadAllData = async () => {
    try {
      const [sumData, txData, alertData, mlData] = await Promise.all([
        analyticsService.getSummary(),
        transactionService.getTransactions(0, 25),
        fraudAlertService.getAlerts(0, 25, 'PENDING_REVIEW'),
        analyticsService.getModelPerformance(),
      ]);

      if (sumData) setSummary(sumData);
      if (txData?.content) setTransactions(txData.content);
      if (alertData?.content) setAlerts(alertData.content);
      if (mlData) setModelInfo(mlData);
    } catch (err) {
      console.error('Error fetching dashboard state:', err);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoginLoading(true);
    try {
      const res = await authService.login(username, password);
      setUser(res.data);
    } catch (err) {
      console.error('Login failed:', err);
    } finally {
      setLoginLoading(false);
    }
  };

  const handleLogout = () => {
    authService.logout();
    setUser(null);
  };

  // If not logged in, render Fintech authentication portal
  if (!user) {
    return (
      <div className="min-h-screen bg-[#0a0f1d] flex items-center justify-center p-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-8 shadow-2xl space-y-6">
          <div className="text-center space-y-2">
            <div className="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto shadow-lg shadow-blue-500/20">
              <ShieldAlert className="w-7 h-7 text-white" />
            </div>
            <h1 className="text-xl font-bold text-slate-100">UK Bank Risk Intelligence</h1>
            <p className="text-xs text-slate-400">Real-Time Fraud Prevention & Anomaly Detection Portal</p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4 text-xs">
            <div>
              <label className="block text-slate-300 font-medium mb-1">Username / Staff ID</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 font-mono"
                required
              />
            </div>

            <div>
              <label className="block text-slate-300 font-medium mb-1">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 font-mono"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loginLoading}
              className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-2.5 rounded-lg transition-colors flex items-center justify-center space-x-2 mt-2 shadow-sm"
            >
              <LogIn className="w-4 h-4" />
              <span>{loginLoading ? 'Authenticating...' : 'Sign In to Risk Console'}</span>
            </button>
          </form>

          <div className="border-t border-slate-800 pt-4 text-center">
            <p className="text-[11px] text-slate-500">
              Default Credentials: <code className="text-blue-400">analyst</code> / <code className="text-blue-400">Password123!</code>
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Distribution chart data
  const riskDistData = [
    { name: 'Approved (0-30)', value: summary?.riskTierDistribution?.LOW_RISK_0_30 || 17920, color: '#10b981' },
    { name: 'Review (31-70)', value: summary?.riskTierDistribution?.MEDIUM_RISK_31_70 || 380, color: '#f59e0b' },
    { name: 'Blocked (71-100)', value: summary?.riskTierDistribution?.HIGH_RISK_71_100 || 150, color: '#ef4444' },
  ];

  return (
    <div className="min-h-screen bg-[#0a0f1d] text-slate-100 flex flex-col">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} user={user} onLogout={handleLogout} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {activeTab === 'overview' && (
          <div className="space-y-6">
            <MetricCards summary={summary} />

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Live Transactions Column */}
              <div className="lg:col-span-8">
                <TransactionStream
                  transactions={transactions}
                  onSelectTransaction={(tx) => setSelectedTx(tx)}
                />
              </div>

              {/* Distribution & Channel Breakdown Column */}
              <div className="lg:col-span-4 space-y-6">
                {/* Risk Distribution Chart */}
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
                  <h3 className="text-sm font-semibold text-slate-100 mb-1">Risk Score Distribution</h3>
                  <p className="text-[11px] text-slate-400 mb-3">24h transaction volume classified by risk score</p>
                  <div className="h-44 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={riskDistData}
                          innerRadius={45}
                          outerRadius={65}
                          paddingAngle={4}
                          dataKey="value"
                        >
                          {riskDistData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip
                          contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', fontSize: '11px' }}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                  <div className="grid grid-cols-3 gap-1 text-center text-[10px] font-mono mt-1">
                    <div><span className="text-emerald-400">●</span> 97.1% Low</div>
                    <div><span className="text-amber-400">●</span> 2.1% Review</div>
                    <div><span className="text-rose-400">●</span> 0.8% Block</div>
                  </div>
                </div>

                {/* Quick Alerts Callout */}
                <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-slate-100 flex items-center space-x-2">
                      <ShieldAlert className="w-4 h-4 text-amber-400" />
                      <span>Pending Triage</span>
                    </h3>
                    <button
                      onClick={() => setActiveTab('alerts')}
                      className="text-xs text-blue-400 hover:text-blue-300 font-medium"
                    >
                      View All →
                    </button>
                  </div>
                  <div className="space-y-2">
                    {alerts.slice(0, 3).map((a) => (
                      <div
                        key={a.id}
                        onClick={() => setActiveTab('alerts')}
                        className="bg-slate-950 p-2.5 rounded-lg border border-slate-800/80 hover:border-slate-700 cursor-pointer text-xs flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="font-semibold text-slate-200">{a.customerName}</div>
                          <div className="text-[10px] text-slate-500 font-mono">£{parseFloat(a.amount || 0).toFixed(2)}</div>
                        </div>
                        <span className="font-mono text-xs font-bold text-rose-400">{a.compositeRiskScore}/100</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'alerts' && (
          <FraudAlertsQueue alerts={alerts} onAlertUpdated={loadAllData} />
        )}

        {activeTab === 'models' && <ModelPerformance modelInfo={modelInfo} />}

        {activeTab === 'customers' && <CustomerRiskView />}

        {activeTab === 'simulator' && (
          <TransactionSimulator onTransactionCreated={loadAllData} />
        )}
      </main>

      {/* Transaction Detail Modal */}
      {selectedTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-slate-100">Transaction Inspection</h3>
              <button
                onClick={() => setSelectedTx(null)}
                className="text-slate-400 hover:text-slate-200"
              >
                ✕
              </button>
            </div>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Transaction ID:</span>
                <span className="font-mono text-slate-200">{selectedTx.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Customer:</span>
                <span className="font-semibold text-slate-200">{selectedTx.customerName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Amount:</span>
                <span className="font-bold text-slate-100 font-mono">£{parseFloat(selectedTx.amount || 0).toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Merchant:</span>
                <span className="text-slate-200">{selectedTx.merchantName} ({selectedTx.merchantCategory})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Status & Risk Score:</span>
                <span className="font-bold text-slate-200 font-mono">{selectedTx.status} ({selectedTx.riskScore}/100)</span>
              </div>
              <div className="border-t border-slate-800 pt-2">
                <span className="text-slate-400 block mb-1">Decision Reason:</span>
                <p className="text-slate-300 bg-slate-900 p-2 rounded border border-slate-800 leading-relaxed">
                  {selectedTx.decisionReason}
                </p>
              </div>
            </div>

            <button
              onClick={() => setSelectedTx(null)}
              className="w-full bg-slate-800 hover:bg-slate-700 text-slate-200 py-2 rounded-lg text-xs font-medium transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
