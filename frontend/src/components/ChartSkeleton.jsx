/**
 * Placeholder card shown while loading or disconnected.
 */
export default function ChartSkeleton() {
  return (
    <div
      className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 animate-pulse"
      role="presentation"
    >
      <div className="h-6 bg-gray-200 dark:bg-gray-700 rounded w-1/3 mb-3" />
      <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/4 mb-2" />
      <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-full mb-2" />
      <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-2/3 mb-4" />
      <div className="h-32 bg-gray-200 dark:bg-gray-700 rounded w-full" />
    </div>
  );
}
