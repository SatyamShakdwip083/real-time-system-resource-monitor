import React from 'react';
import { useStats } from '../context/StatsContext';

const CPU_THRESHOLD = 90;
const RAM_THRESHOLD = 80;
const GPU_THRESHOLD = 85;

export default function AlertBanner() {
  const { stats } = useStats();
  const cpu = stats.cpu?.usagePercent ?? 0;
  const ram = stats.memory?.usagePercent ?? 0;
  const gpu = stats.gpu?.usagePercent ?? 0;

  const alerts = [];
  if (cpu > CPU_THRESHOLD) alerts.push(`CPU usage critical: ${cpu.toFixed(1)}% (threshold ${CPU_THRESHOLD}%)`);
  if (ram > RAM_THRESHOLD) alerts.push(`RAM usage high: ${ram.toFixed(1)}% (threshold ${RAM_THRESHOLD}%)`);
  if (gpu > GPU_THRESHOLD) alerts.push(`GPU usage critical: ${gpu.toFixed(1)}% (threshold ${GPU_THRESHOLD}%)`);

  if (alerts.length === 0) return null;

  return (
    <div
      className="bg-red-500 text-white px-4 py-3 rounded-lg shadow-lg flex flex-wrap items-center justify-center gap-2 text-sm font-medium"
      role="alert"
    >
      <span className="inline-flex items-center">
        <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
        </svg>
        Performance alert
      </span>
      {alerts.map((msg, i) => (
        <span key={i} className="bg-red-600/80 px-2 py-1 rounded">
          {msg}
        </span>
      ))}
    </div>
  );
}
