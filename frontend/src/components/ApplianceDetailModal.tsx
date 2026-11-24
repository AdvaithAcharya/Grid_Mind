import React, { useEffect, useState } from 'react';
import { AlertTriangle, CheckCircle2, X } from 'lucide-react';
import { gridmindApi } from '../services/gridmindApi';
import type { ApplianceDetailDto, FaultEventLog, TelemetryReading } from '../types/telemetry';
import { BaselineCurveChart } from './SparklineSvg';

interface ApplianceDetailModalProps {
  reading: TelemetryReading | null;
  onClose: () => void;
}

export const ApplianceDetailModal: React.FC<ApplianceDetailModalProps> = ({ reading, onClose }) => {
  const [detail, setDetail] = useState<ApplianceDetailDto | null>(null);
  const [faults, setFaults] = useState<FaultEventLog[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!reading) return;
    let active = true;
    setLoading(true);

    Promise.all([
      gridmindApi.getApplianceDetail(reading.applianceId),
      gridmindApi.getApplianceFaults(reading.applianceId),
    ])
      .then(([d, f]) => {
        if (!active) return;
        setDetail(d);
        setFaults(f);
      })
      .catch(() => {
        if (!active) return;
        setDetail(null);
        setFaults([]);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [reading?.applianceId]);

  if (!reading) return null;
  const currentHour = new Date().getHours();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl border border-slate-700/80 bg-slate-900 p-6 shadow-2xl">
        <div className="flex items-start justify-between border-b border-slate-800 pb-4">
          <div>
            <h2 className="text-lg font-bold text-white">
              {reading.displayName} — Intelligence & Trained Baseline
            </h2>
            <p className="font-mono text-xs text-slate-400">
              Node {reading.applianceId} · Live {reading.powerW.toFixed(1)}W · State {reading.state}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-700 p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-4 space-y-4">
          {loading ? (
            <div className="py-10 text-center text-sm text-slate-400">
              Loading trained baseline curves & telemetry logs...
            </div>
          ) : (
            <>
              <BaselineCurveChart
                profiles={detail?.baselineProfile ?? []}
                currentHour={currentHour}
                liveWatts={reading.powerW}
              />

              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
                  <div className="text-[11px] text-slate-400">Today Usage</div>
                  <div className="font-mono text-base font-semibold text-white">
                    {reading.todayKwh.toFixed(3)} kWh
                  </div>
                </div>
                <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
                  <div className="text-[11px] text-slate-400">Expected (p50)</div>
                  <div className="font-mono text-base font-semibold text-cyan-300">
                    {reading.expectedKwh.toFixed(3)} kWh
                  </div>
                </div>
                <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
                  <div className="text-[11px] text-slate-400">Upper Bound (p90)</div>
                  <div className="font-mono text-base font-semibold text-amber-300">
                    {reading.p90Kwh.toFixed(3)} kWh
                  </div>
                </div>
                <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
                  <div className="text-[11px] text-slate-400">Est. Cost</div>
                  <div className="font-mono text-base font-semibold text-emerald-300">
                    ₹{reading.estimatedCostInr.toFixed(2)}
                  </div>
                </div>
              </div>

              <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Recent Fault & Reflex Events
                </h3>
                {faults.length === 0 ? (
                  <p className="text-xs text-slate-400">
                    No faults logged for this appliance in the active session.
                  </p>
                ) : (
                  <div className="max-h-48 space-y-2 overflow-y-auto pr-1">
                    {faults.map((f) => (
                      <div
                        key={f.id ?? `${f.timestamp}-${f.faultType}`}
                        className="flex items-start justify-between gap-3 rounded-lg border border-slate-800 bg-slate-900/80 p-2.5 text-xs"
                      >
                        <div className="flex items-start gap-2">
                          {f.resolved ? (
                            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-400" />
                          ) : (
                            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
                          )}
                          <div>
                            <div className="font-mono font-semibold text-slate-200">
                              {f.faultType} ({f.faultOrigin})
                            </div>
                            <div className="text-slate-400">{f.diagnosisMessage}</div>
                          </div>
                        </div>
                        <span className="shrink-0 font-mono text-[10px] text-slate-500">
                          {new Date(f.timestamp).toLocaleTimeString()}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
