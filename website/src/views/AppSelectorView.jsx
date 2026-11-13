import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getHealthData, getPlanoraTasks } from '../api/client';
import {
  Heart,
  CheckSquare,
  ShieldCheck,
  ArrowRight,
  Flame,
  Scale,
  CheckCircle2,
  Clock,
} from 'lucide-react';

export default function AppSelectorView() {
  const { user, token, isAdmin, setActiveWorkspace } = useAuth();
  const [summary, setSummary] = useState({
    todayCalories: 0,
    targetCalories: 2150,
    latestWeight: null,
    todayTasksTotal: 0,
    todayTasksDone: 0,
  });

  useEffect(() => {
    if (!token) return;
    let mounted = true;
    const todayStr = new Date().toISOString().split('T')[0];
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1;

    Promise.allSettled([getHealthData(token, 0), getPlanoraTasks(token)]).then(([hRes, tRes]) => {
      if (!mounted) return;
      const next = { ...summary };

      if (hRes.status === 'fulfilled' && hRes.value) {
        const foods = (hRes.value.foods || []).filter((f) => !f.isDeleted && f.date === todayStr);
        const weights = (hRes.value.weights || [])
          .filter((w) => !w.isDeleted)
          .sort((a, b) => (b.date || '').localeCompare(a.date || ''));
        const goal = hRes.value.nutritionGoals || hRes.value.goal;

        next.todayCalories = foods.reduce((acc, f) => acc + (f.totalCalories || 0), 0);
        if (goal?.calories) next.targetCalories = goal.calories;
        if (weights[0]?.weightKg) next.latestWeight = weights[0].weightKg;
      }

      if (tRes.status === 'fulfilled' && Array.isArray(tRes.value)) {
        const todayTasks = tRes.value.filter((t) => {
          if (!t.startTime && !t.endTime) return true;
          const s = t.startTime || t.endTime;
          const e = t.endTime || t.startTime;
          return (s >= todayStart && s <= todayEnd) || (e >= todayStart && e <= todayEnd) || (s < todayStart && e > todayEnd);
        });
        next.todayTasksTotal = todayTasks.length;
        next.todayTasksDone = todayTasks.filter((t) => t.status === 'COMPLETED').length;
      }

      setSummary(next);
    });

    return () => {
      mounted = false;
    };
  }, [token]);

  const calPct = Math.min(100, Math.round((summary.todayCalories / (summary.targetCalories || 1)) * 100));

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 sm:py-14 space-y-10">
      {/* Greeting */}
      <div className="space-y-1.5">
        <p className="text-xs font-semibold uppercase tracking-wider text-emerald-400">
          Günlük Özet
        </p>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Merhaba, {user?.name || user?.username}
        </h1>
        <p className="text-sm text-gray-400">
          Devam etmek istediğiniz çalışma alanını seçin.
        </p>
      </div>

      {/* Workspace Cards */}
      <div className={`grid grid-cols-1 md:grid-cols-2 ${isAdmin ? 'lg:grid-cols-3' : ''} gap-5`}>
        {/* HealthTrack Card */}
        <button
          type="button"
          onClick={() => setActiveWorkspace('healthtrack')}
          className="group text-left glass-panel rounded-3xl p-6 border border-emerald-500/20 hover:border-emerald-500/50 transition-all duration-200 flex flex-col justify-between gap-6"
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="w-11 h-11 rounded-2xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
                <Heart className="w-5 h-5" />
              </div>
              <span className="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                Sağlık
              </span>
            </div>

            <div>
              <h2 className="text-xl font-bold text-white group-hover:text-emerald-300 transition-colors">
                HealthTrack
              </h2>
              <p className="text-xs text-gray-400 mt-1">
                Öğün ekle, kalori & makro takibi yap, antrenman ve kilo geçmişini incele.
              </p>
            </div>

            {/* Live Mini Telemetry */}
            <div className="grid grid-cols-2 gap-2.5 pt-1">
              <div className="rounded-2xl bg-white/[0.03] border border-white/[0.06] p-3">
                <div className="flex items-center gap-1.5 text-[11px] text-gray-400 mb-1">
                  <Flame className="w-3.5 h-3.5 text-orange-400" />
                  <span>Bugün</span>
                </div>
                <div className="text-sm font-bold text-white">
                  {summary.todayCalories} <span className="text-[11px] font-normal text-gray-500">/ {summary.targetCalories}</span>
                </div>
                <div className="w-full bg-white/5 h-1 rounded-full mt-2 overflow-hidden">
                  <div className="bg-emerald-400 h-full rounded-full" style={{ width: `${calPct}%` }} />
                </div>
              </div>

              <div className="rounded-2xl bg-white/[0.03] border border-white/[0.06] p-3">
                <div className="flex items-center gap-1.5 text-[11px] text-gray-400 mb-1">
                  <Scale className="w-3.5 h-3.5 text-teal-400" />
                  <span>Son Kilo</span>
                </div>
                <div className="text-sm font-bold text-white">
                  {summary.latestWeight ? `${summary.latestWeight} kg` : '—'}
                </div>
                <div className="text-[10px] text-gray-500 mt-1.5">Güncel ölçüm</div>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-emerald-400">
            <span>HealthTrack&apos;i Aç</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </div>
        </button>

        {/* Planora Card */}
        <button
          type="button"
          onClick={() => setActiveWorkspace('planora')}
          className="group text-left glass-panel rounded-3xl p-6 border border-indigo-500/20 hover:border-indigo-500/50 transition-all duration-200 flex flex-col justify-between gap-6"
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="w-11 h-11 rounded-2xl bg-indigo-500/15 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
                <CheckSquare className="w-5 h-5" />
              </div>
              <span className="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                Planlama
              </span>
            </div>

            <div>
              <h2 className="text-xl font-bold text-white group-hover:text-indigo-300 transition-colors">
                Planora
              </h2>
              <p className="text-xs text-gray-400 mt-1">
                Görev oluştur, tamamlananları işaretle ve günlük planını düzenle.
              </p>
            </div>

            {/* Live Mini Telemetry */}
            <div className="grid grid-cols-2 gap-2.5 pt-1">
              <div className="rounded-2xl bg-white/[0.03] border border-white/[0.06] p-3">
                <div className="flex items-center gap-1.5 text-[11px] text-gray-400 mb-1">
                  <Clock className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Bugün</span>
                </div>
                <div className="text-sm font-bold text-white">
                  {summary.todayTasksTotal} <span className="text-[11px] font-normal text-gray-500">görev</span>
                </div>
                <div className="text-[10px] text-gray-500 mt-1.5">Günlük plan</div>
              </div>

              <div className="rounded-2xl bg-white/[0.03] border border-white/[0.06] p-3">
                <div className="flex items-center gap-1.5 text-[11px] text-gray-400 mb-1">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Tamamlanan</span>
                </div>
                <div className="text-sm font-bold text-white">
                  {summary.todayTasksDone} <span className="text-[11px] font-normal text-gray-500">/ {summary.todayTasksTotal}</span>
                </div>
                <div className="text-[10px] text-gray-500 mt-1.5">İlerleme</div>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-indigo-400">
            <span>Planora&apos;yı Aç</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </div>
        </button>

        {/* Admin Console Card */}
        {isAdmin && (
          <button
            type="button"
            onClick={() => setActiveWorkspace('admin')}
            className="group text-left glass-panel rounded-3xl p-6 border border-amber-500/25 hover:border-amber-500/60 transition-all duration-200 flex flex-col justify-between gap-6"
          >
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="w-11 h-11 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <span className="text-[11px] font-bold px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/25">
                  Admin
                </span>
              </div>

              <div>
                <h2 className="text-xl font-bold text-white group-hover:text-amber-300 transition-colors">
                  Admin Konsolu
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Kullanıcı yönetimi, Premium abonelikler, FCM push bildirimleri ve sistem durumu.
                </p>
              </div>
            </div>

            <div className="pt-4 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-amber-400">
              <span>Konsola Git</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </div>
          </button>
        )}
      </div>
    </div>
  );
}
