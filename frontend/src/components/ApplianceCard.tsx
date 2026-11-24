import React from 'react';
import {
  Activity,
  AlertTriangle,
  Coffee,
  Fan,
  Flame,
  Power,
  Refrigerator,
  ShieldAlert,
  ShieldCheck,
  Thermometer,
  Tv,
  Wind,
  Zap,
} from 'lucide-react';
import {
  getDriftRate,
  type ApplianceState,
  type ApplianceType,
  type SimulatedFaultType,
  type TelemetryReading,
} from '../types/telemetry';
import { SparklineSvg } from './SparklineSvg';

interface ApplianceCardProps {
  reading: TelemetryReading;
  history: number[];
  activeScenario: SimulatedFaultType;
  onInjectScenario: (id: string, scenario: SimulatedFaultType) => void;
  onOpenReset: (reading: TelemetryReading) => void;
  onInspect: (reading: TelemetryReading) => void;
}

const APPLIANCE_ICON: Record<ApplianceType, React.ReactNode> = {
  FAN: <Fan className="h-5 w-5" />,
  AC: <Wind className="h-5 w-5" />,
  FRIDGE: <Refrigerator className="h-5 w-5" />,
  GEYSER: <Flame className="h-5 w-5" />,
  KETTLE: <Coffee className="h-5 w-5" />,
  TV: <Tv className="h-5 w-5" />,
};

function getStateMeta(state: ApplianceState) {
  switch (state) {
    case 'ONLINE':
      return {
        label: 'ONLINE',
        badge: 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300',
        border: 'border-slate-800 hover:border-emerald-500/40',
        sparkStroke: '#10b981',
        sparkFill: 'rgba(16, 185, 129, 0.14)',
        pulse: false,
      };
    case 'LEARNING':
      return {
        label: 'LEARNING',
        badge: 'border-cyan-500/40 bg-cyan-500/15 text-cyan-300',
        border: 'border-cyan-500/40',
        sparkStroke: '#22d3ee',
        sparkFill: 'rgba(34, 211, 238, 0.14)',
        pulse: true,
      };
    case 'WARNING_DRIFT':
      return {
        label: 'WARNING DRIFT',
        badge: 'border-amber-500/50 bg-amber-500/15 text-amber-300',
        border: 'border-amber-500/60 shadow-[0_0_24px_-8px_rgba(245,158,11,0.35)]',
        sparkStroke: '#f59e0b',
        sparkFill: 'rgba(245, 158, 11, 0.16)',
        pulse: true,
      };
    case 'WARNING_ARCING':
      return {
        label: 'WARNING ARCING',
        badge: 'border-orange-500/60 bg-orange-500/20 text-orange-300',
        border: 'border-orange-500/70 shadow-[0_0_26px_-6px_rgba(249,115,22,0.45)]',
        sparkStroke: '#f97316',
        sparkFill: 'rgba(249, 115, 22, 0.18)',
        pulse: true,
      };
    case 'TRIPPED_LATCHED':
      return {
        label: 'TRIPPED LATCHED',
        badge: 'border-rose-500/70 bg-rose-500/20 text-rose-300',
        border: 'border-rose-500/80 shadow-[0_0_30px_-6px_rgba(244,63,94,0.5)]',
        sparkStroke: '#f43f5e',
        sparkFill: 'rgba(244, 63, 94, 0.18)',
        pulse: true,
      };
    case 'FAULT_HARDWARE':
      return {
        label: 'FAULT HARDWARE',
        badge: 'border-red-600/80 bg-red-600/25 text-red-200',
        border: 'border-red-600/90',
        sparkStroke: '#ef4444',
        sparkFill: 'rgba(239, 68, 68, 0.2)',
        pulse: true,
      };
  }
}

const SCENARIO_OPTIONS: { key: SimulatedFaultType; label: string }[] = [
  { key: 'NORMAL', label: 'Normal' },
  { key: 'LOOSE_SOCKET_ARCING', label: 'Arcing' },
  { key: 'THERMAL_PIN_DRIFT', label: 'Drift' },
  { key: 'HIGH_TODAY_USAGE', label: 'High Use' },
  { key: 'GRID_VOLTAGE_SURGE', label: 'Grid Surge' },
];

export const ApplianceCard: React.FC<ApplianceCardProps> = ({
  reading,
  history,
  activeScenario,
  onInjectScenario,
  onOpenReset,
  onInspect,
}) => {
  const meta = getStateMeta(reading.state);
  const driftRate = getDriftRate(reading);
  const tempPct = Math.min(100, Math.max(0, ((reading.pinTempC - 25) / 55) * 100));
  const jitterPct = Math.min(100, Math.max(0, (reading.jitterIndex / 0.8) * 100));
  const isTripped = reading.state === 'TRIPPED_LATCHED' || reading.state === 'FAULT_HARDWARE';

  return (
    <div
      className={`group relative flex flex-col justify-between rounded-2xl border bg-slate-900/85 p-5 backdrop-blur-md transition-all duration-300 ${meta.border}`}
    >
      {/* Card Header */}
      <div>
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div
              className={`flex h-11 w-11 items-center justify-center rounded-xl border ${
                isTripped
                  ? 'border-rose-500/40 bg-rose-500/15 text-rose-300'
                  : 'border-slate-700/80 bg-slate-800/80 text-cyan-300'
              }`}
            >
              {APPLIANCE_ICON[reading.applianceType]}
            </div>
            <div>
              <h3 className="text-base font-semibold tracking-tight text-slate-100">
                {reading.displayName}
              </h3>
              <p className="font-mono text-[11px] text-slate-400">
                {reading.applianceId} · {reading.applianceType}
              </p>
            </div>
          </div>

          <div className="flex flex-col items-end gap-1.5">
            <span
              className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 font-mono text-[11px] font-semibold ${meta.badge}`}
            >
              {meta.pulse && (
                <span className="h-1.5 w-1.5 animate-ping rounded-full bg-current" />
              )}
              {meta.label}
            </span>
            <span
              className={`inline-flex items-center gap-1 font-mono text-[10px] ${
                reading.relayClosed ? 'text-emerald-400' : 'text-rose-400'
              }`}
            >
              <Power className="h-3 w-3" />
              {reading.relayClosed ? 'RELAY CLOSED' : 'RELAY OPEN'}
            </span>
          </div>
        </div>

        {/* Badges Row: Health, Fault Origin, High Today Caution */}
        <div className="mt-3 flex flex-wrap items-center gap-1.5">
          <span className="inline-flex items-center gap-1 rounded-md border border-slate-700/80 bg-slate-800/70 px-2 py-0.5 font-mono text-[11px] text-slate-200">
            {reading.healthScore >= 85 ? (
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
            ) : (
              <ShieldAlert className="h-3.5 w-3.5 text-amber-400" />
            )}
            Health {reading.healthScore}%
          </span>

          {reading.faultOrigin !== 'NONE' && (
            <span
              className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 font-mono text-[11px] font-medium ${
                reading.faultOrigin === 'GRID'
                  ? 'border-purple-500/50 bg-purple-500/15 text-purple-300'
                  : 'border-rose-500/50 bg-rose-500/15 text-rose-300'
              }`}
            >
              <AlertTriangle className="h-3 w-3" />
              {reading.faultOrigin === 'GRID' ? 'GRID BLAME' : 'APPLIANCE FAULT'}
            </span>
          )}

          {reading.highTodayCaution && (
            <span className="inline-flex items-center gap-1 rounded-md border border-amber-500/40 bg-amber-500/15 px-2 py-0.5 font-mono text-[11px] text-amber-300">
              <Zap className="h-3 w-3" />
              Trending High Today
            </span>
          )}
        </div>

        {/* Hero Power Reading + Sparkline */}
        <div className="mt-4 rounded-xl border border-slate-800/90 bg-slate-950/75 p-3">
          <div className="flex items-baseline justify-between">
            <div>
              <span className="text-[11px] uppercase tracking-wider text-slate-400">
                Live Load
              </span>
              <div className="flex items-baseline gap-1.5">
                <span className="font-mono text-2xl font-bold text-white">
                  {reading.powerW.toFixed(1)}
                </span>
                <span className="font-mono text-xs text-slate-400">W</span>
              </div>
            </div>
            <div className="text-right">
              <span className="text-[11px] text-slate-400">Today Cost</span>
              <div className="font-mono text-sm font-semibold text-emerald-300">
                ₹{reading.estimatedCostInr.toFixed(2)}
              </div>
            </div>
          </div>

          <div className="mt-2">
            <SparklineSvg
              values={history}
              strokeColor={meta.sparkStroke}
              fillColor={meta.sparkFill}
              height={36}
            />
          </div>
        </div>

        {/* Key Electrical Readings Grid */}
        <div className="mt-3 grid grid-cols-3 gap-2">
          <div className="rounded-lg border border-slate-800/80 bg-slate-950/50 px-2.5 py-2">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Voltage</div>
            <div
              className={`font-mono text-sm font-semibold ${
                reading.voltageV >= 252 ? 'text-purple-300' : 'text-slate-100'
              }`}
            >
              {reading.voltageV.toFixed(1)} V
            </div>
          </div>

          <div className="rounded-lg border border-slate-800/80 bg-slate-950/50 px-2.5 py-2">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Current</div>
            <div className="font-mono text-sm font-semibold text-slate-100">
              {reading.currentA.toFixed(2)} A
            </div>
          </div>

          <div className="rounded-lg border border-slate-800/80 bg-slate-950/50 px-2.5 py-2">
            <div className="text-[10px] uppercase tracking-wider text-slate-400">Power Factor</div>
            <div
              className={`font-mono text-sm font-semibold ${
                reading.powerFactor < 0.78 ? 'text-amber-300' : 'text-cyan-300'
              }`}
            >
              {reading.powerFactor.toFixed(2)}
            </div>
          </div>
        </div>

        {/* Thermal Pin Drift & Arc Jitter Bars */}
        <div className="mt-3 grid grid-cols-2 gap-2.5">
          <div className="rounded-lg border border-slate-800/80 bg-slate-950/50 p-2.5">
            <div className="flex items-center justify-between text-[10px] text-slate-400">
              <span className="inline-flex items-center gap-1">
                <Thermometer className="h-3 w-3 text-amber-400" />
                Pin Temp
              </span>
              <span className="font-mono text-slate-200">
                {reading.pinTempC.toFixed(1)}°C
              </span>
            </div>
            <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-slate-800">
              <div
                className={`h-full transition-all duration-300 ${
                  reading.pinTempC >= 70
                    ? 'bg-rose-500'
                    : reading.pinTempC >= 55
                    ? 'bg-amber-400'
                    : 'bg-emerald-400'
                }`}
                style={{ width: `${tempPct}%` }}
              />
            </div>
            <div className="mt-1 font-mono text-[10px] text-slate-400">
              dT/dt: {driftRate >= 0 ? '+' : ''}
              {driftRate.toFixed(2)}°C/m
            </div>
          </div>

          <div className="rounded-lg border border-slate-800/80 bg-slate-950/50 p-2.5">
            <div className="flex items-center justify-between text-[10px] text-slate-400">
              <span className="inline-flex items-center gap-1">
                <Activity className="h-3 w-3 text-cyan-400" />
                Arc Jitter
              </span>
              <span className="font-mono text-slate-200">
                {reading.jitterIndex.toFixed(3)}
              </span>
            </div>
            <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-slate-800">
              <div
                className={`h-full transition-all duration-300 ${
                  reading.jitterIndex >= 0.7
                    ? 'bg-rose-500'
                    : reading.jitterIndex > 0.35
                    ? 'bg-orange-400'
                    : 'bg-cyan-400'
                }`}
                style={{ width: `${jitterPct}%` }}
              />
            </div>
            <div className="mt-1 font-mono text-[10px] text-slate-400">
              Today: {reading.todayKwh.toFixed(2)} / {reading.p90Kwh.toFixed(2)} kWh
            </div>
          </div>
        </div>

        {/* Status Reason */}
        <p className="mt-2.5 truncate font-mono text-[11px] text-slate-400" title={reading.lastReason}>
          {reading.lastReason}
        </p>
      </div>

      {/* Footer Actions */}
      <div className="mt-4 border-t border-slate-800/80 pt-3">
        <div className="flex flex-wrap items-center gap-1">
          {SCENARIO_OPTIONS.map((opt) => {
            const selected = activeScenario === opt.key;
            return (
              <button
                key={opt.key}
                type="button"
                disabled={isTripped && opt.key !== 'NORMAL'}
                onClick={() => onInjectScenario(reading.applianceId, opt.key)}
                className={`rounded-md px-2 py-1 text-[10px] font-medium transition ${
                  selected
                    ? 'bg-cyan-500/20 text-cyan-300 ring-1 ring-cyan-500/50'
                    : 'bg-slate-800/80 text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                } disabled:cursor-not-allowed disabled:opacity-40`}
              >
                {opt.label}
              </button>
            );
          })}
        </div>

        <div className="mt-2.5 flex items-center gap-2">
          <button
            type="button"
            onClick={() => onInspect(reading)}
            className="flex-1 rounded-lg border border-slate-700 bg-slate-800/70 px-3 py-1.5 text-xs font-medium text-slate-200 transition hover:border-cyan-500/50 hover:bg-slate-800"
          >
            Baseline & Logs
          </button>

          {isTripped && (
            <button
              type="button"
              onClick={() => onOpenReset(reading)}
              className="flex-1 rounded-lg bg-rose-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-rose-500"
            >
              Inspection Reset
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
