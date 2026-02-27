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

const options = (isDark) => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx) => `RAM: ${ctx.raw?.toFixed(1) ?? 0}%`,
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
      max: 100,
      grid: { color: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)' },
      ticks: { color: isDark ? '#9ca3af' : '#6b7280' },
    },
  },
});

export default function RamChart({ onClick }) {
  const { stats, history } = useStats();
  const { dark: isDark } = useTheme();

  const data = useMemo(() => {
    const labels = (history.length ? history : [stats]).map((_, i) => i.toString());
    const values = (history.length ? history : [stats]).map((s) => s.memory?.usagePercent ?? 0);
    return {
      labels,
      datasets: [
        {
          label: 'RAM %',
          data: values,
          borderColor: 'rgb(34, 197, 94)',
          backgroundColor: 'rgba(34, 197, 94, 0.15)',
          fill: true,
          tension: 0.3,
        },
      ],
    };
  }, [history, stats]);

  const formatBytes = (bytes) => {
    if (bytes >= 1e9) return `${(bytes / 1e9).toFixed(2)} GB`;
    if (bytes >= 1e6) return `${(bytes / 1e6).toFixed(2)} MB`;
    return `${(bytes / 1e3).toFixed(2)} KB`;
  };

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
      className={`bg-white dark:bg-gray-800 rounded-xl shadow-card dark:shadow-none border border-gray-200 dark:border-gray-700 p-4 ${onClick ? 'cursor-pointer hover:shadow-card-hover transition-shadow' : ''}`}
    >
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">RAM Usage</h3>
        {onClick && (
          <span className="text-xs text-gray-400 dark:text-gray-500">Click for apps</span>
        )}
      </div>
      <p className="text-2xl font-bold text-green-500 dark:text-green-400 mb-1">
        {stats.memory?.usagePercent?.toFixed(1) ?? 0}%
      </p>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
        {formatBytes(stats.memory?.usedBytes ?? 0)} / {formatBytes(stats.memory?.totalBytes ?? 0)}
      </p>
      <div className="chart-container">
        <Line data={data} options={options(isDark)} />
      </div>
    </div>
  );
}
