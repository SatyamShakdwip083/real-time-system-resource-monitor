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

const GPU_COLORS = [
  { border: 'rgb(168, 85, 247)', fill: 'rgba(168, 85, 247, 0.15)' },
  { border: 'rgb(236, 72, 153)', fill: 'rgba(236, 72, 153, 0.15)' },
  { border: 'rgb(34, 197, 94)', fill: 'rgba(34, 197, 94, 0.15)' },
];

/** Label for one GPU; disambiguates when multiple share the same name (e.g. "AMD Radeon(TM) Graphics (1)"). */
function gpuLabel(gpu, index, gpus, maxNameLen = 20) {
  const name = gpu?.name?.trim() || '';
  const shortName = name.slice(0, maxNameLen);
  const sameNameCount = gpus.filter((g) => (g?.name?.trim() || '') === name).length;
  if (sameNameCount > 1) return `${shortName} (${index + 1})`;
  return shortName || `GPU ${index + 1}`;
}

const options = (isDark, gpuCount) => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: gpuCount > 1 },
    tooltip: {
      callbacks: {
        label: (ctx) => `${ctx.dataset.label}: ${ctx.raw?.toFixed(1) ?? 0}%`,
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

export default function GpuChart({ onClick }) {
  const { stats, history } = useStats();
  const { dark: isDark } = useTheme();

  const gpus = stats.gpus?.length ? stats.gpus : (stats.gpu ? [stats.gpu] : []);

  const data = useMemo(() => {
    const sources = history?.length ? history : (stats ? [stats] : []);
    const labels = sources.map((_, i) => i.toString());
    const safeGpus = Array.isArray(gpus) ? gpus.filter(Boolean) : [];
    const datasets = safeGpus.length > 0
      ? safeGpus.map((gpu, idx) => ({
          label: gpuLabel(gpu, idx, safeGpus, 18),
          data: sources.map((s) => {
            const list = s?.gpus?.length ? s.gpus : (s?.gpu ? [s.gpu] : []);
            return list[idx]?.usagePercent ?? 0;
          }),
          borderColor: GPU_COLORS[idx % GPU_COLORS.length].border,
          backgroundColor: GPU_COLORS[idx % GPU_COLORS.length].fill,
          fill: true,
          tension: 0.3,
        }))
      : [
          {
            label: 'GPU %',
            data: sources.map((s) => s?.gpu?.usagePercent ?? 0),
            borderColor: GPU_COLORS[0].border,
            backgroundColor: GPU_COLORS[0].fill,
            fill: true,
            tension: 0.3,
          },
        ];
    return { labels: labels.length ? labels : ['0'], datasets };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- gpus derived from stats
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
        <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-200">
          GPU Usage{gpus.length > 1 ? ` (${gpus.length})` : ''}
        </h3>
        {onClick && (
          <span className="text-xs text-gray-400 dark:text-gray-500">Click for apps</span>
        )}
      </div>
      {gpus.length === 0 ? (
        <p className="text-2xl font-bold text-purple-500 dark:text-purple-400 mb-2">N/A</p>
      ) : (
        <>
          {gpus.map((gpu, idx) => (
            <div key={idx} className="mb-2 last:mb-0">
              <p className="text-lg font-bold text-purple-500 dark:text-purple-400">
                {gpu.usagePercent?.toFixed(1) ?? 0}%
                {gpus.length > 1 && (
                  <span className="text-sm font-normal text-gray-500 dark:text-gray-400 ml-1">
                    · {gpuLabel(gpu, idx, gpus, 28)}
                  </span>
                )}
              </p>
              {gpus.length === 1 && (
                <p className="text-sm text-gray-500 dark:text-gray-400 truncate" title={gpu.name}>
                  {gpu.name ?? 'N/A'}
                </p>
              )}
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Temp: {gpu.temperatureCelsius != null && gpu.temperatureCelsius > 0 ? `${gpu.temperatureCelsius} °C` : 'N/A'}
              </p>
            </div>
          ))}
        </>
      )}
      <div className="chart-container min-h-[200px]" style={{ minHeight: 200 }}>
        <Line data={data} options={options(isDark, gpus.length)} />
      </div>
    </div>
  );
}
