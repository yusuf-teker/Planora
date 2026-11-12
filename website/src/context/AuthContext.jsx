import React, { createContext, useContext, useState, useEffect } from 'react';
import * as api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [adminSecret, setAdminSecret] = useState('');
  const [activeWorkspace, setActiveWorkspace] = useState('selector'); // 'selector', 'healthtrack', 'planora', 'admin'
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Restore session from localStorage
    try {
      const savedToken = localStorage.getItem('auth_token');
      const savedUser = localStorage.getItem('auth_user');
      const savedSecret = localStorage.getItem('admin_secret');
      const savedWorkspace = localStorage.getItem('active_workspace') || 'selector';

      if (savedToken && savedToken !== 'undefined' && savedToken !== 'null' && savedUser) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
        setActiveWorkspace(savedWorkspace);
      } else {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_user');
      }
      if (savedSecret) {
        setAdminSecret(savedSecret);
      }
    } catch (e) {
      console.error('Failed to restore auth session:', e);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleLogin = async (identifier, password) => {
    const res = await api.login(identifier, password);
    // res contains: { accessToken, token, refreshToken, userId, name, username, isPremium, premiumUntil, role }
    const authToken = res.accessToken || res.token;
    const userData = {
      id: res.userId,
      name: res.name || res.username,
      username: res.username,
      role: (res.role || 'USER').toUpperCase(),
      isPremium: Boolean(res.isPremium),
      premiumUntil: res.premiumUntil,
      profileImageUrl: res.profileImageUrl,
      avatarId: res.avatarId,
    };

    setToken(authToken);
    setUser(userData);
    localStorage.setItem('auth_token', authToken);
    localStorage.setItem('auth_user', JSON.stringify(userData));

    // Default workspace after login:
    // If admin, show selector so they can choose
    setActiveWorkspace('selector');
    localStorage.setItem('active_workspace', 'selector');
    return userData;
  };

  const handleLogout = () => {
    setUser(null);
    setToken(null);
    setActiveWorkspace('selector');
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('active_workspace');
  };

  const switchWorkspace = (workspace) => {
    setActiveWorkspace(workspace);
    localStorage.setItem('active_workspace', workspace);
  };

  const updateAdminSecret = (secret) => {
    setAdminSecret(secret);
    if (secret) {
      localStorage.setItem('admin_secret', secret);
    } else {
      localStorage.removeItem('admin_secret');
    }
  };

  const isAdmin = user?.role === 'ADMIN' || Boolean(adminSecret);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAdmin,
        adminSecret,
        setAdminSecret: updateAdminSecret,
        activeWorkspace,
        setActiveWorkspace: switchWorkspace,
        login: handleLogin,
        logout: handleLogout,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

