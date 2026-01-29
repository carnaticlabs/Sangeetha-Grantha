import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

/**
 * Error Boundary to catch JavaScript errors anywhere in their child component tree,
 * log those errors, and display a fallback UI instead of the component tree that crashed.
 */
export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // You can also log the error to an error reporting service
    if (import.meta.env.DEV) {
      console.error("Uncaught error:", error, errorInfo);
    }
  }

  private handleReload = () => {
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex flex-col items-center justify-center h-[50vh] p-8 text-center bg-red-50 rounded-xl border border-red-100 m-4">
          <div className="bg-red-100 p-4 rounded-full mb-4">
            <span className="material-symbols-outlined text-red-600 text-3xl">error_outline</span>
          </div>
          <h2 className="text-xl font-bold text-red-900 mb-2">Something went wrong</h2>
          <p className="text-red-700 mb-6 max-w-md">
            The application encountered an unexpected error. Please try reloading the page.
          </p>
          {import.meta.env.DEV && this.state.error && (
            <div className="mb-6 w-full max-w-2xl bg-white p-4 rounded-lg border border-red-200 text-left overflow-auto max-h-48">
              <code className="text-xs text-red-800 font-mono whitespace-pre-wrap">
                {this.state.error.toString()}
              </code>
            </div>
          )}
          <button
            onClick={this.handleReload}
            className="px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors shadow-sm"
          >
            Reload Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
