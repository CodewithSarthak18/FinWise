import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, LayoutDashboard } from 'lucide-react';
import { Button } from './Button';

interface Props {
  children: ReactNode;
  fallbackTitle?: string;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error caught by ErrorBoundary:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[280px] flex flex-col items-center justify-center p-6 text-center bg-white rounded-2xl border border-rose-200 shadow-sm m-4">
          <div className="w-12 h-12 rounded-full bg-rose-100 flex items-center justify-center text-rose-600 mb-4">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-slate-900 mb-1">
            {this.props.fallbackTitle || 'Something went wrong rendering this view'}
          </h3>
          <p className="text-xs text-slate-500 max-w-md mb-4 font-mono bg-slate-50 p-2 rounded border border-slate-200">
            {this.state.error?.message || 'An unexpected rendering error occurred. You can reload the view.'}
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Button
              size="sm"
              variant="outline"
              onClick={() => this.setState({ hasError: false, error: null })}
            >
              Try Again
            </Button>
            <Button
              size="sm"
              variant="secondary"
              onClick={() => (window.location.href = '/dashboard')}
            >
              <LayoutDashboard className="w-3.5 h-3.5 mr-1.5" />
              Go to Dashboard
            </Button>
            <Button
              size="sm"
              variant="primary"
              onClick={() => window.location.reload()}
            >
              <RefreshCw className="w-3.5 h-3.5 mr-1.5" />
              Reload Page
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
