import React, { useState } from 'react';
import {
  UserCog,
  Lock,
  Shield,
  Key,
  Smartphone,
  Mail,
  CheckCircle2,
  AlertCircle,
  Save,
} from 'lucide-react';
import { Card, CardHeader, CardTitle } from '../components/common/Card';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Badge } from '../components/common/Badge';
import { useAuth } from '../contexts/AuthContext';
import { authApi } from '../api/auth';
import { useToast } from '../contexts/ToastContext';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  // Password change state
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  // Preference toggles
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [thresholdAlerts, setThresholdAlerts] = useState(true);
  const [twoFactorAuth, setTwoFactorAuth] = useState(true);

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!oldPassword || !newPassword) {
      showToast('Please provide current and new password', 'error');
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast('New passwords do not match', 'error');
      return;
    }

    if (newPassword.length < 8) {
      showToast('New password must be at least 8 characters long', 'error');
      return;
    }

    setIsChangingPassword(true);
    try {
      await authApi.changePassword({ oldPassword, newPassword });
      showToast('Password changed successfully!', 'success');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch {
      showToast('Failed to change password. Please check your current password.', 'error');
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">User Profile & Account Settings</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Manage your personal details, credentials, security preferences, and financial reporting parameters.
        </p>
      </div>

      {/* Profile Overview Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <UserCog className="w-5 h-5 text-blue-600" />
            <CardTitle>Personal Identification Details</CardTitle>
          </div>
          <Badge variant="emerald" size="sm">
            Active Account
          </Badge>
        </CardHeader>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input
            label="First Name"
            disabled
            value={user?.firstName || 'Rahul'}
            helperText="Synchronized with official identity KYC"
          />

          <Input
            label="Last Name"
            disabled
            value={user?.lastName || 'Verma'}
          />

          <Input
            label="Registered Email"
            disabled
            value={user?.email || 'rahul.verma@example.com'}
          />

          <Input
            label="Phone Number"
            disabled
            value={user?.phoneNumber || '+91 98765 43210'}
          />

          <Input
            label="Base Reporting Currency"
            disabled
            value="INR - Indian Rupee (₹)"
            helperText="Supports Lakh (₹1,00,000) & Crore (₹1,00,00,000) formatting"
          />

          <Input
            label="User Roles"
            disabled
            value="ROLE_USER (Standard Consumer)"
          />
        </div>
      </Card>

      {/* Security & Password Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Lock className="w-5 h-5 text-navy-900" />
            <CardTitle>Change Security Password</CardTitle>
          </div>
        </CardHeader>

        <form onSubmit={handlePasswordChange} className="space-y-4">
          <Input
            label="Current Password"
            type="password"
            required
            placeholder="••••••••••••"
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="New Password"
              type="password"
              required
              placeholder="••••••••••••"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />

            <Input
              label="Confirm New Password"
              type="password"
              required
              placeholder="••••••••••••"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          <div className="flex justify-end pt-2">
            <Button type="submit" variant="primary" isLoading={isChangingPassword}>
              <Key className="w-4 h-4 mr-1.5" />
              Update Password
            </Button>
          </div>
        </form>
      </Card>

      {/* Security & Privacy Preferences */}
      <Card>
        <CardHeader>
          <div className="flex items-center space-x-2">
            <Shield className="w-5 h-5 text-emerald-600" />
            <CardTitle>Security & Notification Thresholds</CardTitle>
          </div>
        </CardHeader>

        <div className="divide-y divide-slate-100 text-xs">
          <div className="py-3 flex items-center justify-between">
            <div>
              <p className="font-bold text-slate-800">Budget Warning Alerts (80% & 100%)</p>
              <p className="text-slate-500 mt-0.5">Push notification whenever category burn-rate reaches critical threshold.</p>
            </div>
            <input
              type="checkbox"
              checked={thresholdAlerts}
              onChange={(e) => setThresholdAlerts(e.target.checked)}
              className="h-4 w-4 rounded text-navy-900 focus:ring-navy-900"
            />
          </div>

          <div className="py-3 flex items-center justify-between">
            <div>
              <p className="font-bold text-slate-800">Upcoming Bill Reminders</p>
              <p className="text-slate-500 mt-0.5">Notify 7 days prior to recurring bill and SIP auto-execution.</p>
            </div>
            <input
              type="checkbox"
              checked={emailAlerts}
              onChange={(e) => setEmailAlerts(e.target.checked)}
              className="h-4 w-4 rounded text-navy-900 focus:ring-navy-900"
            />
          </div>

          <div className="py-3 flex items-center justify-between">
            <div>
              <p className="font-bold text-slate-800">Rotating Refresh Token Protection</p>
              <p className="text-slate-500 mt-0.5">Enforce single-use refresh token exchange with automatic token invalidation.</p>
            </div>
            <Badge variant="emerald" size="sm">
              Enforced by Server
            </Badge>
          </div>
        </div>
      </Card>
    </div>
  );
};
