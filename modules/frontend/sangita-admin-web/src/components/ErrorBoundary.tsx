import React, { Component, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center h-screen bg-slate-50 p-8">
          <div className="max-w-2xl w-full bg-white rounded-xl border border-red-200 shadow-lg p-8">
            <div className="flex items-center gap-4 mb-6">
              <span className="material-symbols-outlined text-5xl text-red-500">error</span>
              <h1 className="text-2xl font-bold text-ink-900">Something went wrong</h1>
            </div>
            <div className="mb-6">
              <p className="text-ink-700 mb-4">
                The application encountered an error. Please check the browser console for details.
              </p>
              {this.state.error && (
                <div className="bg-slate-50 rounded-lg p-4 border border-slate-200">
                  <p className="text-sm font-mono text-red-600 break-all">
                    {this.state.error.toString()}
                  </p>
                  {this.state.error.stack && (
                    <details className="mt-2">
                      <summary className="text-xs text-ink-500 cursor-pointer">Stack trace</summary>
                      <pre className="mt-2 text-xs text-ink-600 overflow-auto max-h-64">
                        {this.state.error.stack}
                      </pre>
                    </details>
                  )}
                </div>
              )}
            </div>
            <div className="flex gap-4">
              <button
                onClick={() => window.location.reload()}
                className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
              >
                Reload Page
              </button>
              <button
                onClick={() => this.setState({ hasError: false, error: null })}
                className="px-4 py-2 bg-slate-100 text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-200 transition-colors"
              >
                Try Again
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;


