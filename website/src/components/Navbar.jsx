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
    const interval = setInterval(testPing, 30000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  const navItems = [
    {
      id: 'selector',
      label: 'Genel Bakış',
      icon: LayoutGrid,
      activeClass: 'bg-white/10 text-white',
      idleClass: 'text-gray-400 hover:text-white hover:bg-white/5',
      iconColor: '',
    },
    {
      id: 'healthtrack',
      label: 'HealthTrack',
      icon: Heart,
      activeClass: 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30',
      idleClass: 'text-gray-400 hover:text-emerald-300 hover:bg-white/5',
      iconColor: 'text-emerald-400',
    },
    {
      id: 'planora',
      label: 'Planora',
      icon: CheckSquare,
      activeClass: 'bg-indigo-500/15 text-indigo-300 border border-indigo-500/30',
      idleClass: 'text-gray-400 hover:text-indigo-300 hover:bg-white/5',
      iconColor: 'text-indigo-400',
    },
  ];

  if (isAdmin) {
    navItems.push({
      id: 'admin',
      label: 'Admin',
      icon: ShieldCheck,
      activeClass: 'bg-amber-500/15 text-amber-300 border border-amber-500/30',
      idleClass: 'text-amber-400/75 hover:text-amber-300 hover:bg-amber-500/10',
      iconColor: 'text-amber-400',
    });
  }

  return (
    <header className="sticky top-0 z-50 border-b border-white/[0.07] bg-[#0B0F19]/85 backdrop-blur-xl no-print">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
        {/* Brand */}
        <button
          type="button"
          onClick={() => setActiveWorkspace('selector')}
          className="flex items-center gap-2.5 text-left group focus:outline-none"
        >
          <div className="flex items-center -space-x-1.5">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-emerald-500 to-teal-400 p-[1.5px] ring-2 ring-[#0B0F19]">
              <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-emerald-400 text-xs">
                H
              </div>
            </div>
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-violet-400 p-[1.5px] ring-2 ring-[#0B0F19]">
              <div className="w-full h-full bg-[#0B0F19] rounded-full flex items-center justify-center font-black text-indigo-400 text-xs">
                P
              </div>
            </div>
          </div>
          <span className="font-bold text-sm tracking-tight text-white group-hover:text-emerald-300 transition-colors hidden sm:inline">
            HealthTrack <span className="text-gray-500 font-normal">&</span> Planora
          </span>
        </button>

        {/* Center Navigation */}
        <nav className="hidden md:flex items-center gap-1 bg-white/[0.03] p-1 rounded-2xl border border-white/[0.06]">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeWorkspace === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveWorkspace(item.id)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${isActive ? item.activeClass : item.idleClass
                  }`}
              >
                <Icon className={`w-3.5 h-3.5 ${item.iconColor}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Right User & Status */}
        <div className="flex items-center gap-2.5">
          <div
            title={
              pingStatus.ok
                ? `Sunucu aktif (${pingStatus.latency ?? '~'}ms)`
                : 'Sunucuya bağlanılıyor...'
            }
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/[0.03] border border-white/[0.06] text-[11px] text-gray-400"
          >
            <span
              className={`w-1.5 h-1.5 rounded-full ${pingStatus.ok ? 'bg-emerald-400' : 'bg-amber-400 animate-pulse'
                }`}
            />
            <span className="hidden sm:inline">
              {pingStatus.ok ? (pingStatus.latency ? `${pingStatus.latency}ms` : 'Çevrimiçi') : 'Bağlanıyor'}
            </span>
          </div>

          <div className="flex items-center gap-2 pl-2 border-l border-white/10">
            <div className="hidden sm:flex items-center gap-1.5">
              <span className="text-xs font-semibold text-white">
                {user?.name || user?.username}
              </span>
              {user?.isPremium && (
                <Crown className="w-3.5 h-3.5 text-amber-400" title="Premium" />
              )}
            </div>

            <button
              onClick={logout}
              title="Çıkış Yap"
              className="p-2 rounded-xl text-gray-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Bottom-of-Header Navigation */}
      <div className="md:hidden flex items-center justify-around border-t border-white/5 px-2 py-1.5 bg-[#0B0F19]/95">
        {navItems.map((item) => {
          const isActive = activeWorkspace === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveWorkspace(item.id)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-xl transition-all ${isActive ? item.activeClass : 'text-gray-400'
                }`}
            >
              {item.label}
            </button>
          );
        })}
      </div>
    </header>
  );
}
