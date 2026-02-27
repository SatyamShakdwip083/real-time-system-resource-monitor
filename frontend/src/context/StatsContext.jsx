/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useCallback } from 'react';

/**
 * Default shape for system stats (matches backend SystemStats DTO).
 */
const defaultStats = {
  timestamp: 0,
  cpu: { usagePercent: 0, logicalProcessorCount: 0 },
  memory: { totalBytes: 0, usedBytes: 0, availableBytes: 0, usagePercent: 0 },
  gpu: { usagePercent: 0, name: 'N/A', vramUsedBytes: 0, vramTotalBytes: 0 },
  disk: {
    readBytesPerSecond: 0,
    writeBytesPerSecond: 0,
    totalBytes: 0,
    usedBytes: 0,
    usagePercent: 0,
  },
  network: {
    downloadBytesPerSecond: 0,
    uploadBytesPerSecond: 0,
    totalBytesReceived: 0,
    totalBytesSent: 0,
  },
};

const StatsContext = createContext(null);

const MAX_HISTORY = 60; // last 60 seconds for CSV export

export function StatsProvider({ children }) {
  const [stats, setStats] = useState(defaultStats);
  const [history, setHistory] = useState([]);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);

  const updateStats = useCallback((newStats) => {
    setStats(newStats);
    setHistory((prev) => {
      const next = [...prev, { ...newStats }];
      if (next.length > MAX_HISTORY) return next.slice(-MAX_HISTORY);
      return next;
    });
    setError(null);
  }, []);

  const setConnectionState = useCallback((isConnected) => {
    setConnected(isConnected);
    if (!isConnected) setError('Disconnected from server');
  }, []);

  const setConnectionError = useCallback((err) => {
    setError(err);
  }, []);

  const value = {
    stats,
    history,
    connected,
    error,
    updateStats,
    setConnectionState,
    setConnectionError,
  };

  return (
    <StatsContext.Provider value={value}>
      {children}
    </StatsContext.Provider>
  );
}

export function useStats() {
  const ctx = useContext(StatsContext);
  if (!ctx) throw new Error('useStats must be used within StatsProvider');
  return ctx;
}
