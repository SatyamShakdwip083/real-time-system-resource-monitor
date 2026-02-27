/**
 * Types for WebSocket stats payload (matches backend SystemStats DTO).
 */
export interface CpuStats {
  name?: string;
  usagePercent: number;
  logicalProcessorCount: number;
  temperatureCelsius?: number | null;
}

export interface MemoryStats {
  totalBytes: number;
  usedBytes: number;
  availableBytes: number;
  usagePercent: number;
}

export interface GpuStats {
  usagePercent: number;
  name: string;
  vramUsedBytes: number;
  vramTotalBytes: number;
  temperatureCelsius?: number | null;
}

export interface DiskStats {
  readBytesPerSecond: number;
  writeBytesPerSecond: number;
  totalBytes: number;
  usedBytes: number;
  usagePercent: number;
}

export interface NetworkStats {
  downloadBytesPerSecond: number;
  uploadBytesPerSecond: number;
  totalBytesReceived: number;
  totalBytesSent: number;
}

export interface SystemStats {
  timestamp: number;
  cpu: CpuStats;
  memory: MemoryStats;
  gpu: GpuStats | null;
  gpus?: GpuStats[];
  disk: DiskStats;
  network: NetworkStats;
}
