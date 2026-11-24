export type ApplianceType = 'FAN' | 'AC' | 'FRIDGE' | 'GEYSER' | 'KETTLE' | 'TV';

export type ApplianceState =
  | 'LEARNING'
  | 'ONLINE'
  | 'WARNING_DRIFT'
  | 'WARNING_ARCING'
  | 'TRIPPED_LATCHED'
  | 'FAULT_HARDWARE';

export type FaultOrigin = 'NONE' | 'GRID' | 'APPLIANCE';

export type SimulatedFaultType =
  | 'NORMAL'
  | 'LOOSE_SOCKET_ARCING'
  | 'THERMAL_PIN_DRIFT'
  | 'HIGH_TODAY_USAGE'
  | 'GRID_VOLTAGE_SURGE';

export interface TelemetryReading {
  applianceId: string;
  applianceType: ApplianceType;
  displayName: string;
  timestamp: string;
  voltageV: number;
  currentA: number;
  powerW: number;
  powerFactor: number;
  pinTempC: number;
  dTdtCPerMin?: number;
  dtdtCPerMin?: number;
  jitterIndex: number;
  todayKwh: number;
  expectedKwh: number;
  p90Kwh: number;
  estimatedCostInr: number;
  highTodayCaution: boolean;
  state: ApplianceState;
  relayClosed: boolean;
  healthScore: number;
  faultOrigin: FaultOrigin;
  lastReason: string;
}

export interface ApplianceNode {
  id: string;
  applianceType: ApplianceType;
  displayName: string;
  ratedWatts: number;
  state: ApplianceState;
  relayClosed: boolean;
  healthScore: number;
  faultOrigin: FaultOrigin;
  lastReason: string;
  activeSimScenario: SimulatedFaultType;
  updatedAt: string;
}

export interface BaselineProfileDoc {
  id: string;
  applianceType: ApplianceType;
  hour: number;
  weekend?: boolean;
  isWeekend?: boolean;
  expectedWatts: number;
  expectedP50Kwh: number;
  expectedP90Kwh: number;
  nominalPowerFactor: number;
}

export interface FaultEventLog {
  id: string;
  applianceId: string;
  applianceType: ApplianceType;
  timestamp: string;
  faultType: string;
  faultOrigin: FaultOrigin;
  diagnosisMessage: string;
  resolved: boolean;
  resolvedAt?: string;
  resolutionNotes?: string;
}

export interface ApplianceDetailDto {
  appliance: ApplianceNode;
  latestTelemetry: TelemetryReading | null;
  recentHistory: TelemetryReading[];
  baselineProfile: BaselineProfileDoc[];
}

export interface ResetResultDto {
  success: boolean;
  applianceId: string;
  previousState: ApplianceState;
  currentState: ApplianceState;
  relayClosed: boolean;
  verifiedPostTripCurrentA: number;
  message: string;
  timestamp: string;
}

export interface TrainedModelSummary {
  activeSource: string;
  trainedRowsLoaded: number;
  trainedSeriesCount: number;
  cachedApplianceProfiles: number;
  applianceSeriesMapping: Record<string, string>;
}

export interface SimulatorStatus {
  running: boolean;
  intervalMs: number;
  kwhCostInr: number;
  activeWebSocketClients: number;
  applianceCount: number;
  trainedModel: TrainedModelSummary;
}

export function getDriftRate(reading: Pick<TelemetryReading, 'dTdtCPerMin' | 'dtdtCPerMin'>): number {
  return reading.dtdtCPerMin ?? reading.dTdtCPerMin ?? 0;
}
