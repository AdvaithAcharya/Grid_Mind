import type {
  ApplianceDetailDto,
  ApplianceNode,
  FaultEventLog,
  ResetResultDto,
  SimulatedFaultType,
  SimulatorStatus,
  TelemetryReading,
} from '../types/telemetry';

const API_BASE = 'http://localhost:8080/api';
export const WS_URL = 'ws://localhost:8080/ws/telemetry';

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
  }
  return res.json() as Promise<T>;
}

export const gridmindApi = {
  getAppliances: () => fetchJson<ApplianceNode[]>(`${API_BASE}/appliances`),

  getLatestTelemetryAll: () =>
    fetchJson<TelemetryReading[]>(`${API_BASE}/appliances/telemetry/latest`),

  getApplianceDetail: (id: string) =>
    fetchJson<ApplianceDetailDto>(`${API_BASE}/appliances/${encodeURIComponent(id)}`),

  getApplianceFaults: (id: string) =>
    fetchJson<FaultEventLog[]>(`${API_BASE}/appliances/${encodeURIComponent(id)}/faults`),

  injectScenario: (id: string, scenario: SimulatedFaultType, customNotes?: string) =>
    fetchJson<ApplianceNode>(`${API_BASE}/appliances/${encodeURIComponent(id)}/simulate`, {
      method: 'POST',
      body: JSON.stringify({ scenario, customNotes }),
    }),

  resetAppliance: (id: string, notes?: string) =>
    fetchJson<ResetResultDto>(`${API_BASE}/appliances/${encodeURIComponent(id)}/reset`, {
      method: 'POST',
      body: JSON.stringify({ notes }),
    }),

  getSimulatorStatus: () => fetchJson<SimulatorStatus>(`${API_BASE}/simulator/status`),

  toggleSimulator: (running?: boolean) =>
    fetchJson<SimulatorStatus>(`${API_BASE}/simulator/toggle`, {
      method: 'POST',
      body: JSON.stringify(running === undefined ? {} : { running }),
    }),

  resetAll: () =>
    fetchJson<SimulatorStatus>(`${API_BASE}/simulator/reset-all`, {
      method: 'POST',
    }),
};
