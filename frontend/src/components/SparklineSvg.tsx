import React from 'react';
import type { BaselineProfileDoc } from '../types/telemetry';

interface SparklineSvgProps {
  values: number[];
  strokeColor?: string;
  fillColor?: string;
  height?: number;
}

export const SparklineSvg: React.FC<SparklineSvgProps> = ({
  values,
  strokeColor = '#22d3ee',
  fillColor = 'rgba(34, 211, 238, 0.14)',
  height = 42,
}) => {
  const width = 220;
  const data = values.length > 1 ? values : [values[0] ?? 0, values[0] ?? 0];
  const min = Math.min(...data);
  const max = Math.max(...data);
  const span = max - min || 1;

  const points = data.map((val, idx) => {
    const x = (idx / (data.length - 1)) * width;
    const y = height - 4 - ((val - min) / span) * (height - 8);
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  });

  const polyline = points.join(' ');
  const areaPath = `M 0,${height} L ${points.join(' L ')} L ${width},${height} Z`;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="w-full overflow-visible"
      style={{ height }}
      preserveAspectRatio="none"
    >
      <path d={areaPath} fill={fillColor} />
      <polyline
        fill="none"
        stroke={strokeColor}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={polyline}
      />
    </svg>
  );
};

interface BaselineCurveChartProps {
  profiles: BaselineProfileDoc[];
  currentHour: number;
  liveWatts?: number;
}

export const BaselineCurveChart: React.FC<BaselineCurveChartProps> = ({
  profiles,
  currentHour,
  liveWatts,
}) => {
  if (profiles.length === 0) {
    return (
      <div className="flex h-44 items-center justify-center rounded-xl border border-slate-800 bg-slate-900/50 text-xs text-slate-400">
        No 24-hour baseline curve data available
      </div>
    );
  }

  const width = 560;
  const height = 180;
  const padX = 36;
  const padY = 18;
  const plotW = width - padX * 2;
  const plotH = height - padY * 2;

  const sorted = [...profiles].sort((a, b) => a.hour - b.hour);
  const maxW = Math.max(
    ...sorted.map((p) => p.expectedWatts),
    liveWatts ?? 0,
    10
  );

  const coords = sorted.map((p) => {
    const x = padX + (p.hour / 23) * plotW;
    const y = padY + plotH - (p.expectedWatts / maxW) * plotH;
    return { x, y, p };
  });

  const pathLine = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ');
  const area = `${pathLine} L ${coords[coords.length - 1].x.toFixed(1)},${padY + plotH} L ${coords[0].x.toFixed(1)},${padY + plotH} Z`;

  const activeCoord = coords.find((c) => c.p.hour === currentHour);
  const liveY = liveWatts !== undefined ? padY + plotH - (Math.min(liveWatts, maxW) / maxW) * plotH : null;

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-3">
      <div className="mb-2 flex items-center justify-between text-xs text-slate-400">
        <span>24h Trained Expected Power Envelope (W)</span>
        <span>Current Hour: {String(currentHour).padStart(2, '0')}:00</span>
      </div>
      <svg viewBox={`0 0 ${width} ${height}`} className="h-44 w-full">
        {/* Grid lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((frac) => {
          const y = padY + plotH - frac * plotH;
          return (
            <g key={frac}>
              <line x1={padX} y1={y} x2={width - padX} y2={y} stroke="#1e293b" strokeDasharray="3 3" />
              <text x={padX - 6} y={y + 3} textAnchor="end" className="fill-slate-500 text-[9px]">
                {Math.round(maxW * frac)}
              </text>
            </g>
          );
        })}

        {/* Baseline fill and line */}
        <path d={area} fill="rgba(56, 189, 248, 0.12)" />
        <path d={pathLine} fill="none" stroke="#38bdf8" strokeWidth="2.2" strokeLinecap="round" />

        {/* Current hour vertical marker */}
        {activeCoord && (
          <>
            <line
              x1={activeCoord.x}
              y1={padY}
              x2={activeCoord.x}
              y2={padY + plotH}
              stroke="#fbbf24"
              strokeDasharray="4 3"
              strokeWidth="1.2"
            />
            <circle cx={activeCoord.x} cy={activeCoord.y} r="4" fill="#38bdf8" stroke="#020617" strokeWidth="1.5" />
            {liveY !== null && (
              <circle cx={activeCoord.x} cy={liveY} r="4.5" fill="#10b981" stroke="#020617" strokeWidth="1.5" />
            )}
          </>
        )}

        {/* X axis ticks */}
        {[0, 6, 12, 18, 23].map((h) => {
          const x = padX + (h / 23) * plotW;
          return (
            <text key={h} x={x} y={height - 3} textAnchor="middle" className="fill-slate-500 text-[9px]">
              {String(h).padStart(2, '0')}h
            </text>
          );
        })}
      </svg>
    </div>
  );
};
