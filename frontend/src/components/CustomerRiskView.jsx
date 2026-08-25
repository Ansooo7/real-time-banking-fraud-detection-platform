import React, { useState, useEffect } from 'react';
import { User, Shield, AlertTriangle, Clock, MapPin, CreditCard, Activity } from 'lucide-react';
import { customerRiskService } from '../services/api';

export default function CustomerRiskView() {
  const sampleCustomers = [
    { id: 'c0000001-0000-0000-0000-000000000001', name: 'Oliver Twist (CUST-UK-1001)', tier: 'LOW' },
    { id: 'c0000002-0000-0000-0000-000000000002', name: 'Emma Watson (CUST-UK-1002)', tier: 'LOW' },
    { id: 'c0000003-0000-0000-0000-000000000003', name: 'James Bond (CUST-UK-1003)', tier: 'MEDIUM' },
    { id: 'c0000004-0000-0000-0000-000000000004', name: 'Arthur Shelby (CUST-UK-1004)', tier: 'HIGH' },
  ];

  const [selectedCustomerId, setSelectedCustomerId] = useState(sampleCustomers[0].id);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadProfile(selectedCustomerId);
  }, [selectedCustomerId]);

  const loadProfile = async (id) => {
    setLoading(true);
    try {
      const data = await customerRiskService.getProfile(id);
      setProfile(data);
    } catch (err) {
      console.error('Failed to load profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const getTierBadge = (tier) => {
    switch (tier) {
      case 'LOW':
        return <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs px-2.5 py-0.5 rounded-full font-medium">Low Risk Tier</span>;
      case 'MEDIUM':
        return <span className="bg-amber-500/10 text-amber-400 border border-amber-500/20 text-xs px-2.5 py-0.5 rounded-full font-medium">Medium Risk Tier</span>;
      case 'HIGH':
        return <span className="bg-rose-500/10 text-rose-400 border border-rose-500/20 text-xs px-2.5 py-0.5 rounded-full font-medium">High Risk Tier</span>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Customer Selector Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center space-x-2">
            <User className="w-5 h-5 text-blue-400" />
            <span>Customer Behavioral Risk Profiling</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Continuous KYC/AML behavioral baseline monitoring and trust score telemetry
          </p>
        </div>

        {/* Customer Select dropdown */}
        <select
          value={selectedCustomerId}
          onChange={(e) => setSelectedCustomerId(e.target.value)}
          className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-medium"
        >
          {sampleCustomers.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name} - [{c.tier}]
            </option>
          ))}
        </select>
      </div>

      {profile && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Profile Overview Card */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-[11px] font-mono text-slate-400">{profile.customerNumber}</span>
                <h3 className="text-lg font-bold text-slate-100">{profile.customerName}</h3>
                <p className="text-xs text-slate-400">{profile.email}</p>
              </div>
              {getTierBadge(profile.riskTier)}
            </div>

            <div className="border-t border-slate-800 pt-4 space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-400 flex items-center space-x-1.5">
                  <MapPin className="w-3.5 h-3.5 text-slate-500" />
                  <span>Home Jurisdiction</span>
                </span>
                <span className="text-slate-200 font-medium">{profile.homeCity}, United Kingdom</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-400 flex items-center space-x-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-500" />
                  <span>Last Seen IP</span>
                </span>
                <span className="text-slate-200 font-mono">{profile.lastKnownIp || '82.132.224.12'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-400 flex items-center space-x-1.5">
                  <AlertTriangle className="w-3.5 h-3.5 text-slate-500" />
                  <span>Prior Fraud Incidents</span>
                </span>
                <span className="text-rose-400 font-bold font-mono">{profile.fraudIncidentCount || 0}</span>
              </div>
            </div>

            {/* Trust Score Gauge */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 text-center space-y-2">
              <div className="text-xs text-slate-400 font-medium uppercase tracking-wide">Overall Trust Score</div>
              <div className="text-3xl font-extrabold text-emerald-400 font-mono">
                {profile.overallTrustScore || 95}/100
              </div>
              <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
                <div
                  className={`h-full ${profile.overallTrustScore > 75 ? 'bg-emerald-500' : 'bg-amber-500'}`}
                  style={{ width: `${profile.overallTrustScore || 95}%` }}
                ></div>
              </div>
            </div>
          </div>

          {/* Behavioral Baselines & History */}
          <div className="lg:col-span-2 space-y-6">
            {/* Top metric chips */}
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400 uppercase font-mono">30-Day Average Spend Baseline</p>
                <h4 className="text-2xl font-bold text-slate-100 mt-1 font-mono">
                  £{parseFloat(profile.avgTransactionAmount30d || 45.5).toFixed(2)}
                </h4>
                <p className="text-[11px] text-slate-500 mt-1">Normal deviation bounds: ±£{(profile.avgTransactionAmount30d * 0.4).toFixed(0)}</p>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400 uppercase font-mono">24h Velocity Count</p>
                <h4 className="text-2xl font-bold text-slate-100 mt-1 font-mono">
                  {profile.txCountLast24h || 2} <span className="text-xs text-slate-400 font-sans font-normal">Transactions</span>
                </h4>
                <p className="text-[11px] text-slate-500 mt-1">Typical velocity threshold: &lt; 5/day</p>
              </div>
            </div>

            {/* Recent Transaction History */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
              <h4 className="text-sm font-semibold text-slate-100 mb-3 flex items-center space-x-2">
                <Activity className="w-4 h-4 text-blue-400" />
                <span>Recent Customer Activity Log</span>
              </h4>
              <div className="overflow-x-auto">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-950 text-slate-400 border-b border-slate-800 uppercase font-mono">
                    <tr>
                      <th className="py-2.5 px-3">Merchant / Counterparty</th>
                      <th className="py-2.5 px-3">Amount</th>
                      <th className="py-2.5 px-3">Channel</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3">Risk Score</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-mono">
                    {(profile.recentTransactions || []).map((tx) => (
                      <tr key={tx.id} className="hover:bg-slate-800/30">
                        <td className="py-2.5 px-3 font-sans text-slate-200">{tx.merchantName || 'Direct Transfer'}</td>
                        <td className="py-2.5 px-3 font-bold text-slate-100">£{parseFloat(tx.amount || 0).toFixed(2)}</td>
                        <td className="py-2.5 px-3 text-slate-400 font-sans">{tx.channel}</td>
                        <td className="py-2.5 px-3 font-sans">
                          <span className={`text-[11px] font-semibold px-2 py-0.5 rounded ${
                            tx.status === 'APPROVED' ? 'text-emerald-400 bg-emerald-500/10' :
                            tx.status === 'REVIEW' ? 'text-amber-400 bg-amber-500/10' : 'text-rose-400 bg-rose-500/10'
                          }`}>
                            {tx.status}
                          </span>
                        </td>
                        <td className="py-2.5 px-3 text-slate-300">{tx.riskScore ?? 12}/100</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
