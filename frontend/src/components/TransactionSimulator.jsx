import React, { useState } from 'react';
import { PlayCircle, ShieldCheck, ShieldAlert, ShieldX, Zap, CheckCircle2, RefreshCw } from 'lucide-react';
import { transactionService } from '../services/api';

export default function TransactionSimulator({ onTransactionCreated }) {
  const templates = [
    {
      name: 'Legitimate Morning Coffee',
      desc: 'Typical daily routine at Costa Coffee from trusted iPhone in London',
      payload: {
        sourceAccountId: 'a0000001-0000-0000-0000-000000000001',
        destinationAccountNumber: '87654321',
        amount: 4.80,
        currency: 'GBP',
        merchantCode: 'MERC-TESCO',
        channel: 'POS',
        deviceFingerprint: 'iphone_15_pro_oliver',
        isNewDevice: false,
        ipAddress: '82.132.224.12',
        latitude: 51.5074,
        longitude: -0.1278,
      },
    },
    {
      name: 'Account Takeover: Midnight Crypto Buy',
      desc: '£4,200.00 at 03:25 AM to Binance UK from untrusted foreign IP & new device',
      payload: {
        sourceAccountId: 'a0000001-0000-0000-0000-000000000001',
        destinationAccountNumber: '99887766',
        amount: 4200.00,
        currency: 'GBP',
        merchantCode: 'MERC-BINANCE',
        channel: 'ONLINE_BANKING',
        deviceFingerprint: 'hacker_kali_linux_fingerprint',
        isNewDevice: true,
        ipAddress: '185.220.101.5',
        latitude: 51.5074,
        longitude: -0.1278,
      },
    },
    {
      name: 'Impossible Travel Geo-Hop (Tokyo)',
      desc: '£1,450.00 in Tokyo 15 minutes after London transaction (>800 km/h anomaly)',
      payload: {
        sourceAccountId: 'a0000001-0000-0000-0000-000000000001',
        destinationAccountNumber: '33445566',
        amount: 1450.00,
        currency: 'GBP',
        merchantCode: 'MERC-AMZN',
        channel: 'ONLINE_BANKING',
        deviceFingerprint: 'foreign_macbook_tokyo',
        isNewDevice: true,
        ipAddress: '133.242.18.2',
        latitude: 35.6762,
        longitude: 139.6503,
      },
    },
    {
      name: 'High-Velocity Burst (Betting / Gambling)',
      desc: 'Rapid successive £650.00 transfers to Bet365 Online',
      payload: {
        sourceAccountId: 'a0000001-0000-0000-0000-000000000001',
        destinationAccountNumber: '77665544',
        amount: 650.00,
        currency: 'GBP',
        merchantCode: 'MERC-BET365',
        channel: 'MOBILE_APP',
        deviceFingerprint: 'iphone_15_pro_oliver',
        isNewDevice: false,
        ipAddress: '82.132.224.12',
        latitude: 51.5074,
        longitude: -0.1278,
      },
    },
  ];

  const [form, setForm] = useState(templates[0].payload);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleApplyTemplate = (tmpl) => {
    setForm(tmpl.payload);
    setResult(null);
  };

  const handleRunSimulation = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await transactionService.createTransaction(form);
      setResult(res.data);
      if (onTransactionCreated) onTransactionCreated();
    } catch (err) {
      console.error('Simulation error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex items-center space-x-2">
          <PlayCircle className="w-5 h-5 text-blue-400" />
          <h2 className="text-base font-bold text-slate-100">Live Transaction Test Bench & Fraud Simulator</h2>
        </div>
        <p className="text-xs text-slate-400 mt-1">
          Execute real or synthetic payment events through the live Spring Boot $\rightarrow$ Kafka $\rightarrow$ Rule Engine $\rightarrow$ ML Scoring pipeline in real time.
        </p>
      </div>

      {/* Templates Selector */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {templates.map((tmpl, idx) => (
          <button
            key={idx}
            onClick={() => handleApplyTemplate(tmpl)}
            className="text-left bg-slate-900 border border-slate-800 hover:border-blue-500/50 p-4 rounded-xl transition-all shadow-sm group"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-200 group-hover:text-blue-400">{tmpl.name}</span>
              <Zap className="w-3.5 h-3.5 text-slate-500 group-hover:text-blue-400" />
            </div>
            <p className="text-[11px] text-slate-400 mt-1.5 line-clamp-2">{tmpl.desc}</p>
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Input Form */}
        <div className="lg:col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm">
          <h3 className="text-sm font-bold text-slate-100 mb-4">Transaction Parameters</h3>
          <form onSubmit={handleRunSimulation} className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-400 mb-1">Amount (£ GBP):</label>
                <input
                  type="number"
                  step="0.01"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: parseFloat(e.target.value) || 0 })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 font-mono focus:outline-none focus:border-blue-500 font-bold"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Channel:</label>
                <select
                  value={form.channel}
                  onChange={(e) => setForm({ ...form, channel: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                >
                  <option value="MOBILE_APP">Mobile App</option>
                  <option value="ONLINE_BANKING">Online Banking</option>
                  <option value="POS">Point of Sale (POS)</option>
                  <option value="ATM">ATM Withdrawal</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-400 mb-1">Merchant / Recipient:</label>
                <select
                  value={form.merchantCode || ''}
                  onChange={(e) => setForm({ ...form, merchantCode: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                >
                  <option value="MERC-TESCO">Tesco Stores (Groceries)</option>
                  <option value="MERC-AMZN">Amazon UK (Retail)</option>
                  <option value="MERC-CURRYS">Currys PC World (Electronics)</option>
                  <option value="MERC-BET365">Bet365 (Gambling)</option>
                  <option value="MERC-BINANCE">Binance UK (Crypto)</option>
                  <option value="MERC-WESTERNUNION">Western Union (Wire)</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Device State:</label>
                <select
                  value={form.isNewDevice ? 'true' : 'false'}
                  onChange={(e) => setForm({ ...form, isNewDevice: e.target.value === 'true' })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                >
                  <option value="false">Trusted Registered Device</option>
                  <option value="true">New / Untrusted Device</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-400 mb-1">Origin IP Address:</label>
                <input
                  type="text"
                  value={form.ipAddress}
                  onChange={(e) => setForm({ ...form, ipAddress: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 font-mono focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1">Destination Account Number:</label>
                <input
                  type="text"
                  value={form.destinationAccountNumber}
                  onChange={(e) => setForm({ ...form, destinationAccountNumber: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-slate-100 font-mono focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 rounded-lg text-xs flex items-center justify-center space-x-2 transition-all shadow-md mt-4"
            >
              {loading ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <PlayCircle className="w-4 h-4" />
              )}
              <span>{loading ? 'Evaluating Transaction Risk...' : 'Execute & Score Transaction'}</span>
            </button>
          </form>
        </div>

        {/* Live Evaluation Result Card */}
        <div className="lg:col-span-6 bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-100 mb-3">Live Risk Pipeline Execution Result</h3>
            {!result ? (
              <div className="py-16 text-center text-slate-500">
                <PlayCircle className="w-10 h-10 mx-auto text-slate-700 mb-2" />
                <p className="font-medium text-slate-400 text-xs">Awaiting Transaction Execution</p>
                <p className="text-[11px] text-slate-600 mt-0.5">Select a template and click 'Execute & Score Transaction'</p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Status Hero Banner */}
                <div
                  className={`p-4 rounded-xl border flex items-center justify-between ${
                    result.status === 'APPROVED'
                      ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-300'
                      : result.status === 'REVIEW'
                      ? 'bg-amber-950/20 border-amber-500/30 text-amber-300'
                      : 'bg-rose-950/20 border-rose-500/30 text-rose-300'
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    {result.status === 'APPROVED' ? (
                      <ShieldCheck className="w-8 h-8 text-emerald-400" />
                    ) : result.status === 'REVIEW' ? (
                      <ShieldAlert className="w-8 h-8 text-amber-400" />
                    ) : (
                      <ShieldX className="w-8 h-8 text-rose-400" />
                    )}
                    <div>
                      <span className="text-[10px] uppercase tracking-wider font-mono opacity-80">Banking Decision</span>
                      <h4 className="text-xl font-extrabold tracking-tight">{result.status}</h4>
                    </div>
                  </div>

                  <div className="text-right font-mono">
                    <span className="text-[10px] uppercase opacity-80">Composite Score</span>
                    <div className="text-2xl font-bold">{result.riskScore}/100</div>
                  </div>
                </div>

                {/* Audit & Explainability Detail */}
                <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Transaction ID:</span>
                    <span className="font-mono text-slate-300 text-[11px]">{result.id}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Idempotency Key:</span>
                    <span className="font-mono text-slate-400 text-[10px]">{result.idempotencyKey}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Executed Amount:</span>
                    <span className="font-bold text-slate-100 font-mono">£{parseFloat(result.amount || 0).toFixed(2)}</span>
                  </div>
                  <div className="border-t border-slate-800 pt-2">
                    <span className="text-slate-400 block mb-1">Decision Rationale:</span>
                    <p className="text-slate-200 bg-slate-900 p-2 rounded border border-slate-800 text-[11px] leading-relaxed">
                      {result.decisionReason}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="text-[11px] text-slate-500 border-t border-slate-800/80 pt-3 mt-4 flex items-center justify-between">
            <span>Kafka Event: <code className="text-blue-400">bank.transactions.created</code></span>
            <span>Audit Trail: <span className="text-emerald-400">Persisted</span></span>
          </div>
        </div>
      </div>
    </div>
  );
}
