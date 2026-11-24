import type {
  ApplianceState,
  ApplianceType,
  FaultOrigin,
  SimulatedFaultType,
  TelemetryReading,
} from '../types/telemetry';

interface FallbackSpec {
  id: string;
  type: ApplianceType;
  name: string;
  ratedWatts: number;
  pf: number;
  expectedKwh: number;
  p90Kwh: number;
}

const SPECS: FallbackSpec[] = [
  { id: 'app-fan-01', type: 'FAN', name: 'Table Fan', ratedWatts: 75, pf: 0.85, expectedKwh: 0.52, p90Kwh: 0.78 },
  { id: 'app-ac-01', type: 'AC', name: 'Inverter AC', ratedWatts: 1500, pf: 0.95, expectedKwh: 5.4, p90Kwh: 7.2 },
  { id: 'app-fridge-01', type: 'FRIDGE', name: 'Refrigerator', ratedWatts: 180, pf: 0.82, expectedKwh: 1.15, p90Kwh: 1.55 },
  { id: 'app-geyser-01', type: 'GEYSER', name: 'Storage Geyser', ratedWatts: 2000, pf: 0.99, expectedKwh: 2.35, p90Kwh: 3.1 },
  { id: 'app-kettle-01', type: 'KETTLE', name: 'Electric Kettle', ratedWatts: 1800, pf: 1.0, expectedKwh: 0.28, p90Kwh: 0.45 },
  { id: 'app-tv-01', type: 'TV', name: 'Smart OLED TV', ratedWatts: 120, pf: 0.92, expectedKwh: 0.46, p90Kwh: 0.65 },
];

const scenarios = new Map<string, SimulatedFaultType>();
const kwhMap = new Map<string, number>();
const tempMap = new Map<string, number>();

export function setFallbackScenario(id: string, scenario: SimulatedFaultType) {
  scenarios.set(id, scenario);
}

export function resetFallbackAppliance(id: string) {
  scenarios.set(id, 'NORMAL');
  tempMap.set(id, 35.0);
}

export function generateFallbackTick(tick: number): TelemetryReading[] {
  const now = new Date().toISOString();
  const baseV = 230.2 + Math.sin(tick * 0.2) * 1.6;

  return SPECS.map((spec, idx) => {
    const scenario = scenarios.get(spec.id) ?? 'NORMAL';
    const prevTemp = tempMap.get(spec.id) ?? 34 + idx * 1.2;
    const prevKwh = kwhMap.get(spec.id) ?? spec.expectedKwh * 0.72;

    let powerW = spec.ratedWatts * (0.65 + 0.25 * Math.sin(tick * 0.18 + idx));
    if (spec.type === 'FRIDGE') {
      powerW = tick % 24 < 15 ? 165 : 14;
    } else if (spec.type === 'GEYSER') {
      powerW = tick % 30 < 18 ? 1920 : 12;
    } else if (spec.type === 'KETTLE') {
      powerW = tick % 28 < 10 ? 1760 : 5;
    }

    let voltageV = baseV;
    let pf = spec.pf;
    let jitter = 0.02 + Math.random() * 0.03;
    let pinTemp = prevTemp + (35 + (powerW / 2000) * 9 - prevTemp) * 0.15;
    let state: ApplianceState = 'ONLINE';
    let relayClosed = true;
    let faultOrigin: FaultOrigin = 'NONE';
    let health = 97;
    let reason = 'Offline simulation fallback — nominal envelope';

    if (scenario === 'LOOSE_SOCKET_ARCING') {
      jitter = 0.52 + Math.random() * 0.14;
      pf = Math.max(0.62, spec.pf - 0.2);
      state = 'WARNING_ARCING';
      faultOrigin = 'APPLIANCE';
      health = 54;
      reason = 'Simulated micro-arcing signature detected';
    } else if (scenario === 'THERMAL_PIN_DRIFT') {
      pinTemp = Math.min(73, prevTemp + 0.8);
      state = pinTemp >= 55 ? 'WARNING_DRIFT' : 'ONLINE';
      faultOrigin = pinTemp >= 55 ? 'APPLIANCE' : 'NONE';
      health = 62;
      reason = 'Simulated pin thermal drift dT/dt >= 2.0°C/min';
    } else if (scenario === 'GRID_VOLTAGE_SURGE') {
      voltageV = 267.5 + Math.random() * 5;
      state = 'WARNING_DRIFT';
      faultOrigin = 'GRID';
      health = 72;
      reason = 'Simulated utility over-voltage surge (Grid Fault)';
    } else if (scenario === 'HIGH_TODAY_USAGE') {
      powerW = spec.ratedWatts * 0.95;
      health = 84;
      reason = 'Trending High Today — exceeding p90 baseline';
    }

    const dTdt = Math.round((pinTemp - prevTemp) * 60 * 100) / 100;
    tempMap.set(spec.id, pinTemp);

    const kwh = prevKwh + powerW / 1000 / 3600 + (scenario === 'HIGH_TODAY_USAGE' ? 0.04 : 0);
    kwhMap.set(spec.id, kwh);

    const currentA = Math.round((powerW / (voltageV * pf)) * 100) / 100;

    return {
      applianceId: spec.id,
      applianceType: spec.type,
      displayName: spec.name,
      timestamp: now,
      voltageV: Math.round(voltageV * 100) / 100,
      currentA,
      powerW: Math.round(powerW * 10) / 10,
      powerFactor: Math.round(pf * 100) / 100,
      pinTempC: Math.round(pinTemp * 10) / 10,
      dtdtCPerMin: dTdt,
      jitterIndex: Math.round(jitter * 1000) / 1000,
      todayKwh: Math.round(kwh * 1000) / 1000,
      expectedKwh: spec.expectedKwh,
      p90Kwh: spec.p90Kwh,
      estimatedCostInr: Math.round(kwh * 8.25 * 100) / 100,
      highTodayCaution: kwh > spec.p90Kwh || scenario === 'HIGH_TODAY_USAGE',
      state,
      relayClosed,
      healthScore: health,
      faultOrigin,
      lastReason: reason,
    };
  });
}
