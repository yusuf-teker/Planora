import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  getHealthData,
  pushHealthData,
  analyzeFoodAi,
  analyzeWorkoutAi,
  seedSampleHealthData,
} from '../api/client';
import {
  Flame,
  Activity,
  Utensils,
  Scale,
  Printer,
  Download,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Sparkles,
  TrendingDown,
  TrendingUp,
  Trash2,
  Plus,
  Calendar,
} from 'lucide-react';

function getLast7Days() {
  const days = [];
  const trDays = ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'];
  const now = new Date();
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    const iso = d.toISOString().split('T')[0];
    days.push({
      iso,
      dayName: i === 0 ? 'Bugün' : trDays[d.getDay()],
      dayNum: d.getDate(),
    });
  }
  return days;
}

function parseFoodItems(itemsJson) {
  if (!itemsJson) return [];
  try {
    const parsed = typeof itemsJson === 'string' ? JSON.parse(itemsJson) : itemsJson;
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

export default function HealthTrackView() {
  const { token, user } = useAuth();
  const cacheKey = user?.id ? `health_data_cache_${user.id}` : 'health_data_cache_default';
  const todayStr = new Date().toISOString().split('T')[0];
  const weekDays = useMemo(() => getLast7Days(), []);

  const [healthData, setHealthData] = useState(() => {
    try {
      const cached = localStorage.getItem(cacheKey);
      if (cached) return JSON.parse(cached);
    } catch (_) { }
    return { foods: [], goal: null, workouts: [], weights: [] };
  });

  const [loading, setLoading] = useState(
    healthData.foods.length === 0 && healthData.workouts.length === 0
  );
  const [seeding, setSeeding] = useState(false);
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [activeTab, setActiveTab] = useState('foods'); // 'foods', 'workouts', 'weights'
  const [selectedDate, setSelectedDate] = useState(todayStr);

  // Interactive composer states
  const [foodInput, setFoodInput] = useState('');
  const [addingFood, setAddingFood] = useState(false);
  const [workoutInput, setWorkoutInput] = useState('');
  const [addingWorkout, setAddingWorkout] = useState(false);
  const [weightInput, setWeightInput] = useState('');
  const [weightNote, setWeightNote] = useState('');
  const [addingWeight, setAddingWeight] = useState(false);

  const persistCache = (parsedData) => {
    setHealthData(parsedData);
    try {
      localStorage.setItem(cacheKey, JSON.stringify(parsedData));
    } catch (_) { }
  };

  const loadData = async (isManualRefresh = false) => {
    if (isManualRefresh || (healthData.foods.length === 0 && healthData.workouts.length === 0)) {
      setLoading(true);
    }
    setError('');
    try {
      const data = await getHealthData(token, 0);
      const parsedData = {
        foods: (data.foods || []).filter((f) => !f.isDeleted),
        goal: data.nutritionGoals || data.goal || null,
        workouts: (data.workouts || []).filter((w) => !w.isDeleted),
        weights: (data.weights || []).filter((wt) => !wt.isDeleted),
      };
      persistCache(parsedData);
    } catch (err) {
      console.error('Failed to load health data:', err);
      if (healthData.foods.length === 0 && healthData.workouts.length === 0) {
        setError(err.message || 'Sağlık verileri yüklenirken bir hata oluştu.');
      } else {
        setInfoMessage('Önbellekteki veriler gösteriliyor (çevrimdışı mod).');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) loadData(false);
  }, [token]);

  // Derived Stats for selectedDate
  const targetCalories = healthData.goal?.calories || 2150;
  const targetProtein = healthData.goal?.proteinG || 135;
  const targetCarbs = healthData.goal?.carbsG || 210;
  const targetFat = healthData.goal?.fatG || 65;

  const dateFoods = useMemo(
    () => healthData.foods.filter((f) => f.date === selectedDate),
    [healthData.foods, selectedDate]
  );
  const dateWorkouts = useMemo(
    () => healthData.workouts.filter((w) => w.date === selectedDate),
    [healthData.workouts, selectedDate]
  );

  const totalCaloriesDate = dateFoods.reduce((acc, f) => acc + (f.totalCalories || 0), 0);
  const burnedCaloriesDate = dateWorkouts.reduce((acc, w) => acc + (w.caloriesBurned || 0), 0);
  const workoutMinutesDate = dateWorkouts.reduce((acc, w) => acc + (w.durationMinutes || 0), 0);
  const remainingCalories = Math.max(0, targetCalories - totalCaloriesDate);
  const calProgress = Math.min(1, totalCaloriesDate / (targetCalories || 1));

  let dateProtein = 0;
  let dateCarbs = 0;
  let dateFat = 0;
  dateFoods.forEach((food) => {
    parseFoodItems(food.itemsJson).forEach((it) => {
      dateProtein += it.proteinG ?? it.protein ?? 0;
      dateCarbs += it.carbsG ?? it.carbs ?? 0;
      dateFat += it.fatG ?? it.fat ?? 0;
    });
  });

  const sortedWeights = useMemo(
    () => [...healthData.weights].sort((a, b) => (b.date || '').localeCompare(a.date || '')),
    [healthData.weights]
  );
  const latestWeight = sortedWeights[0]?.weightKg || null;
  const previousWeight = sortedWeights[1]?.weightKg || null;
  const weightChange =
    latestWeight && previousWeight ? (latestWeight - previousWeight).toFixed(1) : null;

  // Mini SVG sparkline points for last 8 weights
  const sparklineWeights = useMemo(() => [...sortedWeights].slice(0, 8).reverse(), [sortedWeights]);

  // Handlers
  const handleAddFoodAi = async (e) => {
    e.preventDefault();
    if (!foodInput.trim()) return;
    setAddingFood(true);
    setError('');
    setInfoMessage('');
    try {
      const aiRes = await analyzeFoodAi(token, foodInput.trim(), selectedDate);
      const now = Date.now();
      const newFood = {
        id: `web_food_${now}_${Math.random().toString(36).slice(2, 6)}`,
        date: selectedDate,
        rawText: foodInput.trim(),
        summary: aiRes.summary || foodInput.trim(),
        totalCalories: aiRes.totalCalories || 0,
        itemsJson: JSON.stringify(aiRes.items || []),
        updatedAt: now,
        isDeleted: false,
      };
      await pushHealthData(token, { foods: [newFood] });
      persistCache({ ...healthData, foods: [newFood, ...healthData.foods] });
      setFoodInput('');
      setInfoMessage(`"${newFood.summary}" (${newFood.totalCalories} kcal) eklendi.`);
    } catch (err) {
      setError(err.message || 'Öğün analiz edilirken hata oluştu.');
    } finally {
      setAddingFood(false);
    }
  };

  const handleAddWorkoutAi = async (e) => {
    e.preventDefault();
    if (!workoutInput.trim()) return;
    setAddingWorkout(true);
    setError('');
    setInfoMessage('');
    try {
      const aiRes = await analyzeWorkoutAi(token, workoutInput.trim(), latestWeight, selectedDate);
      const now = Date.now();
      const newWorkout = {
        id: `web_workout_${now}_${Math.random().toString(36).slice(2, 6)}`,
        date: selectedDate,
        rawText: workoutInput.trim(),
        caloriesBurned: aiRes.caloriesBurned || 0,
        durationMinutes: aiRes.durationMinutes || 0,
        targetCalories: 350,
        targetDurationMinutes: 45,
        aiAnalysis: aiRes.summary || '',
        updatedAt: now,
        isDeleted: false,
      };
      await pushHealthData(token, { workouts: [newWorkout] });
      persistCache({ ...healthData, workouts: [newWorkout, ...healthData.workouts] });
      setWorkoutInput('');
      setInfoMessage(`Antrenman kaydedildi (${newWorkout.caloriesBurned} kcal yakıldı).`);
    } catch (err) {
      setError(err.message || 'Antrenman analiz edilirken hata oluştu.');
    } finally {
      setAddingWorkout(false);
    }
  };

  const handleAddWeight = async (e) => {
    e.preventDefault();
    const kg = parseFloat(String(weightInput).replace(',', '.'));
    if (isNaN(kg) || kg <= 20 || kg >= 350) {
      setError('Lütfen geçerli bir kilo değeri girin (kg).');
      return;
    }
    setAddingWeight(true);
    setError('');
    setInfoMessage('');
    try {
      const now = Date.now();
      const newWeight = {
        id: `web_weight_${now}_${Math.random().toString(36).slice(2, 6)}`,
        date: selectedDate,
        weightKg: kg,
        targetWeightKg: sortedWeights[0]?.targetWeightKg || null,
        note: weightNote.trim() || null,
        updatedAt: now,
        isDeleted: false,
      };
      await pushHealthData(token, { weights: [newWeight] });
      persistCache({ ...healthData, weights: [newWeight, ...healthData.weights] });
      setWeightInput('');
      setWeightNote('');
      setInfoMessage(`${kg} kg tartı kaydı eklendi.`);
    } catch (err) {
      setError(err.message || 'Kilo kaydedilirken hata oluştu.');
    } finally {
      setAddingWeight(false);
    }
  };

  const handleDeleteItem = async (type, item) => {
    const now = Date.now();
    const deletedItem = { ...item, isDeleted: true, updatedAt: now };
    try {
      if (type === 'food') {
        persistCache({ ...healthData, foods: healthData.foods.filter((f) => f.id !== item.id) });
        await pushHealthData(token, { foods: [deletedItem] });
      } else if (type === 'workout') {
        persistCache({
          ...healthData,
          workouts: healthData.workouts.filter((w) => w.id !== item.id),
        });
        await pushHealthData(token, { workouts: [deletedItem] });
      } else if (type === 'weight') {
        persistCache({
          ...healthData,
          weights: healthData.weights.filter((wt) => wt.id !== item.id),
        });
        await pushHealthData(token, { weights: [deletedItem] });
      }
    } catch (err) {
      setError(err.message || 'Kayıt silinemedi.');
      loadData(true);
    }
  };

  const handleSeedData = async () => {
    setSeeding(true);
    setError('');
    try {
      await seedSampleHealthData(token, selectedDate);
      await loadData(true);
      setInfoMessage('Örnek sağlık verileri yüklendi.');
    } catch (err) {
      setError(err.message || 'Örnek veriler yüklenirken hata oluştu.');
    } finally {
      setSeeding(false);
    }
  };

  const handleExportCSV = () => {
    let csv = 'Tür,Tarih,Açıklama,Değer\n';
    healthData.foods.forEach((f) => {
      csv += `Öğün,"${f.date}","${(f.summary || f.rawText || '').replace(/"/g, '""')}",${f.totalCalories || 0} kcal\n`;
    });
    healthData.workouts.forEach((w) => {
      csv += `Antrenman,"${w.date}","${(w.rawText || '').replace(/"/g, '""')}",${w.caloriesBurned || 0} kcal\n`;
    });
    healthData.weights.forEach((wt) => {
      csv += `Kilo,"${wt.date}","Tartı",${wt.weightKg} kg\n`;
    });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `HealthTrack_Rapor_${selectedDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const isDataEmpty =
    healthData.foods.length === 0 &&
    healthData.workouts.length === 0 &&
    healthData.weights.length === 0;

  // SVG Ring Constants
  const ringRadius = 54;
  const ringCircumference = 2 * Math.PI * ringRadius;
  const ringOffset = ringCircumference * (1 - calProgress);

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-7 space-y-6">
      {/* Header Row */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">HealthTrack</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            Günlük beslenme, antrenman ve vücut takibi
          </p>
        </div>

        <div className="flex items-center gap-2 no-print">
          <button
            onClick={() => window.print()}
            className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-gray-300 hover:text-white transition-all flex items-center gap-1.5"
          >
            <Printer className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Yazdır</span>
          </button>
          <button
            onClick={handleExportCSV}
            className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-gray-300 hover:text-white transition-all flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">CSV</span>
          </button>
          <button
            onClick={() => loadData(true)}
            disabled={loading}
            title="Senkronize Et"
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* 7-Day Strip Selector (App-style) */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 no-print">
        {weekDays.map((day) => {
          const isSelected = selectedDate === day.iso;
          return (
            <button
              key={day.iso}
              onClick={() => setSelectedDate(day.iso)}
              className={`flex-1 min-w-[64px] py-2.5 px-3 rounded-2xl border text-center transition-all ${isSelected
                  ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300 shadow-sm'
                  : 'bg-white/[0.03] border-white/[0.06] text-gray-400 hover:text-white hover:bg-white/[0.06]'
                }`}
            >
              <div className="text-[10px] font-semibold uppercase">{day.dayName}</div>
              <div className={`text-sm font-bold mt-0.5 ${isSelected ? 'text-white' : ''}`}>
                {day.dayNum}
              </div>
            </button>
          );
        })}

        {/* Custom Date Picker */}
        <label className="flex items-center gap-1.5 px-3 py-2.5 rounded-2xl bg-white/[0.03] border border-white/[0.06] text-gray-400 hover:text-white cursor-pointer shrink-0">
          <Calendar className="w-4 h-4 text-emerald-400" />
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="bg-transparent text-xs text-white outline-none cursor-pointer w-[108px]"
          />
        </label>
      </div>

      {/* Notifications */}
      {infoMessage && (
        <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/25 text-emerald-300 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{infoMessage}</span>
          </div>
          <button onClick={() => setInfoMessage('')} className="hover:text-white">
            Kapat
          </button>
        </div>
      )}
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

      {/* Empty Seed Banner */}
      {isDataEmpty && !loading && (
        <div className="glass-panel rounded-3xl p-6 border border-emerald-500/25 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-emerald-400 text-xs font-bold">
              <Sparkles className="w-4 h-4" />
              <span>Başlangıç Verisi</span>
            </div>
            <p className="text-xs text-gray-300">
              Aşağıdan kendi öğün/antrenmanınızı ekleyebilir veya örnek verilerle hemen deneyebilirsiniz.
            </p>
          </div>
          <button
            onClick={handleSeedData}
            disabled={seeding}
            className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-[#0B0F19] text-xs font-bold transition-all shrink-0 disabled:opacity-50"
          >
            {seeding ? 'Yükleniyor...' : 'Örnek Veri Yükle'}
          </button>
        </div>
      )}

      {/* Hero Bento: 2 Clean Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Card 1: Concentric Calorie Gauge + Macros */}
        <div className="lg:col-span-7 glass-panel rounded-3xl p-6 border border-emerald-500/20 flex flex-col sm:flex-row items-center gap-6">
          {/* Concentric Ring */}
          <div className="relative w-36 h-36 shrink-0 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 128 128">
              <circle
                cx="64"
                cy="64"
                r={ringRadius}
                fill="none"
                stroke="rgba(255,255,255,0.06)"
                strokeWidth="10"
              />
              <circle
                cx="64"
                cy="64"
                r={ringRadius}
                fill="none"
                stroke={totalCaloriesDate > targetCalories ? '#F97316' : '#10B981'}
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={ringCircumference}
                strokeDashoffset={ringOffset}
                className="transition-all duration-500"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
              <span className="text-2xl font-black text-white leading-none">
                {remainingCalories}
              </span>
              <span className="text-[10px] text-gray-400 mt-1 font-medium">kcal kaldı</span>
              <span className="text-[10px] text-emerald-400 font-semibold mt-0.5">
                {totalCaloriesDate} / {targetCalories}
              </span>
            </div>
          </div>

          {/* Macro Capsules */}
          <div className="flex-1 w-full space-y-3">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-gray-300">Günlük Makrolar</span>
              {burnedCaloriesDate > 0 && (
                <span className="inline-flex items-center gap-1 text-[11px] text-orange-300 bg-orange-500/10 px-2 py-0.5 rounded-full border border-orange-500/20">
                  <Flame className="w-3 h-3" />
                  -{burnedCaloriesDate} kcal yakıldı
                </span>
              )}
            </div>

            <MacroBar label="Protein" value={dateProtein} target={targetProtein} color="bg-indigo-400" />
            <MacroBar label="Karbonhidrat" value={dateCarbs} target={targetCarbs} color="bg-amber-400" />
            <MacroBar label="Yağ" value={dateFat} target={targetFat} color="bg-rose-400" />
          </div>
        </div>

        {/* Card 2: Weight & Activity Telemetry */}
        <div className="lg:col-span-5 glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between gap-4">
          <div className="flex items-start justify-between">
            <div>
              <span className="text-[11px] font-semibold uppercase tracking-wider text-gray-400">
                Güncel Kilo
              </span>
              <div className="flex items-baseline gap-1.5 mt-1">
                <span className="text-3xl font-black text-white">
                  {latestWeight ?? '--'}
                </span>
                <span className="text-xs text-gray-400">kg</span>
                {weightChange !== null && (
                  <span
                    className={`ml-2 inline-flex items-center gap-0.5 text-[11px] font-semibold px-2 py-0.5 rounded-full ${Number(weightChange) <= 0
                        ? 'bg-emerald-500/15 text-emerald-300'
                        : 'bg-orange-500/15 text-orange-300'
                      }`}
                  >
                    {Number(weightChange) <= 0 ? (
                      <TrendingDown className="w-3 h-3" />
                    ) : (
                      <TrendingUp className="w-3 h-3" />
                    )}
                    {Math.abs(weightChange)} kg
                  </span>
                )}
              </div>
            </div>

            {/* Mini Sparkline */}
            {sparklineWeights.length >= 2 && <WeightSparkline points={sparklineWeights} />}
          </div>

          <div className="pt-3 border-t border-white/5 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-gray-400">
              <Activity className="w-4 h-4 text-indigo-400" />
              <span>Seçili gün aktivitesi</span>
            </div>
            <span className="font-semibold text-white">
              {workoutMinutesDate} dk • {burnedCaloriesDate} kcal
            </span>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex items-center gap-1.5 bg-white/[0.03] p-1 rounded-2xl border border-white/[0.06] w-fit no-print">
        {[
          { id: 'foods', label: `Öğünler (${dateFoods.length})`, icon: Utensils },
          { id: 'workouts', label: `Antrenman (${dateWorkouts.length})`, icon: Activity },
          { id: 'weights', label: `Kilo (${sortedWeights.length})`, icon: Scale },
        ].map((tab) => {
          const Icon = tab.icon;
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all ${active
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                  : 'text-gray-400 hover:text-white'
                }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* TAB 1: FOODS + AI QUICK ADD */}
      {activeTab === 'foods' && (
        <div className="space-y-4">
          <form onSubmit={handleAddFoodAi} className="glass-panel rounded-2xl p-3 border border-emerald-500/20 flex flex-col sm:flex-row gap-2.5 no-print">
            <input
              type="text"
              value={foodInput}
              onChange={(e) => setFoodInput(e.target.value)}
              placeholder="Ne yediniz? (Örn: 2 haşlanmış yumurta, 1 dilim tam buğday ekmeği, 5 zeytin)"
              className="flex-1 px-3.5 py-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 focus:border-emerald-500/50 outline-none"
            />
            <button
              type="submit"
              disabled={addingFood || !foodInput.trim()}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-[#0B0F19] text-xs font-bold flex items-center justify-center gap-1.5 hover:opacity-95 disabled:opacity-50 shrink-0"
            >
              <Sparkles className={`w-3.5 h-3.5 ${addingFood ? 'animate-spin' : ''}`} />
              <span>{addingFood ? 'Hesaplanıyor...' : 'AI ile Ekle'}</span>
            </button>
          </form>

          {dateFoods.length === 0 ? (
            <EmptyCard
              icon={Utensils}
              title={`${selectedDate} tarihinde öğün yok`}
              subtitle="Yukarıdaki alana yediklerinizi yazarak yapay zeka ile anında kalori ve makro hesabı yapabilirsiniz."
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dateFoods.map((food) => {
                const items = parseFoodItems(food.itemsJson);
                return (
                  <div
                    key={food.id}
                    className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-emerald-500/30 transition-all flex flex-col justify-between gap-3"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="space-y-1">
                        <h3 className="text-sm font-bold text-white">
                          {food.summary || 'Öğün Kaydı'}
                        </h3>
                        <p className="text-xs text-gray-400">{food.rawText}</p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="text-sm font-extrabold text-emerald-400">
                          {food.totalCalories} kcal
                        </span>
                        <button
                          onClick={() => handleDeleteItem('food', food)}
                          title="Sil"
                          className="p-1.5 rounded-lg text-gray-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors no-print"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {items.length > 0 && (
                      <div className="pt-2.5 border-t border-white/5 space-y-1">
                        {items.map((it, idx) => (
                          <div key={idx} className="flex items-center justify-between text-[11px] text-gray-400">
                            <span>{it.name}</span>
                            <span className="font-mono text-gray-300">
                              {it.calories} kcal • P:{Math.round(it.proteinG ?? it.protein ?? 0)}g
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: WORKOUTS + AI QUICK ADD */}
      {activeTab === 'workouts' && (
        <div className="space-y-4">
          <form onSubmit={handleAddWorkoutAi} className="glass-panel rounded-2xl p-3 border border-indigo-500/20 flex flex-col sm:flex-row gap-2.5 no-print">
            <input
              type="text"
              value={workoutInput}
              onChange={(e) => setWorkoutInput(e.target.value)}
              placeholder="Hangi egzersizi yaptınız? (Örn: 40 dk tempolu yürüyüş veya 30 dk yüzme)"
              className="flex-1 px-3.5 py-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 focus:border-indigo-500/50 outline-none"
            />
            <button
              type="submit"
              disabled={addingWorkout || !workoutInput.trim()}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-500 to-violet-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 hover:opacity-95 disabled:opacity-50 shrink-0"
            >
              <Sparkles className={`w-3.5 h-3.5 ${addingWorkout ? 'animate-spin' : ''}`} />
              <span>{addingWorkout ? 'Hesaplanıyor...' : 'AI ile Ekle'}</span>
            </button>
          </form>

          {dateWorkouts.length === 0 ? (
            <EmptyCard
              icon={Activity}
              title={`${selectedDate} tarihinde antrenman yok`}
              subtitle="Yaptığınız aktiviteyi yazın, AI süresini ve tahmini yakılan kaloriyi hesaplayıp kaydetsin."
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dateWorkouts.map((w) => (
                <div
                  key={w.id}
                  className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-indigo-500/30 transition-all space-y-2.5"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-sm font-bold text-white">{w.rawText}</h3>
                      <p className="text-xs text-gray-400 mt-1">
                        {w.durationMinutes} dakika • {w.caloriesBurned} kcal yakıldı
                      </p>
                    </div>
                    <button
                      onClick={() => handleDeleteItem('workout', w)}
                      title="Sil"
                      className="p-1.5 rounded-lg text-gray-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors no-print"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                  {w.aiAnalysis && (
                    <p className="text-[11px] text-indigo-300/80 bg-indigo-500/10 px-3 py-2 rounded-xl border border-indigo-500/15">
                      {w.aiAnalysis}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: WEIGHTS + QUICK ADD */}
      {activeTab === 'weights' && (
        <div className="space-y-4">
          <form onSubmit={handleAddWeight} className="glass-panel rounded-2xl p-3 border border-teal-500/20 flex flex-col sm:flex-row gap-2.5 no-print">
            <input
              type="number"
              step="0.1"
              value={weightInput}
              onChange={(e) => setWeightInput(e.target.value)}
              placeholder="Kilo (Örn: 74.5)"
              className="sm:w-40 px-3.5 py-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 focus:border-teal-500/50 outline-none"
            />
            <input
              type="text"
              value={weightNote}
              onChange={(e) => setWeightNote(e.target.value)}
              placeholder="Not (İsteğe bağlı, örn: Sabah aç karnına)"
              className="flex-1 px-3.5 py-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-gray-500 focus:border-teal-500/50 outline-none"
            />
            <button
              type="submit"
              disabled={addingWeight || !weightInput}
              className="px-4 py-2.5 rounded-xl bg-teal-500 hover:bg-teal-400 text-[#0B0F19] text-xs font-bold flex items-center justify-center gap-1.5 disabled:opacity-50 shrink-0"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>{addingWeight ? 'Kaydediliyor...' : 'Kilo Ekle'}</span>
            </button>
          </form>

          {sortedWeights.length === 0 ? (
            <EmptyCard
              icon={Scale}
              title="Henüz kilo kaydı yok"
              subtitle="İlk ölçümünüzü yukarıdan ekleyerek kilo değişim grafiğinizi başlatabilirsiniz."
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {sortedWeights.map((wt) => (
                <div
                  key={wt.id}
                  className="glass-panel rounded-2xl p-4 border border-white/10 flex items-center justify-between"
                >
                  <div>
                    <div className="text-lg font-extrabold text-white">{wt.weightKg} kg</div>
                    <div className="text-[11px] text-gray-400 mt-0.5">
                      {wt.date} {wt.note ? `• ${wt.note}` : ''}
                    </div>
                  </div>
                  <button
                    onClick={() => handleDeleteItem('weight', wt)}
                    title="Sil"
                    className="p-1.5 rounded-lg text-gray-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors no-print"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function MacroBar({ label, value, target, color }) {
  const pct = Math.min(100, Math.round((value / (target || 1)) * 100));
  return (
    <div>
      <div className="flex justify-between text-[11px] mb-1">
        <span className="text-gray-400">{label}</span>
        <span className="text-gray-200 font-mono">
          {Math.round(value)}g <span className="text-gray-500">/ {target}g</span>
        </span>
      </div>
      <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
        <div className={`${color} h-full rounded-full transition-all duration-500`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function WeightSparkline({ points }) {
  const values = points.map((p) => p.weightKg);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const width = 110;
  const height = 38;
  const coords = values
    .map((val, idx) => {
      const x = (idx / (values.length - 1)) * (width - 8) + 4;
      const y = height - 4 - ((val - min) / range) * (height - 8);
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <svg width={width} height={height} className="overflow-visible">
      <polyline
        fill="none"
        stroke="#10B981"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={coords}
      />
    </svg>
  );
}

function EmptyCard({ icon: Icon, title, subtitle }) {
  return (
    <div className="glass-panel rounded-3xl p-10 text-center border border-white/5 space-y-2">
      <Icon className="w-8 h-8 text-gray-600 mx-auto" />
      <h3 className="text-sm font-bold text-gray-200">{title}</h3>
      <p className="text-xs text-gray-500 max-w-md mx-auto">{subtitle}</p>
    </div>
  );
}
