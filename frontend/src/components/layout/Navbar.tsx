import React, { useState, useEffect, useRef } from 'react';
import { Menu, Bell, Plus, ArrowRightLeft, Check, ExternalLink } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../common/Button';
import { notificationsApi } from '../../api/notifications';
import { InAppNotification } from '../../types';
import { useNavigate } from 'react-router-dom';

interface NavbarProps {
  onMenuClick: () => void;
  onOpenAddTransaction: () => void;
  onOpenTransfer: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  onMenuClick,
  onOpenAddTransaction,
  onOpenTransfer,
}) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const notificationRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        const data = await notificationsApi.getNotifications();
        setNotifications(Array.isArray(data) ? data : []);
      } catch {
        setNotifications([]);
      }
    };
    fetchNotifications();

    const handleClickOutside = (event: MouseEvent) => {
      if (notificationRef.current && !notificationRef.current.contains(event.target as Node)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const safeNotifications = Array.isArray(notifications) ? notifications : [];
  const unreadCount = safeNotifications.filter((n) => !n.isRead).length;

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationsApi.markAsRead(id);
      setNotifications((prev) => (Array.isArray(prev) ? prev.map((n) => (n.id === id ? { ...n, isRead: true } : n)) : []));
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationsApi.markAllAsRead();
      setNotifications((prev) => (Array.isArray(prev) ? prev.map((n) => ({ ...n, isRead: true })) : []));
    } catch {
      // ignore
    }
  };

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between h-16 px-4 md:px-8 bg-white/90 backdrop-blur-md border-b border-slate-200/80">
      {/* Left: Mobile Menu Button & Search */}
      <div className="flex items-center space-x-3 md:space-x-4">
        <button
          onClick={onMenuClick}
          className="p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 lg:hidden focus:outline-none"
          aria-label="Toggle navigation"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:block">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Welcome Back,</span>
          <h2 className="text-sm font-bold text-slate-800 -mt-0.5">
            {user ? `${user.firstName} ${user.lastName}` : 'FinWise User'}
          </h2>
        </div>
      </div>

      {/* Right: Quick Actions, Notifications & Avatar */}
      <div className="flex items-center space-x-2.5 sm:space-x-4">
        {/* Quick Action: Transfer */}
        <Button
          size="sm"
          variant="outline"
          onClick={onOpenTransfer}
          className="hidden md:inline-flex text-xs font-medium"
        >
          <ArrowRightLeft className="w-3.5 h-3.5 mr-1 text-slate-500" />
          Transfer
        </Button>

        {/* Quick Action: Add Transaction */}
        <Button
          size="sm"
          variant="emerald"
          onClick={onOpenAddTransaction}
          className="text-xs font-medium"
        >
          <Plus className="w-4 h-4 mr-1" />
          <span className="hidden sm:inline">Record</span> Transaction
        </Button>

        {/* Notifications Dropdown */}
        <div className="relative" ref={notificationRef}>
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors focus:outline-none"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-rose-500 rounded-full ring-2 ring-white animate-pulse" />
            )}
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-slate-200 overflow-hidden z-50 animate-in fade-in zoom-in-95">
              <div className="flex items-center justify-between px-4 py-3 bg-slate-50 border-b border-slate-100">
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-800">Notifications</span>
                  {unreadCount > 0 && (
                    <span className="px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-rose-100 text-rose-700">
                      {unreadCount} new
                    </span>
                  )}
                </div>
                {unreadCount > 0 && (
                  <button
                    onClick={handleMarkAllRead}
                    className="text-[11px] font-medium text-blue-600 hover:text-blue-800 flex items-center"
                  >
                    <Check className="w-3 h-3 mr-1" /> Mark all read
                  </button>
                )}
              </div>

              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                {safeNotifications.length === 0 ? (
                  <div className="py-6 text-center text-xs text-slate-500">No notifications at this time.</div>
                ) : (
                  safeNotifications.map((n) => (
                    <div
                      key={n.id}
                      className={`p-3.5 transition-colors flex items-start space-x-3 ${
                        n.isRead ? 'bg-white' : 'bg-blue-50/40 hover:bg-blue-50/70'
                      }`}
                    >
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <p className={`text-xs font-semibold ${n.isRead ? 'text-slate-700' : 'text-slate-900'}`}>
                            {n.title}
                          </p>
                          {!n.isRead && (
                            <button
                              onClick={() => handleMarkAsRead(n.id)}
                              title="Mark as read"
                              className="text-slate-400 hover:text-emerald-600 p-0.5"
                            >
                              <Check className="w-3 h-3" />
                            </button>
                          )}
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2">{n.message}</p>
                        {n.actionUrl && (
                          <button
                            onClick={() => {
                              setShowNotifications(false);
                              navigate(n.actionUrl!);
                            }}
                            className="inline-flex items-center text-[11px] font-medium text-blue-600 hover:text-blue-800 mt-1"
                          >
                            View details <ExternalLink className="w-2.5 h-2.5 ml-1" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User Avatar */}
        <div
          onClick={() => navigate('/settings')}
          className="flex items-center space-x-2 pl-2 border-l border-slate-200 cursor-pointer group"
        >
          <div className="w-8 h-8 rounded-full bg-navy-900 text-white font-semibold text-xs flex items-center justify-center shadow-sm group-hover:ring-2 group-hover:ring-blue-500/50 transition-all">
            {user?.firstName?.charAt(0) || 'U'}
          </div>
        </div>
      </div>
    </header>
  );
};
