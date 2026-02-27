import { useState, useEffect } from 'react';

const apiBase = import.meta.env.VITE_API_BASE || '';

export default function VersionFooter() {
  const [info, setInfo] = useState(null);

  useEffect(() => {
    fetch(`${apiBase}/api/info`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setInfo(data))
      .catch(() => setInfo(null));
  }, []);

  if (!info?.version) return null;
  return (
    <footer className="mt-8 pt-4 border-t border-gray-200 dark:border-gray-700 text-center text-xs text-gray-500 dark:text-gray-400">
      Backend v{info.version}
    </footer>
  );
}
