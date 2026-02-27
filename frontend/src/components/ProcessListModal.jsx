import React, { useEffect, useState } from 'react';

const RESOURCE_LABELS = {
  cpu: 'CPU Usage',
  memory: 'RAM Usage',
  gpu: 'GPU Usage',
  disk: 'Disk',
  network: 'Network',
};

/** Only these resources have per-app data from the API; each uses its own sort. */
const RESOURCE_SORT = {
  cpu: 'cpu',
  memory: 'memory',
  disk: 'disk',
};

function formatBytes(bytes) {
  if (bytes >= 1e9) return `${(bytes / 1e9).toFixed(2)} GB`;
  if (bytes >= 1e6) return `${(bytes / 1e6).toFixed(2)} MB`;
  if (bytes >= 1e3) return `${(bytes / 1e3).toFixed(2)} KB`;
  return `${bytes} B`;
}

export default function ProcessListModal({ isOpen, onClose, resourceType }) {
  const [processes, setProcesses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sort = RESOURCE_SORT[resourceType];
  const label = RESOURCE_LABELS[resourceType] || resourceType;
  const hasPerAppData = sort != null;
  const unavailableMessage =
    resourceType === 'gpu'
      ? 'Per-application GPU usage is not available on this system.'
      : resourceType === 'network'
        ? 'Per-application network usage is not available on this system.'
        : null;

  useEffect(() => {
    if (!isOpen || !resourceType) return;
    if (!hasPerAppData) {
      setProcesses([]);
      setLoading(false);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    const apiBase = import.meta.env.VITE_API_URL
      || (typeof window !== 'undefined' && window.location.port === '3000' ? 'http://localhost:8081' : '');
    const url = `${apiBase}/api/processes?sort=${sort}&limit=25`;
    fetch(url)
      .then((res) => {
        if (!res.ok) {
          const msg = res.status === 404
            ? 'Process list not available. Restart the backend (mvn spring-boot:run) and try again.'
            : res.statusText || 'Request failed';
          throw new Error(msg);
        }
        return res.json();
      })
      .then((data) => {
        setProcesses(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || 'Failed to load processes');
        setProcesses([]);
        setLoading(false);
      });
  }, [isOpen, resourceType, sort, hasPerAppData]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 dark:bg-black/70"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="process-modal-title"
    >
      <div
        className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-2xl w-full max-h-[85vh] flex flex-col border border-gray-200 dark:border-gray-700"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 id="process-modal-title" className="text-xl font-semibold text-gray-900 dark:text-white">
            Applications using {label}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400"
            aria-label="Close"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </button>
        </div>
        {unavailableMessage && (
          <div className="px-4 py-6 text-center">
            <p className="text-gray-600 dark:text-gray-400">{unavailableMessage}</p>
          </div>
        )}
        {hasPerAppData && (
          <>
            <div className="overflow-auto flex-1 p-4">
              {loading && (
                <p className="text-gray-500 dark:text-gray-400 text-center py-8">Loading…</p>
              )}
              {error && (
                <p className="text-red-600 dark:text-red-400 text-center py-8">{error}</p>
              )}
              {!loading && !error && processes.length === 0 && (
                <p className="text-gray-500 dark:text-gray-400 text-center py-8">No processes found.</p>
              )}
              {!loading && !error && processes.length > 0 && resourceType === 'cpu' && (
                <table className="w-full text-sm text-left text-gray-700 dark:text-gray-300">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-600">
                      <th className="py-2 pr-4 font-semibold">Name</th>
                      <th className="py-2 pr-4 font-semibold w-20">PID</th>
                      <th className="py-2 pr-4 font-semibold w-24">CPU %</th>
                      <th className="py-2 font-semibold">Memory</th>
                    </tr>
                  </thead>
                  <tbody>
                    {processes.map((proc) => (
                      <tr key={proc.pid} className="border-b border-gray-100 dark:border-gray-700">
                        <td className="py-2 pr-4 truncate max-w-[200px]" title={proc.name}>{proc.name}</td>
                        <td className="py-2 pr-4">{proc.pid}</td>
                        <td className="py-2 pr-4 font-medium">{proc.cpuPercent?.toFixed(1) ?? 0}%</td>
                        <td className="py-2">{formatBytes(proc.memoryBytes ?? 0)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {!loading && !error && processes.length > 0 && resourceType === 'memory' && (
                <table className="w-full text-sm text-left text-gray-700 dark:text-gray-300">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-600">
                      <th className="py-2 pr-4 font-semibold">Name</th>
                      <th className="py-2 pr-4 font-semibold w-20">PID</th>
                      <th className="py-2 pr-4 font-semibold">Memory</th>
                      <th className="py-2 font-semibold w-24">CPU %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {processes.map((proc) => (
                      <tr key={proc.pid} className="border-b border-gray-100 dark:border-gray-700">
                        <td className="py-2 pr-4 truncate max-w-[200px]" title={proc.name}>{proc.name}</td>
                        <td className="py-2 pr-4">{proc.pid}</td>
                        <td className="py-2 pr-4 font-medium">{formatBytes(proc.memoryBytes ?? 0)}</td>
                        <td className="py-2">{proc.cpuPercent?.toFixed(1) ?? 0}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {!loading && !error && processes.length > 0 && resourceType === 'disk' && (
                <table className="w-full text-sm text-left text-gray-700 dark:text-gray-300">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-600">
                      <th className="py-2 pr-4 font-semibold">Name</th>
                      <th className="py-2 pr-4 font-semibold w-20">PID</th>
                      <th className="py-2 pr-4 font-semibold">Read</th>
                      <th className="py-2 font-semibold">Written</th>
                    </tr>
                  </thead>
                  <tbody>
                    {processes.map((proc) => (
                      <tr key={proc.pid} className="border-b border-gray-100 dark:border-gray-700">
                        <td className="py-2 pr-4 truncate max-w-[200px]" title={proc.name}>{proc.name}</td>
                        <td className="py-2 pr-4">{proc.pid}</td>
                        <td className="py-2 pr-4 font-medium">{formatBytes(proc.diskReadBytes ?? 0)}</td>
                        <td className="py-2 font-medium">{formatBytes(proc.diskWriteBytes ?? 0)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
