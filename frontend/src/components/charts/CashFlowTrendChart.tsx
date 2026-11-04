import React from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { formatCompactINR, formatINR } from '../../utils/formatters';

interface CashFlowTrendChartProps {
  data: Array<{
    displayLabel: string;
    totalSpending: string | number;
  }>;
}

export const CashFlowTrendChart: React.FC<CashFlowTrendChartProps> = ({ data }) => {
  const chartData = data.map((d) => ({
    name: d.displayLabel,
    Spending: typeof d.totalSpending === 'string' ? parseFloat(d.totalSpending) : d.totalSpending,
  }));

  return (
    <div className="w-full h-64">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
          <defs>
            <linearGradient id="spendingGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.2} />
              <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
          <XAxis
            dataKey="name"
            axisLine={false}
            tickLine={false}
            tick={{ fill: '#64748B', fontSize: 12 }}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{ fill: '#64748B', fontSize: 11 }}
            tickFormatter={(val) => formatCompactINR(val)}
          />
          <Tooltip
            formatter={(value: number) => [formatINR(value), 'Total Spent']}
            contentStyle={{
              backgroundColor: '#FFFFFF',
              borderRadius: '0.75rem',
              border: '1px solid #E2E8F0',
              boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              fontSize: '12px',
            }}
          />
          <Area
            type="monotone"
            dataKey="Spending"
            stroke="#2563EB"
            strokeWidth={2.5}
            fillOpacity={1}
            fill="url(#spendingGradient)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};
