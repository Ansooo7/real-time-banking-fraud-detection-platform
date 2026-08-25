import React, { useState } from 'react';
import { ShieldAlert, CheckCircle, XCircle, AlertCircle, Send, User, MapPin } from 'lucide-react';
import { fraudAlertService } from '../services/api';

export default function FraudAlertsQueue({ alerts, onAlertUpdated }) {
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [notes, setNotes] = useState('');
  const [loadingAction, setLoadingAction] = useState(false);

  const handleAction = async (alertId, decision) => {
    setLoadingAction(true);
    try {
      await fraudAlertService.submitDecision(alertId, decision, notes || `Actioned as ${decision}`);
      if (onAlertUpdated) onAlertUpdated();
      setSelectedAlert(null);
      setNotes('');
    } catch (err) {
      console.error('Failed to submit decision:', err);
    } finally {
      setLoadingAction(false);
    }
  };

  const parseJson = (str) => {
    try {
      return typeof str === 'string' ? JSON.parse(str) : str;
    } catch (e) {
      return [];
    }
  };

  return (
    <div className="space-y-4">
      {/* Alert Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 className="text-lg font-bold text-slate-100 flex items-center space-x-2">
              <ShieldAlert className="w-5 h-5 text-amber-400" />
              <span>Fraud Analyst Review Workbench</span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              High & medium risk flagged transactions requiring manual triage under UK Banking compliance
            </p>
          </div>
          <span className="text-xs bg-amber-500/10 text-amber-400 border border-amber-500/30 px-3 py-1 rounded-full font-medium">
            {alerts?.length || 0} Alerts in Queue
          </span>
        </div>
      </div>

      {/* Grid of Alert Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {alerts?.length === 0 ? (
          <div className="col-span-full py-12 text-center bg-slate-900 border border-slate-800 rounded-xl text-slate-500">
            <CheckCircle className="w-8 h-8 mx-auto text-emerald-500 mb-2 opacity-80" />
            <p className="font-medium text-slate-300">All Fraud Alerts Resolved</p>
            <p className="text-xs text-slate-500">No transactions currently pending analyst triage.</p>
          </div>
        ) : (
          alerts.map((alert) => {
            const triggers = parseJson(alert.triggeredRules);
            const mlFactors = parseJson(alert.mlFeatureContributions);

            return (
              <div
                key={alert.id}
                className="bg-slate-900 border border-slate-800 hover:border-slate-700 rounded-xl p-5 flex flex-col justify-between transition-all shadow-sm"
              >
                <div>
                  {/* Top Bar */}
                  <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-3">
                    <div>
                      <span className="text-[11px] font-mono text-slate-400">{alert.customerNumber}</span>
                      <h4 className="text-sm font-bold text-slate-100">{alert.customerName}</h4>
                    </div>
                    <div className="text-right">
                      <span className="text-base font-bold text-slate-100 font-mono">
                        £{parseFloat(alert.amount || 0).toFixed(2)}
                      </span>
                      <div className="text-[10px] text-slate-500">{alert.merchantName}</div>
                    </div>
                  </div>

                  {/* Scores Comparison */}
                  <div className="grid grid-cols-3 gap-2 bg-slate-950 p-2.5 rounded-lg border border-slate-800/80 mb-3 text-center">
                    <div>
                      <div className="text-[10px] text-slate-400">Rule Score</div>
                      <div className="font-mono text-sm font-bold text-blue-400">{alert.ruleScore}/100</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-400">ML Score</div>
                      <div className="font-mono text-sm font-bold text-indigo-400">{alert.mlScore}/100</div>
                    </div>
                    <div>
                      <div className="text-[10px] text-slate-400">Composite</div>
                      <div className="font-mono text-sm font-bold text-rose-400">{alert.compositeRiskScore}/100</div>
                    </div>
                  </div>

                  {/* Triggered Heuristics */}
                  <div className="space-y-1.5 mb-4">
                    <p className="text-[11px] font-medium text-slate-400 uppercase tracking-wide">Triggered Rules:</p>
                    <div className="flex flex-wrap gap-1">
                      {Array.isArray(triggers) && triggers.length > 0 ? (
                        triggers.map((rule, idx) => (
                          <span
                            key={idx}
                            className="bg-rose-500/10 text-rose-300 border border-rose-500/20 text-[10px] px-2 py-0.5 rounded font-mono"
                          >
                            {rule}
                          </span>
                        ))
                      ) : (
                        <span className="text-xs text-slate-500">Anomaly flagged by statistical ML model</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Action Button */}
                <button
                  onClick={() => setSelectedAlert(alert)}
                  className="w-full bg-blue-600 hover:bg-blue-500 text-white font-medium py-2 rounded-lg text-xs transition-colors shadow-sm"
                >
                  Triage & Resolve Alert
                </button>
              </div>
            );
          })
        )}
      </div>

      {/* Analyst Decision Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="text-base font-bold text-slate-100">Fraud Alert Investigation & Action</h3>
                <p className="text-xs text-slate-400">Case Reference: {selectedAlert.id}</p>
              </div>
              <button
                onClick={() => setSelectedAlert(null)}
                className="text-slate-400 hover:text-slate-200 text-sm"
              >
                ✕
              </button>
            </div>

            {/* Case Details */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-400">Customer:</span>
                <span className="font-semibold text-slate-200">{selectedAlert.customerName} ({selectedAlert.customerNumber})</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Transaction Amount:</span>
                <span className="font-bold text-slate-100 font-mono">£{parseFloat(selectedAlert.amount || 0).toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Merchant:</span>
                <span className="text-slate-200">{selectedAlert.merchantName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Risk Assessment:</span>
                <span className="font-mono text-rose-400 font-bold">{selectedAlert.compositeRiskScore}/100 (HIGH RISK)</span>
              </div>
            </div>

            {/* Analyst Notes */}
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">
                Investigation Notes & Audit Rationale:
              </label>
              <textarea
                rows="3"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g., Customer confirmed unauthorized overseas card activity. Device compromised."
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-3 gap-3 pt-2">
              <button
                disabled={loadingAction}
                onClick={() => handleAction(selectedAlert.id, 'CONFIRMED_FRAUD')}
                className="bg-rose-600 hover:bg-rose-500 text-white font-medium py-2.5 px-3 rounded-lg text-xs flex items-center justify-center space-x-1.5 transition-colors"
              >
                <XCircle className="w-4 h-4" />
                <span>Confirm Fraud</span>
              </button>

              <button
                disabled={loadingAction}
                onClick={() => handleAction(selectedAlert.id, 'FALSE_POSITIVE')}
                className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium py-2.5 px-3 rounded-lg text-xs flex items-center justify-center space-x-1.5 transition-colors"
              >
                <CheckCircle className="w-4 h-4" />
                <span>False Positive</span>
              </button>

              <button
                disabled={loadingAction}
                onClick={() => handleAction(selectedAlert.id, 'DISMISSED')}
                className="bg-slate-700 hover:bg-slate-600 text-slate-200 font-medium py-2.5 px-3 rounded-lg text-xs flex items-center justify-center space-x-1.5 transition-colors"
              >
                <span>Dismiss Alert</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
