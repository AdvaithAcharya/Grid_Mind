import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Activity,
  Cpu,
  Database,
  IndianRupee,
  Pause,
  Play,
  RotateCcw,
  ShieldAlert,
  Wifi,
  WifiOff,
  Zap,
} from 'lucide-react';
import { ApplianceCard } from './components/ApplianceCard';
import { ApplianceDetailModal } from './components/ApplianceDetailModal';
import { InspectionResetModal } from './components/InspectionResetModal';
import { gridmindApi, WS_URL } from './services/gridmindApi';
import {
  generateFallbackTick,
  resetFallbackAppliance,
  setFallbackScenario,
} from './services/offlineFallback';
import type {
  SimulatedFaultType,
  SimulatorStatus,
  TelemetryReading,
} from './types/telemetry';

type FilterMode = 'ALL' | 'ALERTS' | 'HIGH_USAGE';

export const App: React.FC = () => {
  const [readingsMap, setReadingsMap] = useState<Record<string, TelemetryReading>>({});
  const [historyMap, setHistoryMap] = useState<Record<string, number[]>>({});
  const [scenarioMap, setScenarioMap] = useState<Record<string, SimulatedFaultType>>({});
  const [wsConnected, setWsConnected] = useState(false);
  const [simStatus, setSimStatus] = useState<SimulatorStatus | null>(null);
  const [filter, setFilter] = useState<FilterMode>('ALL');
  const [inspectTarget, setInspectTarget] = useState<TelemetryReading | null>(null);
  const [resetTarget, setResetTarget] = useState<TelemetryReading | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const fallbackTickRef = useRef(0);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => {
      setToast((prev) => (prev === msg ? null : prev));
    }, 3500);
  };

  const ingestReading = (reading: TelemetryReading) => {
    setReadingsMap((prev) => ({
      ...prev,
      [reading.applianceId]: reading,
    }));
    setHistoryMap((prev) => {
      const existing = prev[reading.applianceId] ?? [];
      const next = [...existing, reading.powerW].slice(-30);
      return {
        ...prev,
        [reading.applianceId]: next,
      };
    });
  };

  // Initial snapshot + simulator status
  useEffect(() => {
    let mounted = true;
    const loadInitial = async () => {
      try {
        const [latest, status, nodes] = await Promise.all([
          gridmindApi.getLatestTelemetryAll(),
          gridmindApi.getSimulatorStatus(),
          gridmindApi.getAppliances(),
        ]);
        if (!mounted) return;
        latest.forEach(ingestReading);
        setSimStatus(status);
        const sMap: Record<string, SimulatedFaultType> = {};
        nodes.forEach((n) => {
          sMap[n.id] = n.activeSimScenario;
        });
        setScenarioMap(sMap);
      } catch {
        // Fallback tick effect handles offline demo automatically
      }
    };
    loadInitial();
    return () => {
      mounted = false;
    };
  }, []);

  // WebSocket live streaming with automatic reconnect
  useEffect(() => {
    let disposed = false;
    let reconnectTimer: number | undefined;

    const connect = () => {
      if (disposed) return;
      try {
        const ws = new WebSocket(WS_URL);
        wsRef.current = ws;

        ws.onopen = () => {
          if (disposed) return;
          setWsConnected(true);
          gridmindApi.getSimulatorStatus().then(setSimStatus).catch(() => {});
        };

        ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data && data.applianceId) {
              ingestReading(data as TelemetryReading);
            }
          } catch {
            // ignore non-telemetry pings
          }
        };

        ws.onclose = () => {
          if (disposed) return;
          setWsConnected(false);
          reconnectTimer = window.setTimeout(connect, 2500);
        };

        ws.onerror = () => {
          ws.close();
        };
      } catch {
        setWsConnected(false);
        reconnectTimer = window.setTimeout(connect, 2500);
      }
    };

    connect();

    return () => {
      disposed = true;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      wsRef.current?.close();
    };
  }, []);

  // Offline fallback loop when WebSocket is disconnected
  useEffect(() => {
    if (wsConnected) return;
    const interval = window.setInterval(() => {
      fallbackTickRef.current += 1;
      const batch = generateFallbackTick(fallbackTickRef.current);
      batch.forEach(ingestReading);
    }, 1000);
    return () => window.clearInterval(interval);
  }, [wsConnected]);

  const readings = useMemo(() => {
    const list = Object.values(readingsMap);
    return list.sort((a, b) => a.applianceId.localeCompare(b.applianceId));
  }, [readingsMap]);

  const filteredReadings = useMemo(() => {
    if (filter === 'ALERTS') {
      return readings.filter((r) => r.state !== 'ONLINE' && r.state !== 'LEARNING');
    }
    if (filter === 'HIGH_USAGE') {
      return readings.filter((r) => r.highTodayCaution);
    }
    return readings;
  }, [readings, filter]);

  const totals = useMemo(() => {
    return readings.reduce(
      (acc, r) => {
        acc.powerW += r.powerW;
        acc.todayKwh += r.todayKwh;
        acc.costInr += r.estimatedCostInr;
        if (r.state !== 'ONLINE' && r.state !== 'LEARNING') acc.alerts += 1;
        return acc;
      },
      { powerW: 0, todayKwh: 0, costInr: 0, alerts: 0 }
    );
  }, [readings]);

  const handleInjectScenario = async (id: string, scenario: SimulatedFaultType) => {
    setScenarioMap((prev) => ({ ...prev, [id]: scenario }));
    setFallbackScenario(id, scenario);
    try {
      await gridmindApi.injectScenario(id, scenario);
      showToast(`${id}: scenario set to ${scenario}`);
    } catch {
      showToast(`${id}: offline simulation scenario ${scenario}`);
    }
  };

  const handleConfirmReset = async (id: string, notes: string) => {
    resetFallbackAppliance(id);
    setScenarioMap((prev) => ({ ...prev, [id]: 'NORMAL' }));
    try {
      const res = await gridmindApi.resetAppliance(id, notes);
      showToast(res.message);
    } catch {
      showToast(`${id}: offline inspection reset completed`);
    }
  };

  const handleToggleStream = async () => {
    try {
      const next = await gridmindApi.toggleSimulator();
      setSimStatus(next);
      showToast(next.running ? 'Telemetry stream resumed' : 'Telemetry stream paused');
    } catch {
      showToast('Backend not reachable to toggle server stream');
    }
  };

  const handleResetAll = async () => {
    Object.keys(readingsMap).forEach((id) => resetFallbackAppliance(id));
    setScenarioMap({});
    try {
      const status = await gridmindApi.resetAll();
      setSimStatus(status);
      showToast('All appliance nodes reset to safe ONLINE operation');
    } catch {
      showToast('Offline nodes reset to NORMAL');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-cyan-950/30 via-slate-950 to-slate-950" />

      <div className="relative mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        {/* Top Command Header */}
        <header className="flex flex-col gap-4 rounded-2xl border border-slate-800/90 bg-slate-900/75 p-5 backdrop-blur-md lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/15 text-cyan-400 ring-1 ring-cyan-500/40">
                <Zap className="h-5 w-5" />
              </div>
              <h1 className="text-xl font-bold tracking-tight text-white">
                GRIDMIND
              </h1>
              <span className="rounded-md border border-slate-700 bg-slate-800 px-2 py-0.5 font-mono text-[11px] text-slate-300">
                Power, with a brain
              </span>

              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 font-mono text-[11px] font-medium ${
                  wsConnected
                    ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-300'
                    : 'border-amber-500/40 bg-amber-500/15 text-amber-300'
                }`}
              >
                {wsConnected ? (
                  <>
                    <Wifi className="h-3 w-3" />
                    LIVE WS 1Hz
                  </>
                ) : (
                  <>
                    <WifiOff className="h-3 w-3" />
                    OFFLINE SIM PREVIEW
                  </>
                )}
              </span>
            </div>

            <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-slate-400">
              <span className="inline-flex items-center gap-1">
                <Cpu className="h-3.5 w-3.5 text-cyan-400" />
                Trained Model:{' '}
                <strong className="font-mono text-slate-200">
                  {simStatus?.trainedModel?.trainedRowsLoaded
                    ? `${simStatus.trainedModel.trainedRowsLoaded} rows / ${simStatus.trainedModel.trainedSeriesCount} series`
                    : 'gridmind_baseline_profiles.json'}
                </strong>
              </span>
              <span className="inline-flex items-center gap-1">
                <Database className="h-3.5 w-3.5 text-emerald-400" />
                {readings.length} Appliance Nodes Active
              </span>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={handleToggleStream}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/80 px-3.5 py-2 text-xs font-medium text-slate-200 transition hover:border-cyan-500/50 hover:bg-slate-800"
            >
              {simStatus?.running === false ? (
                <>
                  <Play className="h-3.5 w-3.5 text-emerald-400" />
                  Resume Push
                </>
              ) : (
                <>
                  <Pause className="h-3.5 w-3.5 text-amber-400" />
                  Pause Push
                </>
              )}
            </button>

            <button
              type="button"
              onClick={handleResetAll}
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-700 bg-slate-800/80 px-3.5 py-2 text-xs font-medium text-slate-200 transition hover:border-emerald-500/50 hover:bg-slate-800"
            >
              <RotateCcw className="h-3.5 w-3.5 text-cyan-400" />
              Reset All Nodes
            </button>
          </div>
        </header>

        {/* Summary KPI Strip */}
        <section className="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
          <div className="rounded-xl border border-slate-800/90 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Total Home Load</span>
              <Activity className="h-4 w-4 text-cyan-400" />
            </div>
            <div className="mt-1 font-mono text-2xl font-bold text-white">
              {totals.powerW.toFixed(0)}{' '}
              <span className="text-sm font-normal text-slate-400">W</span>
            </div>
          </div>

          <div className="rounded-xl border border-slate-800/90 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Today Total Energy</span>
              <Zap className="h-4 w-4 text-emerald-400" />
            </div>
            <div className="mt-1 font-mono text-2xl font-bold text-white">
              {totals.todayKwh.toFixed(2)}{' '}
              <span className="text-sm font-normal text-slate-400">kWh</span>
            </div>
          </div>

          <div className="rounded-xl border border-slate-800/90 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Est. Daily Bill</span>
              <IndianRupee className="h-4 w-4 text-emerald-400" />
            </div>
            <div className="mt-1 font-mono text-2xl font-bold text-emerald-300">
              ₹{totals.costInr.toFixed(2)}
            </div>
          </div>

          <div className="rounded-xl border border-slate-800/90 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span>Active Hazard Alerts</span>
              <ShieldAlert
                className={`h-4 w-4 ${
                  totals.alerts > 0 ? 'text-rose-400' : 'text-emerald-400'
                }`}
              />
            </div>
            <div
              className={`mt-1 font-mono text-2xl font-bold ${
                totals.alerts > 0 ? 'text-rose-400' : 'text-emerald-400'
              }`}
            >
              {totals.alerts}
            </div>
          </div>
        </section>

        {/* Filter Bar */}
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            {(
              [
                { key: 'ALL', label: `All Appliances (${readings.length})` },
                { key: 'ALERTS', label: `Warnings / Tripped (${totals.alerts})` },
                {
                  key: 'HIGH_USAGE',
                  label: `Trending High (${readings.filter((r) => r.highTodayCaution).length})`,
                },
              ] as { key: FilterMode; label: string }[]
            ).map((tab) => (
              <button
                key={tab.key}
                type="button"
                onClick={() => setFilter(tab.key)}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                  filter === tab.key
                    ? 'bg-cyan-500/20 text-cyan-300 ring-1 ring-cyan-500/50'
                    : 'bg-slate-900 text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {!wsConnected && (
            <span className="font-mono text-[11px] text-amber-300/90">
              Waiting for Spring Boot WebSocket on ws://localhost:8080/ws/telemetry
            </span>
          )}
        </div>

        {/* Cards Visual Grid — One Card Per Appliance */}
        <main className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filteredReadings.map((reading) => (
            <ApplianceCard
              key={reading.applianceId}
              reading={reading}
              history={historyMap[reading.applianceId] ?? [reading.powerW]}
              activeScenario={scenarioMap[reading.applianceId] ?? 'NORMAL'}
              onInjectScenario={handleInjectScenario}
              onOpenReset={(r) => setResetTarget(r)}
              onInspect={(r) => setInspectTarget(r)}
            />
          ))}
        </main>
      </div>

      {/* Toast Notification */}
      {toast && (
        <div className="fixed bottom-5 right-5 z-50 rounded-xl border border-cyan-500/40 bg-slate-900/95 px-4 py-2.5 font-mono text-xs text-cyan-200 shadow-xl backdrop-blur">
          {toast}
        </div>
      )}

      {/* Detail & Trained Baseline Curve Modal */}
      <ApplianceDetailModal
        reading={inspectTarget ? readingsMap[inspectTarget.applianceId] ?? inspectTarget : null}
        onClose={() => setInspectTarget(null)}
      />

      {/* Safety Inspection Reset Modal */}
      <InspectionResetModal
        reading={resetTarget ? readingsMap[resetTarget.applianceId] ?? resetTarget : null}
        onClose={() => setResetTarget(null)}
        onConfirmReset={handleConfirmReset}
      />
    </div>
  );
};

export default App;
