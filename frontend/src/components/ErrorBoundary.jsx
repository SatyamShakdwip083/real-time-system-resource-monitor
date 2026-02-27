import React from 'react';

/**
 * Catches React errors so the app shows a message instead of a blank screen.
 */
export default class ErrorBoundary extends React.Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="p-6 max-w-xl mx-auto text-gray-900 dark:text-gray-100"
          role="alert"
          aria-live="assertive"
        >
          <h2 className="text-xl font-semibold text-red-600 dark:text-red-400 mb-2">
            Something went wrong
          </h2>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            {this.state.error?.message || 'An error occurred. The rest of the app may still work.'}
          </p>
          <button
            type="button"
            onClick={() => this.setState({ hasError: false, error: null })}
            className="px-4 py-2 rounded-lg bg-primary-500 hover:bg-primary-600 text-white font-medium"
          >
            Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
