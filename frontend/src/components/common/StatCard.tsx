import React from 'react';
import { LucideIcon, TrendingUp, TrendingDown } from 'lucide-react';
import { cn } from '../../utils/cn';

interface StatCardProps {
  title: string;
  value: string;
  subtitle?: string;
  trend?: {
    value: string;
    isPositive: boolean;
    label?: string;
  };
  icon: LucideIcon;
  variant?: 'default' | 'income' | 'expense' | 'savings' | 'health';
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  trend,
  icon: Icon,
  variant = 'default',
  className,
}) => {
  const iconVariants = {
    default: 'bg-slate-100 text-slate-700',
    income: 'bg-emerald-50 text-emerald-600',
    expense: 'bg-rose-50 text-rose-600',
    savings: 'bg-blue-50 text-blue-600',
    health: 'bg-purple-50 text-purple-600',
  };

  return (
    <div
      className={cn(
        'bg-white rounded-xl border border-slate-200/80 p-5 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden',
        className
      )}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{title}</p>
          <h4 className="text-2xl font-bold text-slate-900 mt-1 tracking-tight">{value}</h4>
        </div>
        <div className={cn('p-2.5 rounded-xl flex items-center justify-center', iconVariants[variant])}>
          <Icon className="w-5 h-5" />
        </div>
      </div>

      {(trend || subtitle) && (
        <div className="mt-3 pt-3 border-t border-slate-100 flex items-center text-xs">
          {trend && (
            <span
              className={cn(
                'inline-flex items-center font-semibold mr-2',
                trend.isPositive ? 'text-emerald-600' : 'text-rose-600'
              )}
            >
              {trend.isPositive ? (
                <TrendingUp className="w-3.5 h-3.5 mr-1" />
              ) : (
                <TrendingDown className="w-3.5 h-3.5 mr-1" />
              )}
              {trend.value}
            </span>
          )}
          <span className="text-slate-500 truncate">{trend?.label || subtitle}</span>
        </div>
      )}
    </div>
  );
};
