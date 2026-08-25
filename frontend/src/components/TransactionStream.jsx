import React, { useState } from 'react';
import { Search, ArrowUpRight, CheckCircle2, AlertCircle, XCircle } from 'lucide-react';

export default function TransactionStream({ transactions, onSelectTransaction }) {
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  const filtered = (transactions || []).filter((tx) => {
    const matchesFilter = filter === 'ALL' || tx.status === filter;
    const matchesSearch =
      search === '' ||
      tx.customerName?.toLowerCase().includes(search.toLowerCase()) ||
      tx.merchantName?.toLowerCase().includes(search.toLowerCase()) ||
      tx.id?.toLowerCase().includes(search.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="w-3 h-3 mr-1" /> Approved
          </span>
        );
      case 'REVIEW':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <AlertCircle className="w-3 h-3 mr-1" /> Review
          </span>
        );
      case 'BLOCKED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
            <XCircle className="w-3 h-3 mr-1" /> Blocked
          </span>
        );
      default:
        return <span className="text-xs text-slate-400">{status}</span>;
    }
  };

  const getRiskScoreBar = (score) => {
    let color = 'bg-emerald-500';
    if (score > 70) color = 'bg-rose-500';
    else if (score > 30) color = 'bg-amber-500';

    return (
      <div className="flex items-center space-x-2">
        <div className="w-16 bg-slate-800 rounded-full h-1.5 overflow-hidden">
          <div className={`h-full ${color}`} style={{ width: `${score}%` }}></div>
        </div>
        <span className="font-mono text-xs font-medium text-slate-300">{score}</span>
      </div>
    );
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
      {/* Header & Controls */}
      <div className="p-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-slate-100">Live Transaction Stream</h2>
          <p className="text-xs text-slate-400">Real-time incoming payment events evaluated by Kafka & ML engine</p>
        </div>

        <div className="flex items-center space-x-2">
          {/* Status filters */}
          <div className="flex bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
            {['ALL', 'APPROVED', 'REVIEW', 'BLOCKED'].map((st) => (
              <button
                key={st}
                onClick={() => setFilter(st)}
                className={`px-2.5 py-1 rounded font-medium transition-all ${
                  filter === st ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st}
              </button>
            ))}
          </div>

          {/* Search bar */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search customer, merchant..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 w-44 sm:w-56"
            />
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-950/70 text-slate-400 border-b border-slate-800 uppercase font-mono">
            <tr>
              <th className="py-3 px-4">Customer</th>
              <th className="py-3 px-4">Amount</th>
              <th className="py-3 px-4">Merchant / Channel</th>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4">Risk Score</th>
              <th className="py-3 px-4">Decision Reason</th>
              <th className="py-3 px-4 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan="7" className="py-8 text-center text-slate-500">
                  No transactions found matching the filter criteria.
                </td>
              </tr>
            ) : (
              filtered.map((tx) => (
                <tr key={tx.id} className="hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4">
                    <div className="font-medium text-slate-200">{tx.customerName || 'Unknown Customer'}</div>
                    <div className="text-[11px] text-slate-500 font-mono">
                      {tx.createdAt ? new Date(tx.createdAt).toLocaleTimeString('en-GB') : 'Just now'}
                    </div>
                  </td>
                  <td className="py-3 px-4">
                    <span className="font-semibold text-slate-100 font-mono">
                      £{parseFloat(tx.amount || 0).toFixed(2)}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="text-slate-200">{tx.merchantName || 'Direct Transfer'}</div>
                    <div className="text-[10px] text-slate-400">{tx.channel || 'ONLINE_BANKING'}</div>
                  </td>
                  <td className="py-3 px-4">{getStatusBadge(tx.status)}</td>
                  <td className="py-3 px-4">{getRiskScoreBar(tx.riskScore ?? 15)}</td>
                  <td className="py-3 px-4 max-w-xs truncate text-slate-400" title={tx.decisionReason}>
                    {tx.decisionReason || 'Normal transaction pattern'}
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => onSelectTransaction && onSelectTransaction(tx)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-blue-400 hover:bg-slate-800 transition-colors"
                      title="Inspect Details"
                    >
                      <ArrowUpRight className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
