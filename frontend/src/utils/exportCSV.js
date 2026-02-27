/**
 * Exports the last 60 seconds of recorded stats (from history) into a downloadable CSV file.
 * @param {Array} history - Array of stats objects from StatsContext
 */
export function exportStatsToCSV(history) {
  if (!history || history.length === 0) {
    return;
  }

  const headers = [
    'timestamp',
    'cpu_usage_percent',
    'memory_usage_percent',
    'memory_used_bytes',
    'memory_total_bytes',
    'gpu_usage_percent',
    'gpu_name',
    'disk_usage_percent',
    'disk_read_bps',
    'disk_write_bps',
    'network_download_bps',
    'network_upload_bps',
  ];

  const rows = history.map((s) => {
    const c = s.cpu || {};
    const m = s.memory || {};
    const g = s.gpu || {};
    const d = s.disk || {};
    const n = s.network || {};
    return [
      s.timestamp,
      c.usagePercent ?? '',
      m.usagePercent ?? '',
      m.usedBytes ?? '',
      m.totalBytes ?? '',
      g.usagePercent ?? '',
      `"${(g.name || '').replace(/"/g, '""')}"`,
      d.usagePercent ?? '',
      d.readBytesPerSecond ?? '',
      d.writeBytesPerSecond ?? '',
      n.downloadBytesPerSecond ?? '',
      n.uploadBytesPerSecond ?? '',
    ].join(',');
  });

  const csv = [headers.join(','), ...rows].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `system-stats-${Date.now()}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
