import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  getPlanoraTasks,
  createPlanoraTask,
  updatePlanoraTask,
  deletePlanoraTask,
} from '../api/client';
import {
  CheckSquare,
  Clock,
  CheckCircle2,
  Circle,
  RefreshCw,
  AlertCircle,
  Search,
  Plus,
  Trash2,
} from 'lucide-react';

const TYPE_LABELS = {
  TASK: 'Görev',
  EVENT: 'Etkinlik',
  NOTE: 'Not',
  FOLDER: 'Klasör',
};

export default function PlanoraView() {
  const { token, user } = useAuth();
  const cacheKey = user?.id ? `planora_tasks_cache_${user.id}` : 'planora_tasks_cache_default';
  const todayStr = new Date().toISOString().split('T')[0];

  const [tasks, setTasks] = useState(() => {
    try {
      const cached = localStorage.getItem(cacheKey);
      if (cached) return JSON.parse(cached);
    } catch (_) { }
    return [];
  });

  const [loading, setLoading] = useState(tasks.length === 0);
  const [error, setError] = useState('');
  const [dateFilter, setDateFilter] = useState('TODAY'); // 'TODAY', 'FUTURE', 'PAST', 'ALL'
  const [filterStatus, setFilterStatus] = useState('ALL'); // 'ALL', 'PENDING', 'COMPLETED'
  const [searchQuery, setSearchQuery] = useState('');

  // Quick Add Task State
  const [newTitle, setNewTitle] = useState('');
  const [newType, setNewType] = useState('TASK');
  const [newDate, setNewDate] = useState(todayStr);
  const [creating, setCreating] = useState(false);

  const persistCache = (list) => {
    setTasks(list);
    try {
      localStorage.setItem(cacheKey, JSON.stringify(list));
    } catch (_) { }
  };

  const loadTasks = async (isManualRefresh = false) => {
    if (isManualRefresh || tasks.length === 0) setLoading(true);
    setError('');
    try {
      const data = await getPlanoraTasks(token);
      const list = Array.isArray(data) ? data : [];
      persistCache(list);
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
    if (token) loadTasks(false);
  }, [token]);

  const handleCreateTask = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setCreating(true);
    setError('');
    try {
      const [y, m, d] = newDate.split('-').map(Number);
      const startTime = new Date(y, m - 1, d, 9, 0, 0).getTime();
      const created = await createPlanoraTask(token, {
        title: newTitle.trim(),
        startTime,
        type: newType,
        status: 'PENDING',
        visibility: 'PRIVATE',
      });
      persistCache([created, ...tasks]);
      setNewTitle('');
    } catch (err) {
      setError(err.message || 'Görev eklenemedi.');
    } finally {
      setCreating(false);
    }
  };

  const handleToggleTask = async (task) => {
    const nextStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
    const updatedLocal = tasks.map((t) =>
      t.id === task.id ? { ...t, status: nextStatus } : t
    );
    persistCache(updatedLocal);

    try {
      await updatePlanoraTask(token, task.id, {
        title: task.title,
        description: task.description || null,
        startTime: task.startTime || Date.now(),
        endTime: task.endTime || null,
        type: task.type || 'TASK',
        status: nextStatus,
        visibility: task.visibility || 'PRIVATE',
      });
    } catch (err) {
      setError(err.message || 'Görev güncellenemedi.');
      loadTasks(true);
    }
  };

  const handleDeleteTask = async (taskId) => {
    const prev = tasks;
    persistCache(tasks.filter((t) => t.id !== taskId));
    try {
      await deletePlanoraTask(token, taskId);
    } catch (err) {
      setError(err.message || 'Görev silinemedi.');
      persistCache(prev);
    }
  };

  // Date Boundaries
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1;

  const isTaskToday = (task) => {
    if (!task.startTime && !task.endTime) return true;
    const start = task.startTime || task.endTime;
    const end = task.endTime || task.startTime;
    return (
      (start >= todayStart && start <= todayEnd) ||
      (end >= todayStart && end <= todayEnd) ||
      (start < todayStart && end > todayEnd)
    );
  };

  const isTaskPast = (task) => {
    const end = task.endTime || task.startTime;
    return Boolean(end && end < todayStart);
  };

  const isTaskFuture = (task) => {
    const start = task.startTime;
    return Boolean(start && start > todayEnd);
  };

  const todayCount = tasks.filter(isTaskToday).length;
  const futureCount = tasks.filter(isTaskFuture).length;
  const pastCount = tasks.filter(isTaskPast).length;
  const totalTasks = tasks.length;
  const completedTasks = tasks.filter((t) => t.status === 'COMPLETED').length;
  const completionRate = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

  const filteredTasks = tasks.filter((task) => {
    const matchesSearch =
      !searchQuery.trim() ||
      task.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      task.description?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus =
      filterStatus === 'ALL' ||
      (filterStatus === 'COMPLETED' && task.status === 'COMPLETED') ||
      (filterStatus === 'PENDING' && task.status !== 'COMPLETED');

    let matchesDate = true;
    if (dateFilter === 'TODAY') matchesDate = isTaskToday(task);
    else if (dateFilter === 'FUTURE') matchesDate = isTaskFuture(task);
    else if (dateFilter === 'PAST') matchesDate = isTaskPast(task);

    return matchesSearch && matchesStatus && matchesDate;
  });

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-7 space-y-6">
      {/* Compact Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Planora</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Günlük görevler, etkinlikler ve planlarınız
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/25 text-xs font-semibold text-indigo-300">
            %{completionRate} • {completedTasks}/{totalTasks} tamamlandı
          </div>
          <button
            onClick={() => loadTasks(true)}
            disabled={loading}
            title="Yenile"
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {error && (
        <div className="p-3.5 rounded-2xl bg-rose-500/10 border border-rose-500/25 text-rose-300 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError('')} className="hover:text-white">
            Kapat
          </button>
        </div>
      )}

      {/* Quick Add Task Bar */}
      <form
        onSubmit={handleCreateTask}
        className="glass-panel rounded-2xl p-3 border border-indigo-500/25 flex flex-col sm:flex-row gap-2.5"
      >
        <input
          type="text"
          value={newTitle}
          onChange={(e) => setNewTitle(e.target.value)}
          placeholder="Yeni görev veya etkinlik ekle..."
          className="flex-1 px-3.5 py-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 focus:border-indigo-500/50 outline-none"
        />
        <div className="flex items-center gap-2">
          <select
            value={newType}
            onChange={(e) => setNewType(e.target.value)}
            className="px-3 py-2.5 rounded-xl bg-[#0F1626] border border-white/10 text-xs text-gray-200 outline-none cursor-pointer"
          >
            <option value="TASK">Görev</option>
            <option value="EVENT">Etkinlik</option>
            <option value="NOTE">Not</option>
          </select>
          <input
            type="date"
            value={newDate}
            onChange={(e) => setNewDate(e.target.value)}
            className="px-3 py-2.5 rounded-xl bg-[#0F1626] border border-white/10 text-xs text-gray-200 outline-none cursor-pointer"
          />
          <button
            type="submit"
            disabled={creating || !newTitle.trim()}
            className="px-4 py-2.5 rounded-xl bg-indigo-500 hover:bg-indigo-400 text-white text-xs font-bold flex items-center gap-1.5 disabled:opacity-50 shrink-0 transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>{creating ? 'Ekleniyor...' : 'Ekle'}</span>
          </button>
        </div>
      </form>

      {/* Streamlined Filter & Search Row */}
      <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
        {/* Date Tabs */}
        <div className="flex items-center gap-1 bg-white/[0.03] p-1 rounded-2xl border border-white/[0.06] overflow-x-auto">
          {[
            { id: 'TODAY', label: 'Bugün', count: todayCount },
            { id: 'FUTURE', label: 'Gelecek', count: futureCount },
            { id: 'PAST', label: 'Geçmiş', count: pastCount },
            { id: 'ALL', label: 'Tümü', count: totalTasks },
          ].map((tab) => {
            const active = dateFilter === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setDateFilter(tab.id)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all whitespace-nowrap ${active
                    ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-gray-400 hover:text-white'
                  }`}
              >
                <span>{tab.label}</span>
                <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-white/5 text-gray-400">
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
          {/* Status Pills */}
          <div className="flex items-center gap-1 bg-white/[0.03] p-1 rounded-xl border border-white/[0.06]">
            {[
              { id: 'ALL', label: 'Tümü' },
              { id: 'PENDING', label: 'Bekleyen' },
              { id: 'COMPLETED', label: 'Biten' },
            ].map((s) => (
              <button
                key={s.id}
                onClick={() => setFilterStatus(s.id)}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-all ${filterStatus === s.id
                    ? 'bg-white/10 text-white'
                    : 'text-gray-400 hover:text-white'
                  }`}
              >
                {s.label}
              </button>
            ))}
          </div>

          {/* Search */}
          <div className="relative sm:w-52">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Ara..."
              className="w-full pl-8 pr-3 py-1.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 outline-none focus:border-indigo-500/40"
            />
          </div>
        </div>
      </div>

      {/* Task List */}
      <div className="space-y-2.5">
        {filteredTasks.length === 0 ? (
          <div className="glass-panel rounded-3xl p-10 text-center border border-white/5 space-y-2">
            <CheckSquare className="w-8 h-8 text-gray-600 mx-auto" />
            <h3 className="text-sm font-bold text-gray-200">
              {searchQuery ? 'Eşleşen görev bulunamadı' : 'Bu görünümde görev yok'}
            </h3>
            <p className="text-xs text-gray-500">
              Yukarıdaki alandan hemen yeni bir görev ekleyebilirsiniz.
            </p>
          </div>
        ) : (
          filteredTasks.map((task) => {
            const isCompleted = task.status === 'COMPLETED';
            return (
              <div
                key={task.id}
                className={`group glass-panel rounded-2xl p-4 border transition-all flex items-start justify-between gap-4 ${isCompleted
                    ? 'border-white/5 opacity-65'
                    : 'border-white/10 hover:border-indigo-500/30'
                  }`}
              >
                <div className="flex items-start gap-3.5 min-w-0">
                  <button
                    type="button"
                    onClick={() => handleToggleTask(task)}
                    title={isCompleted ? 'Bekleyene al' : 'Tamamlandı olarak işaretle'}
                    className="pt-0.5 text-gray-400 hover:text-emerald-400 transition-colors shrink-0"
                  >
                    {isCompleted ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                    ) : (
                      <Circle className="w-5 h-5" />
                    )}
                  </button>

                  <div className="min-w-0">
                    <h3
                      className={`text-sm font-semibold break-words ${isCompleted ? 'line-through text-gray-400' : 'text-white'
                        }`}
                    >
                      {task.title}
                    </h3>

                    {task.description && (
                      <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                        {task.description}
                      </p>
                    )}

                    <div className="flex flex-wrap items-center gap-2 mt-2">
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-white/5 text-gray-300">
                        {TYPE_LABELS[task.type] || task.type || 'Görev'}
                      </span>
                      {task.startTime && (
                        <span className="text-[11px] text-gray-400 flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {new Date(task.startTime).toLocaleDateString('tr-TR', {
                            day: 'numeric',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => handleDeleteTask(task.id)}
                  title="Görevi Sil"
                  className="p-1.5 rounded-lg text-gray-500 hover:text-rose-400 hover:bg-rose-500/10 opacity-70 sm:opacity-0 sm:group-hover:opacity-100 transition-all shrink-0"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
