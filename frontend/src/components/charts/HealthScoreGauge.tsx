import React from 'react';
import { FinancialHealthScore } from '../../types';
import { ShieldCheck, Info } from 'lucide-react';

interface HealthScoreGaugeProps {
  scoreData: FinancialHealthScore;
}

export const HealthScoreGauge: React.FC<HealthScoreGaugeProps> = ({ scoreData }) => {
  const { score, ratingGrade, evaluationSummary, components } = scoreData;

  // SVG Gauge calculations
  const radius = 55;
  const stroke = 10;
  const normalizedRadius = radius - stroke * 2;
  const circumference = normalizedRadius * 2 * Math.PI;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  const getScoreColor = (val: number) => {
    if (val >= 80) return '#10B981'; // Emerald
    if (val >= 60) return '#3B82F6'; // Blue
    if (val >= 40) return '#F59E0B'; // Amber
    return '#EF4444'; // Red
  };

  const scoreColor = getScoreColor(score);

  return (
    <div className="flex flex-col space-y-4">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-6 pb-4 border-b border-slate-100">
        {/* Radial Meter */}
        <div className="relative flex items-center justify-center flex-shrink-0">
          <svg height={radius * 2} width={radius * 2} className="transform -rotate-90">
            <circle
              stroke="#E2E8F0"
              fill="transparent"
              strokeWidth={stroke}
              r={normalizedRadius}
              cx={radius}
              cy={radius}
            />
            <circle
              stroke={scoreColor}
              fill="transparent"
              strokeWidth={stroke}
              strokeDasharray={circumference + ' ' + circumference}
              style={{ strokeDashoffset }}
              strokeLinecap="round"
              r={normalizedRadius}
              cx={radius}
              cy={radius}
              className="transition-all duration-1000 ease-out"
            />
          </svg>
          <div className="absolute flex flex-col items-center justify-center text-center">
            <span className="text-2xl font-black text-slate-900 leading-none">{score}</span>
            <span className="text-[10px] font-semibold text-slate-400 mt-0.5">/ 100</span>
          </div>
        </div>

        {/* Grade & Description */}
        <div className="flex-1 text-center sm:text-left">
          <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-50 text-emerald-700 border border-emerald-200 mb-1.5">
            <ShieldCheck className="w-3.5 h-3.5 mr-1 text-emerald-600" />
            {ratingGrade} Score
          </div>
          <p className="text-xs text-slate-600 leading-relaxed">{evaluationSummary}</p>
        </div>
      </div>

      {/* Component Breakdown Meters */}
      <div className="space-y-3">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
          Score Components Breakdown
        </span>
        {components.map((comp, idx) => {
          const pct = Math.round((comp.score / comp.maxScore) * 100);
          return (
            <div key={idx} className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-slate-700">{comp.componentName}</span>
                <span className="font-semibold text-slate-900">
                  {comp.score} / {comp.maxScore} pts
                </span>
              </div>
              <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  className="h-full bg-navy-900 rounded-full transition-all duration-500"
                  style={{ width: `${pct}%` }}
                />
              </div>
              <p className="text-[10px] text-slate-500">{comp.feedback}</p>
            </div>
          );
        })}
      </div>

      {/* Educational Notice */}
      <div className="flex items-start space-x-2 p-2.5 rounded-lg bg-slate-50 border border-slate-200/60 text-[11px] text-slate-500">
        <Info className="w-3.5 h-3.5 text-slate-400 mt-0.5 flex-shrink-0" />
        <span>
          Educational score based on personal cash flow habits. Not a credit score or regulated credit assessment.
        </span>
      </div>
    </div>
  );
};
