import React, { Suspense, lazy } from 'react';
import { useStats } from './context/StatsContext';
import ThemeToggle from './components/ThemeToggle';
import ExportButton from './components/ExportButton';
import VersionFooter from './components/VersionFooter';

// Lazy-load dashboard (charts + WebSocket) so the shell renders even if that bundle fails
const Dashboard = lazy(() => import('./components/Dashboard'));

function AppShell() {
  const { connected, error } = useStats();
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100 transition-colors">
      <div className="p-4 md:p-6 lg:p-8">
        <header className="flex flex-wrap items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white">
              Real-Time System Resource Monitor
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Live CPU, RAM, GPU, Disk & Network metrics
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                connected
                  ? 'bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-200'
                  : 'bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-200'
              }`}
            >
              <span className={`w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-amber-500'} animate-pulse`} />
              {connected ? 'Connected' : 'Disconnected — reconnecting…'}
            </span>
            <ThemeToggle />
            <ExportButton />
          </div>
        </header>
        {error && !connected && (
          <p className="mb-4 text-sm text-amber-600 dark:text-amber-400">
            {error}. Ensure the backend is running on port 8081.
          </p>
        )}
        <Suspense fallback={<p className="text-gray-500 dark:text-gray-400">Loading charts…</p>}>
          <Dashboard />
        </Suspense>
        <VersionFooter />
      </div>
    </div>
  );
}

function App() {
  return <AppShell />;
}

export default App;
