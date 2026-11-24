import React, { useState } from 'react';
import { AlertOctagon, ShieldCheck, X } from 'lucide-react';
import type { TelemetryReading } from '../types/telemetry';

interface InspectionResetModalProps {
  reading: TelemetryReading | null;
  onClose: () => void;
  onConfirmReset: (applianceId: string, notes: string) => Promise<void>;
}

export const InspectionResetModal: React.FC<InspectionResetModalProps> = ({
  reading,
  onClose,
  onConfirmReset,
}) => {
  const [checkedSocket, setCheckedSocket] = useState(false);
  const [checkedPlug, setCheckedPlug] = useState(false);
  const [notes, setNotes] = useState('Inspected wall socket & plug pins; zero scorch or looseness');
  const [submitting, setSubmitting] = useState(false);

  if (!reading) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!checkedSocket || !checkedPlug) return;
    setSubmitting(true);
    try {
      await onConfirmReset(reading.applianceId, notes);
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg rounded-2xl border border-rose-500/50 bg-slate-900 p-6 shadow-2xl"
      >
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2.5 text-rose-400">
            <AlertOctagon className="h-6 w-6" />
            <div>
              <h2 className="text-base font-bold text-white">
                Safety Inspection Reset — {reading.displayName}
              </h2>
              <p className="font-mono text-xs text-rose-300/90">
                {reading.applianceId} · {reading.state}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-700 p-1.5 text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <p className="mt-3 rounded-lg border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-200">
          Relay was latched open (&lt;10ms reflex): {reading.lastReason}
        </p>

        <div className="mt-4 space-y-2.5 text-xs text-slate-200">
          <label className="flex cursor-pointer items-start gap-2.5 rounded-lg border border-slate-800 bg-slate-950/60 p-3">
            <input
              type="checkbox"
              checked={checkedSocket}
              onChange={(e) => setCheckedSocket(e.target.checked)}
              className="mt-0.5 accent-emerald-500"
            />
            <span>
              I have physically inspected the wall socket for loose contacts, arcing marks, or heat damage.
            </span>
          </label>

          <label className="flex cursor-pointer items-start gap-2.5 rounded-lg border border-slate-800 bg-slate-950/60 p-3">
            <input
              type="checkbox"
              checked={checkedPlug}
              onChange={(e) => setCheckedPlug(e.target.checked)}
              className="mt-0.5 accent-emerald-500"
            />
            <span>
              Plug pins have cooled below 45°C and post-trip leakage current check (&lt;0.1A) is authorized.
            </span>
          </label>
        </div>

        <div className="mt-3">
          <label className="block text-[11px] font-medium text-slate-400">
            Inspection Notes
          </label>
          <input
            type="text"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100 focus:border-cyan-500 focus:outline-none"
          />
        </div>

        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-700 px-4 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!checkedSocket || !checkedPlug || submitting}
            className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-xs font-semibold text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <ShieldCheck className="h-4 w-4" />
            {submitting ? 'Verifying & Re-latching...' : 'Verify & Re-latch Relay'}
          </button>
        </div>
      </form>
    </div>
  );
};
