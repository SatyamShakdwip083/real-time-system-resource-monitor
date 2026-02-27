import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatsProvider, useStats } from '../context/StatsContext';

function TestConsumer() {
  const { stats, connected } = useStats();
  return (
    <div>
      <span data-testid="connected">{String(connected)}</span>
      <span data-testid="cpu">{stats.cpu?.usagePercent ?? 0}</span>
    </div>
  );
}

describe('StatsContext', () => {
  it('provides default stats and connected state', () => {
    render(
      <StatsProvider>
        <TestConsumer />
      </StatsProvider>
    );
    expect(screen.getByTestId('connected')).toHaveTextContent('false');
    expect(screen.getByTestId('cpu')).toHaveTextContent('0');
  });
});
