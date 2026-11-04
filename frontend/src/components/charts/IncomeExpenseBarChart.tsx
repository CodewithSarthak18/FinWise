import React from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { formatCompactINR, formatINR } from '../../utils/formatters';

interface IncomeExpenseBarChartProps {
  data: Array<{
    displayLabel: string;
    income: string | number;
    expense: string | number;
  }>;
}

export const IncomeExpenseBarChart: React.FC<IncomeExpenseBarChartProps> = ({ data }) => {
  const chartData = data.map((d) => ({
    name: d.displayLabel,
    Income: typeof d.income === 'string' ? parseFloat(d.income) : d.income,
    Expense: typeof d.expense === 'string' ? parseFloat(d.expense) : d.expense,
  }));

  return (
    <div className="w-full h-72">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }} barGap={6}>
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
            cursor={{ fill: '#F8FAFC' }}
            formatter={(value: number) => [formatINR(value), '']}
            contentStyle={{
              backgroundColor: '#FFFFFF',
              borderRadius: '0.75rem',
              border: '1px solid #E2E8F0',
              boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              fontSize: '12px',
            }}
          />
          <Legend
            iconType="circle"
            wrapperStyle={{ paddingTop: 10, fontSize: 12 }}
          />
          <Bar dataKey="Income" fill="#10B981" radius={[4, 4, 0, 0]} maxBarSize={36} />
          <Bar dataKey="Expense" fill="#EF4444" radius={[4, 4, 0, 0]} maxBarSize={36} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};
