import React from 'react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { formatINR } from '../../utils/formatters';

interface CategoryPieChartProps {
  data: Array<{
    categoryName: string;
    totalExpense: string | number;
    colorCode?: string;
  }>;
}

const DEFAULT_COLORS = ['#EF4444', '#F59E0B', '#F97316', '#3B82F6', '#8B5CF6', '#10B981', '#EC4899', '#6366F1'];

export const CategoryPieChart: React.FC<CategoryPieChartProps> = ({ data }) => {
  const chartData = data.map((d, index) => ({
    name: d.categoryName,
    value: typeof d.totalExpense === 'string' ? parseFloat(d.totalExpense) : d.totalExpense,
    color: d.colorCode || DEFAULT_COLORS[index % DEFAULT_COLORS.length],
  }));

  const total = chartData.reduce((sum, item) => sum + item.value, 0);

  return (
    <div className="w-full flex flex-col md:flex-row items-center justify-between gap-4">
      {/* Donut Chart */}
      <div className="w-full md:w-1/2 h-64 relative flex items-center justify-center">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={65}
              outerRadius={95}
              paddingAngle={3}
              dataKey="value"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} stroke="#FFFFFF" strokeWidth={2} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value: number) => [
                `${formatINR(value)} (${total > 0 ? ((value / total) * 100).toFixed(1) : 0}%)`,
                'Spent',
              ]}
              contentStyle={{
                backgroundColor: '#FFFFFF',
                borderRadius: '0.75rem',
                border: '1px solid #E2E8F0',
                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
                fontSize: '12px',
              }}
            />
          </PieChart>
        </ResponsiveContainer>
        {/* Center Label */}
        <div className="absolute flex flex-col items-center justify-center pointer-events-none">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Total Spent</span>
          <span className="text-sm font-bold text-slate-800">{formatINR(total)}</span>
        </div>
      </div>

      {/* Legend List */}
      <div className="w-full md:w-1/2 flex flex-col space-y-2 max-h-56 overflow-y-auto pr-1">
        {chartData.map((item, index) => {
          const pct = total > 0 ? ((item.value / total) * 100).toFixed(1) : 0;
          return (
            <div key={index} className="flex items-center justify-between text-xs py-1 px-2 rounded-lg hover:bg-slate-50 transition-colors">
              <div className="flex items-center space-x-2 truncate">
                <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: item.color }} />
                <span className="text-slate-700 font-medium truncate">{item.name}</span>
              </div>
              <div className="flex items-center space-x-2 text-right flex-shrink-0">
                <span className="font-semibold text-slate-900">{formatINR(item.value)}</span>
                <span className="text-slate-400 text-[10px] w-10">{pct}%</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
