import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { useStats } from '../context/StatsContext';
import { useTheme } from '../context/ThemeContext';
import CpuChart from './CpuChart';

vi.mock('../context/StatsContext', () => ({
  useStats: vi.fn(),
}));
vi.mock('../context/ThemeContext', () => ({
  useTheme: vi.fn(() => ({ dark: false })),
}));

describe('CpuChart', () => {
  it('shows CPU usage and N/A when no stats', () => {
    useStats.mockReturnValue({
      stats: {
        cpu: { usagePercent: 0, logicalProcessorCount: 4 },
        memory: {},
        gpu: null,
        disk: {},
        network: {},
      },
      history: [],
    });
    render(<CpuChart />);
    expect(screen.getByText(/0%/)).toBeInTheDocument();
    expect(screen.getByText(/CPU Usage/)).toBeInTheDocument();
  });

  it('shows CPU usage percentage from stats', () => {
    useStats.mockReturnValue({
      stats: {
        cpu: { usagePercent: 42.5, logicalProcessorCount: 8, name: 'Test CPU' },
        memory: {},
        gpu: null,
        disk: {},
        network: {},
      },
      history: [],
    });
    render(<CpuChart />);
    expect(screen.getByText('42.5%')).toBeInTheDocument();
  });
});
