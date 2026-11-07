import React from 'react';
import { FinancialHealthScore } from '../../types';
import { ShieldCheck, Info } from 'lucide-react';

interface HealthScoreGaugeProps {
  scoreData: FinancialHealthScore;
}

export const HealthScoreGauge: React.FC<HealthScoreGaugeProps> = ({ scoreData }) => {
  if (!scoreData) return null;

  const raw = scoreData as any;
  const score = Number(scoreData.score ?? raw.overallScore ?? 0);
  const ratingGrade = scoreData.ratingGrade ?? raw.ratingBand ?? 'GOOD';
  const evaluationSummary = scoreData.evaluationSummary ?? raw.summary ?? '';

  // Extract score components from array or individual component fields
  const componentsList: Array<{
    componentName: string;
    score: number;
    maxScore: number;
    feedback: string;
  }> = [];

  if (Array.isArray(scoreData.components) && scoreData.components.length > 0) {
    componentsList.push(...scoreData.components);
  } else {
    if (raw.savingsRateComponent) {
      componentsList.push({
        componentName: raw.savingsRateComponent.name || 'Savings Rate',
        score: Number(raw.savingsRateComponent.earnedScore ?? 0),
        maxScore: Number(raw.savingsRateComponent.maxScore ?? 40),
        feedback: raw.savingsRateComponent.recommendation || raw.savingsRateComponent.evaluation || '',
      });
    }
    if (raw.budgetAdherenceComponent) {
      componentsList.push({
        componentName: raw.budgetAdherenceComponent.name || 'Budget Adherence',
        score: Number(raw.budgetAdherenceComponent.earnedScore ?? 0),
        maxScore: Number(raw.budgetAdherenceComponent.maxScore ?? 35),
        feedback: raw.budgetAdherenceComponent.recommendation || raw.budgetAdherenceComponent.evaluation || '',
      });
    }
    if (raw.spendingConsistencyComponent) {
      componentsList.push({
        componentName: raw.spendingConsistencyComponent.name || 'Spending Consistency',
        score: Number(raw.spendingConsistencyComponent.earnedScore ?? 0),
        maxScore: Number(raw.spendingConsistencyComponent.maxScore ?? 25),
        feedback: raw.spendingConsistencyComponent.recommendation || raw.spendingConsistencyComponent.evaluation || '',
      });
    }
  }

  // SVG Gauge calculations
  const radius = 55;
  const stroke = 10;
  const normalizedRadius = radius - stroke * 2;
  const circumference = normalizedRadius * 2 * Math.PI;
  const strokeDashoffset = circumference - (Math.min(100, Math.max(0, score)) / 100) * circumference;

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
        {componentsList.length > 0 ? (
          componentsList.map((comp, idx) => {
            const max = comp.maxScore > 0 ? comp.maxScore : 100;
            const pct = Math.min(100, Math.max(0, Math.round(((comp.score ?? 0) / max) * 100)));
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
          })
        ) : (
          <p className="text-xs text-slate-400 italic">No component metrics available for this period.</p>
        )}
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
