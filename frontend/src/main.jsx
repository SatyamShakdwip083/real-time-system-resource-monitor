import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { StatsProvider } from './context/StatsContext';
import { ThemeProvider } from './context/ThemeContext';
import ErrorBoundary from './components/ErrorBoundary';
import './index.css';

const rootEl = document.getElementById('root');
if (!rootEl) {
  document.body.innerHTML = '<p style="padding:24px;font-family:sans-serif;">Root element not found.</p>';
} else {
  try {
    ReactDOM.createRoot(rootEl).render(
      <React.StrictMode>
        <ErrorBoundary>
          <ThemeProvider>
            <StatsProvider>
              <App />
            </StatsProvider>
          </ThemeProvider>
        </ErrorBoundary>
      </React.StrictMode>
    );
  } catch (err) {
    rootEl.innerHTML = '<p style="padding:24px;font-family:sans-serif;color:#b91c1c;">Failed to start app. Open DevTools (F12) and check the Console for errors.</p>';
    console.error(err);
  }
}
