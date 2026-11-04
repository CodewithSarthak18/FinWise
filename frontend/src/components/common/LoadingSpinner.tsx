import React from 'react';

export const LoadingSpinner: React.FC<{ message?: string }> = ({ message = 'Loading financial data...' }) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center">
      <div className="relative w-10 h-10">
        <div className="absolute inset-0 rounded-full border-2 border-slate-200"></div>
        <div className="absolute inset-0 rounded-full border-2 border-navy-900 border-t-transparent animate-spin"></div>
      </div>
      <p className="mt-4 text-xs font-medium text-slate-500">{message}</p>
    </div>
  );
};
