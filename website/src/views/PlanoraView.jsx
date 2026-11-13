import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getPlanoraTasks } from '../api/client';
import { 
  CheckSquare, 
  Clock, 
  Calendar, 
  CheckCircle2, 
  Circle, 
  RefreshCw, 
  AlertCircle, 
  Search, 
  Tag, 
  Layers,
  Sparkles,
  BarChart2,
  History,
  CalendarDays
} from 'lucide-react';

export default function PlanoraView() {
  const { token, user } = useAuth();
  const cacheKey = user?.id ? `planora_tasks_cache_${user.id}` : 'planora_tasks_cache_default';

  const [tasks, setTasks] = useState(() => {
    try {
      const cached = localStorage.getItem(cacheKey);
      if (cached) return JSON.parse(cached);
    } catch (_) {}
    return [];
  });

  const [loading, setLoading] = useState(tasks.length === 0);
  const [error, setError] = useState('');
  const [dateFilter, setDateFilter] = useState('TODAY'); // 'TODAY', 'FUTURE', 'PAST', 'ALL'
  const [filterStatus, setFilterStatus] = useState('ALL'); // 'ALL', 'PENDING', 'COMPLETED'
  const [searchQuery, setSearchQuery] = useState('');

  const loadTasks = async (isManualRefresh = false) => {
    if (isManualRefresh || tasks.length === 0) {
      setLoading(true);
    }
    setError('');
    try {
      const data = await getPlanoraTasks(token);
      const list = Array.isArray(data) ? data : [];
      setTasks(list);
      try {
        localStorage.setItem(cacheKey, JSON.stringify(list));
      } catch (_) {}
    } catch (err) {
      console.error('Failed to load Planora tasks:', err);
      if (tasks.length === 0) {
        setError(err.message || 'Görevler alınırken hata oluştu.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      loadTasks(false);
    }
  }, [token]);

  // Date Boundaries
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1;

  const isTaskToday = (task) => {
    if (!task.startTime && !task.endTime) return true; // Tarihsiz görevler bugün görünür
    const start = task.startTime || task.endTime;
    const end = task.endTime || task.startTime;
    return (start >= todayStart && start <= todayEnd) || 
           (end >= todayStart && end <= todayEnd) || 
           (start < todayStart && end > todayEnd);
  };

  const isTaskPast = (task) => {
    const end = task.endTime || task.startTime;
    return Boolean(end && end < todayStart);
  };

  const isTaskFuture = (task) => {
    const start = task.startTime;
    return Boolean(start && start > todayEnd);
  };

  // Date counts
  const todayCount = tasks.filter(isTaskToday).length;
  const futureCount = tasks.filter(isTaskFuture).length;
  const pastCount = tasks.filter(isTaskPast).length;
  const totalTasks = tasks.length;

  const completedTasks = tasks.filter(t => t.status === 'COMPLETED').length;
  const pendingTasks = tasks.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS').length;
  const completionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  // Filtered List
  const filteredTasks = tasks.filter(task => {
    // Search query
    const matchesSearch = !searchQuery.trim() || 
      task.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      task.description?.toLowerCase().includes(searchQuery.toLowerCase());

    // Status filter
    const matchesStatus = 
      filterStatus === 'ALL' ||
      (filterStatus === 'COMPLETED' && task.status === 'COMPLETED') ||
      (filterStatus === 'PENDING' && task.status !== 'COMPLETED');

    // Date filter
    let matchesDate = true;
    if (dateFilter === 'TODAY') {
      matchesDate = isTaskToday(task);
    } else if (dateFilter === 'FUTURE') {
      matchesDate = isTaskFuture(task);
    } else if (dateFilter === 'PAST') {
      matchesDate = isTaskPast(task);
    }

    return matchesSearch && matchesStatus && matchesDate;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <CheckSquare className="w-4 h-4" />
            </div>
            <h1 className="text-2xl font-extrabold text-white tracking-tight">
              Planora Verimlilik & Görev Panosu
            </h1>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            Masaüstünden görevlerinizi ve hedeflerinizi anlık olarak takip edin
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => loadTasks(true)}
            disabled={loading}
            className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition-all flex items-center gap-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Yenile</span>
          </button>
        </div>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-3">
          <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Telemetry Bento Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        
        {/* Metric 1: Total Tasks */}
        <div className="glass-panel rounded-3xl p-6 border border-indigo-500/20">
          <div className="flex items-center justify-between text-xs text-indigo-400 font-bold uppercase tracking-wider mb-2">
            <span>Toplam Görev</span>
            <Layers className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-3xl font-black text-white">{totalTasks}</div>
          <p className="text-[11px] text-gray-400 mt-2">Kayıtlı tüm görevler</p>
        </div>

        {/* Metric 2: Today Tasks */}
        <div className="glass-panel rounded-3xl p-6 border border-teal-500/20">
          <div className="flex items-center justify-between text-xs text-teal-400 font-bold uppercase tracking-wider mb-2">
            <span>Bugünkü Görevler</span>
            <CalendarDays className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-3xl font-black text-white">{todayCount}</div>
          <p className="text-[11px] text-gray-400 mt-2">Bugün yapılması planlananlar</p>
        </div>

        {/* Metric 3: Pending */}
        <div className="glass-panel rounded-3xl p-6 border border-amber-500/20">
          <div className="flex items-center justify-between text-xs text-amber-400 font-bold uppercase tracking-wider mb-2">
            <span>Bekleyen / Aktif</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-3xl font-black text-white">{pendingTasks}</div>
          <p className="text-[11px] text-gray-400 mt-2">Yapılacaklar listesindeki görevler</p>
        </div>

        {/* Metric 4: Completion Rate */}
        <div className="glass-panel rounded-3xl p-6 border border-white/10">
          <div className="flex items-center justify-between text-xs text-indigo-300 font-bold uppercase tracking-wider mb-2">
            <span>Verimlilik Skoru</span>
            <BarChart2 className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-black text-white">%{completionRate}</span>
            <span className="text-xs text-gray-400">tamamlanma</span>
          </div>

          <div className="w-full bg-white/5 h-2 rounded-full mt-3 overflow-hidden">
            <div 
              className="bg-gradient-to-r from-indigo-500 to-violet-400 h-full rounded-full transition-all duration-500"
              style={{ width: `${completionRate}%` }}
            />
          </div>
        </div>

      </div>

      {/* Primary Date Filter Tabs: BUGÜN, GEÇMİŞ, GELECEK, TÜMÜ */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        
        {/* Date Tabs */}
        <div className="flex items-center gap-2 bg-white/[0.03] p-1 rounded-2xl border border-white/10 overflow-x-auto">
          <button
            onClick={() => setDateFilter('TODAY')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all whitespace-nowrap ${
              dateFilter === 'TODAY'
                ? 'bg-gradient-to-r from-indigo-600 to-indigo-500 text-white shadow-lg shadow-indigo-500/25 border border-indigo-400/40'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <Calendar className="w-3.5 h-3.5" />
            <span>Bugün</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full ${dateFilter === 'TODAY' ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-400'}`}>
              {todayCount}
            </span>
          </button>

          <button
            onClick={() => setDateFilter('FUTURE')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all whitespace-nowrap ${
              dateFilter === 'FUTURE'
                ? 'bg-gradient-to-r from-teal-600 to-emerald-500 text-white shadow-lg shadow-teal-500/25 border border-teal-400/40'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>Gelecek</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full ${dateFilter === 'FUTURE' ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-400'}`}>
              {futureCount}
            </span>
          </button>

          <button
            onClick={() => setDateFilter('PAST')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all whitespace-nowrap ${
              dateFilter === 'PAST'
                ? 'bg-gradient-to-r from-purple-600 to-violet-500 text-white shadow-lg shadow-purple-500/25 border border-purple-400/40'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <History className="w-3.5 h-3.5" />
            <span>Geçmiş</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full ${dateFilter === 'PAST' ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-400'}`}>
              {pastCount}
            </span>
          </button>

          <button
            onClick={() => setDateFilter('ALL')}
            className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center gap-2 transition-all whitespace-nowrap ${
              dateFilter === 'ALL'
                ? 'bg-white/15 text-white border border-white/20 shadow-sm'
                : 'text-gray-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            <span>Tümü</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full ${dateFilter === 'ALL' ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-400'}`}>
              {totalTasks}
            </span>
          </button>
        </div>

        {/* Secondary: Status Filters (Tümü, Bekleyenler, Tamamlananlar) */}
        <div className="flex items-center gap-1.5 bg-white/5 p-1 rounded-xl border border-white/5 self-stretch sm:self-auto justify-center">
          <button
            onClick={() => setFilterStatus('ALL')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterStatus === 'ALL' ? 'bg-white/10 text-white shadow-sm' : 'text-gray-400 hover:text-white'
            }`}
          >
            Tüm Durumlar
          </button>
          <button
            onClick={() => setFilterStatus('PENDING')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterStatus === 'PENDING' ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' : 'text-gray-400 hover:text-white'
            }`}
          >
            Bekleyenler
          </button>
          <button
            onClick={() => setFilterStatus('COMPLETED')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterStatus === 'COMPLETED' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'text-gray-400 hover:text-white'
            }`}
          >
            Tamamlananlar
          </button>
        </div>

      </div>

      {/* Search Input */}
      <div className="relative w-full">
        <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder={`${dateFilter === 'TODAY' ? 'Bugünkü' : dateFilter === 'FUTURE' ? 'Gelecekteki' : dateFilter === 'PAST' ? 'Geçmiş' : 'Tüm'} görevlerde ara...`}
          className="w-full pl-10 pr-4 py-2.5 rounded-xl glass-input text-xs text-white placeholder-gray-500 outline-none border border-white/10 focus:border-indigo-500/40"
        />
      </div>

      {/* Task List */}
      <div className="space-y-3">
        {filteredTasks.length === 0 ? (
          <div className="glass-panel rounded-3xl p-12 text-center border border-white/5">
            <CheckSquare className="w-10 h-10 text-gray-600 mx-auto mb-3" />
            <h3 className="text-base font-bold text-gray-200">
              {searchQuery ? 'Aramanızla Eşleşen Görev Bulunamadı' : `${dateFilter === 'TODAY' ? 'Bugün için' : dateFilter === 'FUTURE' ? 'Gelecek için' : dateFilter === 'PAST' ? 'Geçmişte' : ''} Görev Bulunmuyor`}
            </h3>
            <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
              {searchQuery 
                ? 'Farklı bir arama terimi deneyebilir veya filtreleri değiştirebilirsiniz.' 
                : 'Planora mobil uygulamasından yeni görevler ve etkinlikler ekleyebilirsiniz.'}
            </p>
          </div>
        ) : (
          filteredTasks.map((task) => {
            const isCompleted = task.status === 'COMPLETED';
            const isToday = isTaskToday(task);
            const isPast = isTaskPast(task);
            const isFuture = isTaskFuture(task);

            return (
              <div 
                key={task.id}
                className={`glass-panel rounded-2xl p-4 sm:p-5 border transition-all duration-200 flex items-start justify-between gap-4 ${
                  isCompleted 
                    ? 'border-white/5 opacity-70 bg-white/[0.01]' 
                    : 'border-white/10 hover:border-indigo-500/30'
                }`}
              >
                <div className="flex items-start gap-3.5">
                  <div className="pt-0.5">
                    {isCompleted ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                    ) : (
                      <Circle className="w-5 h-5 text-gray-500" />
                    )}
                  </div>

                  <div>
                    <h3 className={`text-sm sm:text-base font-bold ${isCompleted ? 'line-through text-gray-400' : 'text-white'}`}>
                      {task.title}
                    </h3>

                    {task.description && (
                      <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                        {task.description}
                      </p>
                    )}

                    <div className="flex flex-wrap items-center gap-2 mt-3">
                      {/* Date Badge: Bugün / Gelecek / Geçmiş */}
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        isToday 
                          ? 'bg-indigo-500/15 text-indigo-300 border border-indigo-500/30' 
                          : isFuture 
                          ? 'bg-teal-500/15 text-teal-300 border border-teal-500/30' 
                          : 'bg-purple-500/15 text-purple-300 border border-purple-500/30'
                      }`}>
                        {isToday ? 'Bugün' : isFuture ? 'Gelecek' : 'Geçmiş'}
                      </span>

                      {/* Type Badge */}
                      <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded bg-white/5 border border-white/10 text-gray-300">
                        {task.type || 'TASK'}
                      </span>

                      {/* Time */}
                      {task.startTime && (
                        <span className="text-[10px] text-gray-400 flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {new Date(task.startTime).toLocaleDateString('tr-TR', { 
                            day: 'numeric', 
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="shrink-0">
                  <span className={`text-[10px] font-bold uppercase px-2 py-0.5 rounded ${
                    isCompleted 
                      ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' 
                      : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                  }`}>
                    {task.status || 'PENDING'}
                  </span>
                </div>
              </div>
            );
          })
        )}
      </div>

    </div>
  );
}
