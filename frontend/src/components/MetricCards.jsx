import React from 'react';
import { DollarSign, ShieldCheck, AlertTriangle, ShieldX } from 'lucide-react';

export default function MetricCards({ summary }) {
  if (!summary) return null;

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP', maximumFractionDigits: 0 }).format(val || 0);
  };

  const cards = [
    {
      title: '24h Processed Volume',
      value: formatCurrency(summary.totalVolumeGbp),
      subtitle: `${summary.totalTransactions24h?.toLocaleString() || 0} Transactions`,
      icon: DollarSign,
      iconBg: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    },
    {
      title: 'System Fraud Rate',
      value: `${summary.fraudRatePercent || 0}%`,
      subtitle: 'Industry Benchmark < 1.8%',
      icon: ShieldCheck,
      iconBg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    },
    {
      title: 'Open Review Queue',
      value: summary.openAlertsCount || 0,
      subtitle: `${summary.reviewCount || 0} Flagged in 24h`,
      icon: AlertTriangle,
      iconBg: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    },
    {
      title: 'Blocked Fraud Value',
      value: formatCurrency(summary.totalBlockedAmountGbp),
      subtitle: `${summary.blockedCount || 0} Transactions Prevented`,
      icon: ShieldX,
      iconBg: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card, idx) => {
        const Icon = card.icon;
        return (
          <div
            key={idx}
            className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-all shadow-sm"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-slate-400 uppercase tracking-wider">{card.title}</p>
                <h3 className="text-2xl font-bold text-slate-100 mt-1 tracking-tight">{card.value}</h3>
                <p className="text-xs text-slate-500 mt-1">{card.subtitle}</p>
              </div>
              <div className={`p-3 rounded-xl border ${card.iconBg}`}>
                <Icon className="w-6 h-6" />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
