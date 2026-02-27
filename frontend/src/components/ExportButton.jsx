import React from 'react';
import { useStats } from '../context/StatsContext';
import { exportStatsToCSV } from '../utils/exportCSV';

export default function ExportButton() {
  const { history } = useStats();

  const handleExport = () => {
    exportStatsToCSV(history);
  };

  return (
    <button
      type="button"
      onClick={handleExport}
      disabled={!history?.length}
      className="px-4 py-2 rounded-lg bg-primary-500 hover:bg-primary-600 dark:bg-primary-600 dark:hover:bg-primary-700 text-white font-medium shadow-card hover:shadow-card-hover transition disabled:opacity-50 disabled:cursor-not-allowed"
    >
      Export last 60s to CSV
    </button>
  );
}
