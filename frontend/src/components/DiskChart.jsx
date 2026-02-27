import React, { useMemo } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line } from 'react-chartjs-2';
import { useStats } from '../context/StatsContext';
import { useTheme } from '../context/ThemeContext';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

const formatBps = (bps) => {
  if (bps >= 1e9) return `${(bps / 1e9).toFixed(2)} GB/s`;
  if (bps >= 1e6) return `${(bps / 1e6).toFixed(2)} MB/s`;
  if (bps >= 1e3) return `${(bps / 1e3).toFixed(2)} KB/s`;
  return `${bps} B/s`;
};

const options = (isDark) => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: true, position: 'top' },
    tooltip: {
      callbacks: {
        label: (ctx) => {
          const v = ctx.raw;
          return `${ctx.dataset.label}: ${formatBps(v)}`;
        },
      },
    },
  },
  scales: {
    x: {
      grid: { color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' },
      ticks: { maxTicksLimit: 6, color: isDark ? '#9ca3af' : '#6b7280' },
    },
    y: {
      min: 0,
      grid: { color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' },
      ticks: { color: isDark ? '#9ca3af' : '#6b7280', callback: (v) => formatBps(v) },
    },
  },
});

export default function DiskChart({ onClick }) {
  const { stats, history } = useStats();
  const { dark: isDark } = useTheme();

  const data = useMemo(() => {
    const labels = (history.length ? history : [stats]).map((_, i) => i.toString());
    const read = (history.length ? history : [stats]).map((s) => s.disk?.readBytesPerSecond ?? 0);
    const write = (history.length ? history : [stats]).map((s) => s.disk?.writeBytesPerSecond ?? 0);
    return {
      labels,
      datasets: [
        {
          label: 'Read',
          data: read,
          borderColor: 'rgb(234, 179, 8)',
          backgroundColor: 'rgba(234, 179, 8, 0.1)',
          fill: true,
          tension: 0.3,
        },
        {
          label: 'Write',
          data: write,
          borderColor: 'rgb(249, 115, 22)',
          backgroundColor: 'rgba(249, 115, 22, 0.1)',
          fill: true,
          tension: 0.3,
        },
      ],
    };
  }, [history, stats]);

  const totalBytes = stats.disk?.totalBytes ?? 0;
  const usedBytes = stats.disk?.usedBytes ?? 0;
  const formatBytes = (b) => (b >= 1e9 ? `${(b / 1e9).toFixed(2)} GB` : b >= 1e6 ? `${(b / 1e6).toFixed(2)} MB` : `${(b / 1e3).toFixed(2)} KB`);

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
      className={`bg-white dark:bg-gray-800 rounded-xl shadow-card dark:shadow-none border border-gray-200 dark:border-gray-700 p-4 ${onClick ? 'cursor-pointer hover:shadow-card-hover transition-shadow' : ''}`}
    >
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">Disk</h3>
        {onClick && (
          <span className="text-xs text-gray-400 dark:text-gray-500">Click for apps</span>
        )}
      </div>
      <p className="text-2xl font-bold text-amber-500 dark:text-amber-400 mb-1">
        {stats.disk?.usagePercent?.toFixed(1) ?? 0}% used
      </p>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">
        {formatBytes(usedBytes)} / {formatBytes(totalBytes)}
      </p>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
        Read: {formatBps(stats.disk?.readBytesPerSecond ?? 0)} · Write: {formatBps(stats.disk?.writeBytesPerSecond ?? 0)}
      </p>
      <div className="chart-container">
        <Line data={data} options={options(isDark)} />
      </div>
    </div>
  );
}
