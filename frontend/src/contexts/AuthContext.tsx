import React, { createContext, useContext, useEffect, useState } from 'react';
import { User } from '../types';
import { authApi } from '../api/auth';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: { email: string; password: string }) => Promise<void>;
  register: (data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phoneNumber?: string;
  }) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('finwise_user');
    if (saved && saved !== 'undefined' && saved !== 'null') {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  });

  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('finwise_access_token');
      if (token) {
        try {
          const profile = await authApi.getCurrentUser();
          setUser(profile);
          localStorage.setItem('finwise_user', JSON.stringify(profile));
        } catch {
          // Token expired or invalid
          localStorage.removeItem('finwise_access_token');
          localStorage.removeItem('finwise_refresh_token');
          localStorage.removeItem('finwise_user');
          setUser(null);
        }
      }
      setIsLoading(false);
    };

    initAuth();

    const handleLogout = () => {
      setUser(null);
    };

    window.addEventListener('finwise-auth-logout', handleLogout);
    return () => window.removeEventListener('finwise-auth-logout', handleLogout);
  }, []);

  const login = async (credentials: { email: string; password: string }) => {
    setIsLoading(true);
    try {
      const authData = await authApi.login(credentials);
      localStorage.setItem('finwise_access_token', authData.accessToken);
      localStorage.setItem('finwise_refresh_token', authData.refreshToken);
      if (authData.user) {
        localStorage.setItem('finwise_user', JSON.stringify(authData.user));
        setUser(authData.user);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phoneNumber?: string;
  }) => {
    setIsLoading(true);
    try {
      const authData = await authApi.register(data);
      localStorage.setItem('finwise_access_token', authData.accessToken);
      localStorage.setItem('finwise_refresh_token', authData.refreshToken);
      if (authData.user) {
        localStorage.setItem('finwise_user', JSON.stringify(authData.user));
        setUser(authData.user);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  };

  const updateUser = (updated: User) => {
    setUser(updated);
    localStorage.setItem('finwise_user', JSON.stringify(updated));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
