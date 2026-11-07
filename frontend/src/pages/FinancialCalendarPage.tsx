import React, { useState, useEffect } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Clock,
  ArrowUpRight,
  ArrowDownRight,
  CheckCircle2,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Badge } from '../components/common/Badge';
import { recurringApi } from '../api/recurring';
import { transactionsApi } from '../api/transactions';
import { RecurringTransaction, Transaction, UpcomingReminder } from '../types';
import { formatINR, formatDate } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const FinancialCalendarPage: React.FC = () => {
  const [currentYear, setCurrentYear] = useState(2026);
  const [currentMonth, setCurrentMonth] = useState(10); // 0-indexed: 10 = November
  const [selectedDay, setSelectedDay] = useState(3);
  const [isLoading, setIsLoading] = useState(true);

  const [recurringList, setRecurringList] = useState<RecurringTransaction[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);

  useEffect(() => {
    const fetchCalendarData = async () => {
      setIsLoading(true);
      try {
        const [recRes, txRes] = await Promise.all([
          recurringApi.getRecurringTransactions(),
          transactionsApi.getTransactions({ size: 50 }),
        ]);
        setRecurringList(Array.isArray(recRes) ? recRes : []);
        const txList = Array.isArray(txRes?.content) ? txRes.content : Array.isArray(txRes) ? txRes : [];
        setTransactions(txList);
      } catch {
        setRecurringList([]);
        setTransactions([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchCalendarData();
  }, [currentYear, currentMonth]);

  // Calendar calculations
  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const firstDayOfWeek = new Date(currentYear, currentMonth, 1).getDay(); // 0 = Sun

  const handlePrevMonth = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear((y) => y - 1);
    } else {
      setCurrentMonth((m) => m - 1);
    }
  };

  const handleNextMonth = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear((y) => y + 1);
    } else {
      setCurrentMonth((m) => m + 1);
    }
  };

  // Find events for each day
  const getEventsForDay = (day: number) => {
    const formattedDay = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    const txsOnDay = transactions.filter((t) => t.transactionDate.startsWith(formattedDay));
    const billsOnDay = recurringList.filter((r) => r.nextExecutionDate.startsWith(formattedDay));

    return { txsOnDay, billsOnDay };
  };

  const selectedEvents = getEventsForDay(selectedDay);

  if (isLoading) {
    return <LoadingSpinner message="Mapping recurring schedules and cash flows to calendar grid..." />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Financial Calendar</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Visualize income credits, bill due dates, and SIP auto-debits plotted across the month.
          </p>
        </div>

        {/* Month Navigation */}
        <div className="flex items-center space-x-3 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-sm">
          <button
            onClick={handlePrevMonth}
            className="p-1 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-sm font-bold text-slate-800 min-w-32 text-center">
            {monthNames[currentMonth]} {currentYear}
          </span>
          <button
            onClick={handleNextMonth}
            className="p-1 rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Calendar Grid */}
        <Card className="lg:col-span-2 p-4 sm:p-6">
          {/* Day of Week Headers */}
          <div className="grid grid-cols-7 gap-1 text-center font-bold text-xs uppercase text-slate-400 pb-2 border-b border-slate-100">
            <span>Sun</span>
            <span>Mon</span>
            <span>Tue</span>
            <span>Wed</span>
            <span>Thu</span>
            <span>Fri</span>
            <span>Sat</span>
          </div>

          {/* Calendar Day Cells */}
          <div className="grid grid-cols-7 gap-1 mt-2">
            {/* Blank leading days */}
            {Array.from({ length: firstDayOfWeek }).map((_, idx) => (
              <div key={`blank-${idx}`} className="h-20 sm:h-24 p-1 rounded-lg bg-slate-50/40 opacity-40" />
            ))}

            {/* Days in Month */}
            {Array.from({ length: daysInMonth }).map((_, idx) => {
              const day = idx + 1;
              const isSelected = day === selectedDay;
              const { txsOnDay, billsOnDay } = getEventsForDay(day);
              const hasEvents = txsOnDay.length > 0 || billsOnDay.length > 0;

              return (
                <div
                  key={`day-${day}`}
                  onClick={() => setSelectedDay(day)}
                  className={`h-20 sm:h-24 p-1.5 rounded-xl border text-left cursor-pointer transition-all flex flex-col justify-between ${
                    isSelected
                      ? 'border-navy-900 bg-blue-50/40 ring-2 ring-navy-900/10'
                      : 'border-slate-200/70 hover:border-slate-300 hover:bg-slate-50/60 bg-white'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span
                      className={`text-xs font-bold ${
                        isSelected ? 'text-blue-700' : 'text-slate-700'
                      }`}
                    >
                      {day}
                    </span>
                    {hasEvents && (
                      <span className="w-1.5 h-1.5 rounded-full bg-blue-600" />
                    )}
                  </div>

                  <div className="space-y-0.5 overflow-hidden">
                    {billsOnDay.slice(0, 1).map((b) => (
                      <div
                        key={b.id}
                        className="truncate text-[9px] font-bold px-1 py-0.5 rounded bg-rose-50 text-rose-700 border border-rose-200/50"
                      >
                        Bill: {b.title}
                      </div>
                    ))}
                    {txsOnDay.slice(0, 1).map((t) => (
                      <div
                        key={t.id}
                        className={`truncate text-[9px] font-bold px-1 py-0.5 rounded ${
                          t.transactionType === 'INCOME'
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {formatINR(t.amount)}
                      </div>
                    ))}
                    {billsOnDay.length + txsOnDay.length > 2 && (
                      <span className="text-[9px] text-slate-400 font-semibold block text-center">
                        +{billsOnDay.length + txsOnDay.length - 2} more
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </Card>

        {/* Selected Day Agenda Drawer */}
        <Card className="h-full">
          <CardHeader>
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
                Selected Day Schedule
              </span>
              <CardTitle>
                {selectedDay} {monthNames[currentMonth]} {currentYear}
              </CardTitle>
            </div>
          </CardHeader>

          <div className="space-y-4">
            {/* Scheduled Bills Due On This Day */}
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block mb-2">
                Due Bills & Recurring Payments
              </span>
              {selectedEvents.billsOnDay.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No scheduled bills due on this day.</p>
              ) : (
                <div className="space-y-2">
                  {selectedEvents.billsOnDay.map((b) => (
                    <div
                      key={b.id}
                      className="p-3 rounded-xl border border-rose-200 bg-rose-50/50 flex items-center justify-between text-xs"
                    >
                      <div>
                        <p className="font-bold text-slate-900">{b.title}</p>
                        <p className="text-[11px] text-slate-500">{b.accountName}</p>
                      </div>
                      <span className="font-bold text-rose-600">{formatINR(b.amount)}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Transactions Logged On This Day */}
            <div className="pt-3 border-t border-slate-100">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block mb-2">
                Transactions Logged
              </span>
              {selectedEvents.txsOnDay.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No transactions recorded on this day.</p>
              ) : (
                <div className="space-y-2">
                  {selectedEvents.txsOnDay.map((t) => {
                    const isIncome = t.transactionType === 'INCOME';
                    return (
                      <div
                        key={t.id}
                        className="p-3 rounded-xl border border-slate-100 bg-slate-50 flex items-center justify-between text-xs"
                      >
                        <div>
                          <p className="font-bold text-slate-900">{t.merchantName || t.description}</p>
                          <p className="text-[11px] text-slate-500">
                            {t.categoryName} • {t.paymentMode}
                          </p>
                        </div>
                        <span className={`font-bold ${isIncome ? 'text-emerald-600' : 'text-slate-900'}`}>
                          {isIncome ? '+' : '-'} {formatINR(t.amount)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};
