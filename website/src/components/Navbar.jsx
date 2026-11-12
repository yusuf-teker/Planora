import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { checkServerPing } from '../api/client';
import { 
  Heart, 
  CheckSquare, 
  ShieldCheck, 
  LayoutGrid, 
  LogOut, 
  Crown, 
  Activity, 
  Wifi, 
  WifiOff,
  User,
  ExternalLink
} from 'lucide-react';

export default function Navbar() {
  const { user, isAdmin, activeWorkspace, setActiveWorkspace, logout } = useAuth();
  const [pingStatus, setPingStatus] = useState({ ok: true, latency: null });

  useEffect(() => {
    let isMounted = true;
    const testPing = async () => {
      const res = await checkServerPing();
      if (isMounted) setPingStatus(res);
    };
    testPing();
    const interval = setInterval(testPing, 30000); // Test every 30s
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <header className="sticky top-0 z-50 glass-panel border-b border-white/10 bg-[#0B0F19]/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Left: Brand Pod */}
        <div 
          onClick={() => setActiveWorkspace('selector')}
          className="flex items-center gap-3 cursor-pointer select-none group"
        >
          <div className="flex items-center gap-1.5">
            {/* HealthTrack H mark */}
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-emerald-600 to-teal-400 p-[1.5px] shadow-glow-health">
              <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-emerald-400 text-sm tracking-tighter">
                H
              </div>
            </div>
            {/* Planora P mark */}
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-indigo-600 to-violet-400 p-[1.5px] shadow-glow-planora">
              <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-indigo-400 text-sm tracking-tighter">
                P
              </div>
            </div>
          </div>

          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-sm tracking-tight text-white group-hover:text-emerald-300 transition-colors">
                HealthTrack <span className="text-gray-500 font-normal">&</span> Planora
              </span>
              <span className="text-[10px] font-semibold uppercase px-1.5 py-0.5 rounded bg-white/5 border border-white/10 text-gray-300">
                Portal
              </span>
            </div>
            <p className="text-[11px] text-gray-400 leading-none">
              Merkezi Sağlık, Görev & Admin Platformu
            </p>
          </div>
        </div>

        {/* Center: Workspace Navigation Tabs */}
        <nav className="hidden md:flex items-center gap-1.5 bg-white/[0.03] p-1 rounded-2xl border border-white/5">
          <button
            onClick={() => setActiveWorkspace('selector')}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${
              activeWorkspace === 'selector'
                ? 'bg-white/10 text-white shadow-sm'
                : 'text-gray-400 hover:text-gray-200 hover:bg-white/5'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" />
            <span>Merkez</span>
          </button>

          <button
            onClick={() => setActiveWorkspace('healthtrack')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${
              activeWorkspace === 'healthtrack'
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow-glow-health'
                : 'text-gray-400 hover:text-emerald-400 hover:bg-white/5'
            }`}
          >
            <Heart className="w-3.5 h-3.5 text-emerald-400" />
            <span>HealthTrack</span>
          </button>

          <button
            onClick={() => setActiveWorkspace('planora')}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${
              activeWorkspace === 'planora'
                ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 shadow-glow-planora'
                : 'text-gray-400 hover:text-indigo-400 hover:bg-white/5'
            }`}
          >
            <CheckSquare className="w-3.5 h-3.5 text-indigo-400" />
            <span>Planora</span>
          </button>

          {/* Admin Tab - Only visible to ADMIN */}
          {isAdmin && (
            <button
              onClick={() => setActiveWorkspace('admin')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${
                activeWorkspace === 'admin'
                  ? 'bg-gradient-to-r from-amber-500/25 to-rose-500/25 text-amber-300 border border-amber-500/40 shadow-glow-gold'
                  : 'text-amber-400/80 hover:text-amber-300 hover:bg-amber-500/10'
              }`}
            >
              <ShieldCheck className="w-3.5 h-3.5 text-amber-400" />
              <span>Admin Konsolu</span>
              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
            </button>
          )}
        </nav>

        {/* Right: Telemetry & User Session */}
        <div className="flex items-center gap-3">
          {/* Server Uptime / Ping Indicator */}
          <div 
            title={pingStatus.ok ? `Render Sunucu: Aktif (${pingStatus.latency}ms)` : 'Sunucu kontrol ediliyor...'}
            className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/[0.03] border border-white/5 text-[11px]"
          >
            <span className={`w-2 h-2 rounded-full ${pingStatus.ok ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400 animate-pulse'}`} />
            <span className="text-gray-400">Render:</span>
            <span className={pingStatus.ok ? 'text-gray-200 font-mono font-medium' : 'text-amber-400'}>
              {pingStatus.ok ? (pingStatus.latency ? `${pingStatus.latency}ms` : 'Canlı') : 'Bağlanıyor...'}
            </span>
          </div>

          {/* User Badge & Profile */}
          <div className="flex items-center gap-2 pl-2 border-l border-white/10">
            <div className="text-right hidden sm:block">
              <div className="flex items-center justify-end gap-1.5">
                <span className="text-xs font-bold text-white leading-none">
                  {user?.name || user?.username}
                </span>
                {user?.isPremium && (
                  <Crown className="w-3 h-3 text-amber-400" title="Premium Üye" />
                )}
              </div>
              <div className="flex items-center justify-end gap-1 mt-0.5">
                <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded uppercase ${
                  isAdmin 
                    ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' 
                    : user?.isPremium 
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'bg-white/10 text-gray-400'
                }`}>
                  {isAdmin ? 'ADMIN' : user?.isPremium ? 'PREMIUM' : 'USER'}
                </span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Güvenli Çıkış Yap"
              className="p-2 rounded-xl text-gray-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors border border-transparent hover:border-rose-500/20"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>

        </div>
      </div>

      {/* Mobile Workspace Tabs */}
      <div className="md:hidden flex items-center justify-around border-t border-white/5 px-2 py-1.5 bg-[#0B0F19]/90">
        <button
          onClick={() => setActiveWorkspace('selector')}
          className={`px-3 py-1 text-xs font-medium rounded-lg ${
            activeWorkspace === 'selector' ? 'text-white bg-white/10' : 'text-gray-400'
          }`}
        >
          Merkez
        </button>
        <button
          onClick={() => setActiveWorkspace('healthtrack')}
          className={`px-3 py-1 text-xs font-medium rounded-lg ${
            activeWorkspace === 'healthtrack' ? 'text-emerald-400 bg-emerald-500/10 font-bold' : 'text-gray-400'
          }`}
        >
          HealthTrack
        </button>
        <button
          onClick={() => setActiveWorkspace('planora')}
          className={`px-3 py-1 text-xs font-medium rounded-lg ${
            activeWorkspace === 'planora' ? 'text-indigo-400 bg-indigo-500/10 font-bold' : 'text-gray-400'
          }`}
        >
          Planora
        </button>
        {isAdmin && (
          <button
            onClick={() => setActiveWorkspace('admin')}
            className={`px-3 py-1 text-xs font-medium rounded-lg ${
              activeWorkspace === 'admin' ? 'text-amber-400 bg-amber-500/10 font-bold' : 'text-amber-400/70'
            }`}
          >
            Admin
          </button>
        )}
      </div>
    </header>
  );
}

