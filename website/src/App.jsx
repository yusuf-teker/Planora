import React, { useState, useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginView from './views/LoginView';
import AppSelectorView from './views/AppSelectorView';
import HealthTrackView from './views/HealthTrackView';
import PlanoraView from './views/PlanoraView';
import AdminView from './views/AdminView';
import PrivacyPolicyView from './views/PrivacyPolicyView';
import TermsOfServiceView from './views/TermsOfServiceView';
import DeleteAccountView from './views/DeleteAccountView';

function getLegalRoute(pathname) {
  if (pathname === '/privacy' || pathname === '/privacy/') return 'privacy';
  if (pathname === '/terms' || pathname === '/terms/') return 'terms';
  if (pathname === '/delete-account' || pathname === '/delete-account/') return 'delete-account';
  return null;
}

export default function App() {
  const { user, loading, activeWorkspace, isAdmin } = useAuth();
  const [legalRoute, setLegalRoute] = useState(() =>
    typeof window !== 'undefined' ? getLegalRoute(window.location.pathname) : null
  );

  useEffect(() => {
    const handlePopState = () => {
      setLegalRoute(getLegalRoute(window.location.pathname));
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateTo = (path) => {
    if (typeof window !== 'undefined') {
      window.history.pushState({}, '', path);
      setLegalRoute(getLegalRoute(path));
      window.scrollTo(0, 0);
    }
  };

  if (legalRoute === 'privacy') {
    return <PrivacyPolicyView onBack={() => navigateTo('/')} />;
  }
  if (legalRoute === 'terms') {
    return <TermsOfServiceView onBack={() => navigateTo('/')} />;
  }
  if (legalRoute === 'delete-account') {
    return <DeleteAccountView onBack={() => navigateTo('/')} />;
  }

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
    return <LoginView onNavigateLegal={navigateTo} />;
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
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <span>HealthTrack & Planora Ekosistemi © 2026</span>
          <div className="flex items-center gap-4 text-[11px] text-gray-400">
            <a
              href="/privacy"
              onClick={(e) => { e.preventDefault(); navigateTo('/privacy'); }}
              className="hover:text-emerald-400 transition-colors"
            >
              Gizlilik Politikası
            </a>
            <a
              href="/terms"
              onClick={(e) => { e.preventDefault(); navigateTo('/terms'); }}
              className="hover:text-emerald-400 transition-colors"
            >
              Kullanım Koşulları
            </a>
            <a
              href="/delete-account"
              onClick={(e) => { e.preventDefault(); navigateTo('/delete-account'); }}
              className="hover:text-rose-400 transition-colors"
            >
              Hesap Silme
            </a>
          </div>
          <span className="text-[11px] text-gray-600">Render Cloud & Neon PostgreSQL Senkronizasyonu</span>
        </div>
      </footer>
    </div>
  );
}
