import React, { useState } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useStats } from '../context/StatsContext';
import CpuChart from './CpuChart';
import RamChart from './RamChart';
import GpuChart from './GpuChart';
import DiskChart from './DiskChart';
import NetworkChart from './NetworkChart';
import AlertBanner from './AlertBanner';
import ChartSkeleton from './ChartSkeleton';
import ProcessListModal from './ProcessListModal';

export default function Dashboard() {
  useWebSocket();
  const { connected, stats } = useStats();
  const [processModalResource, setProcessModalResource] = useState(null);
  const hasData = stats?.timestamp > 0;

  const openProcessModal = (resource) => setProcessModalResource(resource);
  const closeProcessModal = () => setProcessModalResource(null);

  const showSkeletons = !connected || !hasData;

  return (
    <>
      <AlertBanner />
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6 mt-6">
        {showSkeletons ? (
          <>
            {[1, 2, 3, 4, 5].map((i) => (
              <ChartSkeleton key={i} />
            ))}
          </>
        ) : (
          <>
            <CpuChart onClick={() => openProcessModal('cpu')} />
            <RamChart onClick={() => openProcessModal('memory')} />
            <GpuChart onClick={() => openProcessModal('gpu')} />
            <DiskChart onClick={() => openProcessModal('disk')} />
            <NetworkChart onClick={() => openProcessModal('network')} />
          </>
        )}
      </div>
      <ProcessListModal
        isOpen={!!processModalResource}
        onClose={closeProcessModal}
        resourceType={processModalResource}
      />
    </>
  );
}
