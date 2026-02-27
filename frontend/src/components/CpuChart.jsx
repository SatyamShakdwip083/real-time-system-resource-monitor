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
        label: (ctx) => `CPU: ${ctx.raw?.toFixed(1) ?? 0}%`,
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

export default function CpuChart({ onClick }) {
  const { stats, history } = useStats();
  const { dark: isDark } = useTheme();

  const data = useMemo(() => {
    const labels = (history.length ? history : [stats]).map((_, i) => i.toString());
    const values = (history.length ? history : [stats]).map((s) => s.cpu?.usagePercent ?? 0);
    return {
      labels,
      datasets: [
        {
          label: 'CPU %',
          data: values,
          borderColor: 'rgb(14, 165, 233)',
          backgroundColor: 'rgba(14, 165, 233, 0.15)',
          fill: true,
          tension: 0.3,
        },
      ],
    };
  }, [history, stats]);

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
      className={`bg-white dark:bg-gray-800 rounded-xl shadow-card dark:shadow-none border border-gray-200 dark:border-gray-700 p-4 ${onClick ? 'cursor-pointer hover:shadow-card-hover transition-shadow' : ''}`}
    >
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">CPU Usage</h3>
        {onClick && (
          <span className="text-xs text-gray-400 dark:text-gray-500">Click for apps</span>
        )}
      </div>
      <p className="text-2xl font-bold text-primary-500 dark:text-primary-400 mb-1">
        {stats.cpu?.usagePercent?.toFixed(1) ?? 0}%
      </p>
      {stats.cpu?.name && (
        <p className="text-sm text-gray-500 dark:text-gray-400 truncate mb-1" title={stats.cpu.name}>
          {stats.cpu.name}
        </p>
      )}
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-2" title="On Windows, run LibreHardwareMonitor for CPU temp">
        Temp: {stats.cpu?.temperatureCelsius != null && stats.cpu.temperatureCelsius > 0
          ? `${stats.cpu.temperatureCelsius} °C`
          : 'N/A'}
      </p>
      <div className="chart-container">
        <Line data={data} options={options(isDark)} />
      </div>
    </div>
  );
}
