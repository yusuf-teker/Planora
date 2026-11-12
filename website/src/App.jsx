import React from 'react';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginView from './views/LoginView';
import AppSelectorView from './views/AppSelectorView';
import HealthTrackView from './views/HealthTrackView';
import PlanoraView from './views/PlanoraView';
import AdminView from './views/AdminView';

export default function App() {
  const { user, loading, activeWorkspace, isAdmin } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0B0F19] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
          <span className="text-xs text-gray-400 font-medium tracking-wider uppercase">
            Yükleniyor...
          </span>
        </div>
      </div>
    );
  }

  if (!user) {
    return <LoginView />;
  }

  return (
    <div className="min-h-screen bg-[#0B0F19] flex flex-col text-[#F8FAFC]">
      <Navbar />

      <main className="flex-1 pb-16">
        <div className={activeWorkspace === 'selector' ? 'block' : 'hidden'}>
          <AppSelectorView />
        </div>
        <div className={activeWorkspace === 'healthtrack' ? 'block' : 'hidden'}>
          <HealthTrackView />
        </div>
        <div className={activeWorkspace === 'planora' ? 'block' : 'hidden'}>
          <PlanoraView />
        </div>
        {isAdmin && (
          <div className={activeWorkspace === 'admin' ? 'block' : 'hidden'}>
            <AdminView />
          </div>
        )}
      </main>

      <footer className="border-t border-white/5 py-6 text-center text-xs text-gray-500 no-print">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>HealthTrack & Planora Ekosistemi © 2026</span>
          <span className="text-[11px] text-gray-600">Render Cloud & Neon PostgreSQL Senkronizasyonu</span>
        </div>
      </footer>
    </div>
  );
}

