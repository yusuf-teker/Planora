import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getHealthData, getNutritionGoals } from '../api/client';
import { 
  Heart, 
  Flame, 
  Activity, 
  Utensils, 
  Scale, 
  Calendar, 
  Printer, 
  Download, 
  RefreshCw, 
  AlertCircle, 
  CheckCircle2, 
  ChevronRight,
  Sparkles,
  TrendingDown,
  TrendingUp,
  Clock,
  ShieldCheck
} from 'lucide-react';

export default function HealthTrackView() {
  const { token, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [healthData, setHealthData] = useState({
    foods: [],
    goal: null,
    workouts: [],
    weights: [],
  });

  const [activeTab, setActiveTab] = useState('foods'); // 'foods', 'workouts', 'weights', 'report'
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getHealthData(token, 0);
      setHealthData({
        foods: (data.foods || []).filter(f => !f.isDeleted),
        goal: data.nutritionGoals || data.goal || null,
        workouts: (data.workouts || []).filter(w => !w.isDeleted),
        weights: (data.weights || []).filter(wt => !wt.isDeleted),
      });
    } catch (err) {
      console.error('Failed to load health data:', err);
      setError(err.message || 'Sağlık verileri yüklenirken bir hata oluştu.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) loadData();
  }, [token]);

  // Derived Stats
  const targetCalories = healthData.goal?.calories || 2000;
  const targetProtein = healthData.goal?.proteinG || 120;
  const targetCarbs = healthData.goal?.carbsG || 220;
  const targetFat = healthData.goal?.fatG || 65;

  // Selected date food stats
  const dateFoods = healthData.foods.filter(f => f.date === selectedDate);
  const totalCaloriesDate = dateFoods.reduce((acc, f) => acc + (f.totalCalories || 0), 0);

  // Parse items from dateFoods to compute date macros
  let dateProtein = 0;
  let dateCarbs = 0;
  let dateFat = 0;

  dateFoods.forEach(food => {
    if (food.itemsJson) {
      try {
        const items = typeof food.itemsJson === 'string' ? JSON.parse(food.itemsJson) : food.itemsJson;
        if (Array.isArray(items)) {
          items.forEach(it => {
            dateProtein += it.protein || it.proteinG || 0;
            dateCarbs += it.carbs || it.carbsG || 0;
            dateFat += it.fat || it.fatG || 0;
          });
        }
      } catch (_) {}
    }
  });

  // Latest weight
  const sortedWeights = [...healthData.weights].sort((a, b) => (b.date || '').localeCompare(a.date || ''));
  const latestWeight = sortedWeights[0]?.weightKg || null;
  const previousWeight = sortedWeights[1]?.weightKg || null;
  const weightChange = (latestWeight && previousWeight) ? (latestWeight - previousWeight).toFixed(1) : null;

  // Total workouts burn
  const totalWorkoutBurn = healthData.workouts.reduce((acc, w) => acc + (w.caloriesBurned || 0), 0);
  const totalWorkoutMinutes = healthData.workouts.reduce((acc, w) => acc + (w.durationMinutes || 0), 0);

  // Export CSV
  const handleExportCSV = () => {
    let csv = 'Tür,Tarih,Açıklama / Detay,Kalori / Değer\n';
    healthData.foods.forEach(f => {
      csv += `Öğün,"${f.date}","${(f.summary || f.rawText || '').replace(/"/g, '""')}",${f.totalCalories || 0}\n`;
    });
    healthData.workouts.forEach(w => {
      csv += `Antrenman,"${w.date}","${(w.rawText || '').replace(/"/g, '""')}",${w.caloriesBurned || 0} kcal (${w.durationMinutes || 0} dk)\n`;
    });
    healthData.weights.forEach(wt => {
      csv += `Kilo,"${wt.date}","Tartı Kaydı",${wt.weightKg} kg\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `HealthTrack_Rapor_${user?.username || 'user'}_${selectedDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header & Date Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Heart className="w-4 h-4" />
            </div>
            <h1 className="text-2xl font-extrabold text-white tracking-tight">
              HealthTrack Biyo-Telemetri Portalı
            </h1>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            Günlük besin dökümü, kalori hedefleri, antrenmanlar ve klinik randevu geçmişiniz
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-xl border border-white/10">
            <span className="text-xs text-gray-400">Tarih:</span>
            <input 
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-transparent text-xs text-white outline-none cursor-pointer"
            />
          </div>

          <button
            onClick={loadData}
            title="Verileri Yenile"
            disabled={loading}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
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

      {/* Telemetry Hero Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        
        {/* Metric 1: Calories Ring */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-emerald-500/20">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
              Günlük Kalori
            </span>
            <Flame className="w-4 h-4 text-orange-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {totalCaloriesDate}
            </span>
            <span className="text-xs text-gray-400 font-medium">
              / {targetCalories} kcal
            </span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-white/5 h-2.5 rounded-full mt-4 overflow-hidden p-0.5 border border-white/5">
            <div 
              className={`h-full rounded-full transition-all duration-500 ${
                totalCaloriesDate > targetCalories ? 'bg-orange-500' : 'bg-gradient-to-r from-emerald-500 to-teal-400'
              }`}
              style={{ width: `${Math.min(100, Math.round((totalCaloriesDate / (targetCalories || 1)) * 100))}%` }}
            />
          </div>

          <div className="flex justify-between items-center text-[11px] text-gray-400 mt-2">
            <span>Kalan: {Math.max(0, targetCalories - totalCaloriesDate)} kcal</span>
            <span>%{Math.round((totalCaloriesDate / (targetCalories || 1)) * 100)}</span>
          </div>
        </div>

        {/* Metric 2: Macros */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-white/10">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-teal-400">
              Makro Dağılımı ({selectedDate})
            </span>
            <Utensils className="w-4 h-4 text-teal-400" />
          </div>

          <div className="space-y-2 mt-2">
            {/* Protein */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-orange-400 font-semibold">Protein</span>
                <span className="text-gray-300 font-mono">{Math.round(dateProtein)}g / {targetProtein}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-orange-500 h-full rounded-full"
                  style={{ width: `${Math.min(100, Math.round((dateProtein / (targetProtein || 1)) * 100))}%` }}
                />
              </div>
            </div>

            {/* Carbs */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-cyan-400 font-semibold">Karbonhidrat</span>
                <span className="text-gray-300 font-mono">{Math.round(dateCarbs)}g / {targetCarbs}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-cyan-500 h-full rounded-full"
                  style={{ width: `${Math.min(100, Math.round((dateCarbs / (targetCarbs || 1)) * 100))}%` }}
                />
              </div>
            </div>

            {/* Fat */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-amber-400 font-semibold">Yağ</span>
                <span className="text-gray-300 font-mono">{Math.round(dateFat)}g / {targetFat}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-amber-500 h-full rounded-full"
                  style={{ width: `${Math.min(100, Math.round((dateFat / (targetFat || 1)) * 100))}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Metric 3: Weight Status */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-white/10">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
              Güncel Tartı
            </span>
            <Scale className="w-4 h-4 text-emerald-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {latestWeight ? `${latestWeight}` : '--'}
            </span>
            <span className="text-sm text-gray-400 font-medium">kg</span>
          </div>

          <div className="mt-4 flex items-center gap-2">
            {weightChange !== null ? (
              <span className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full ${
                Number(weightChange) < 0 
                  ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' 
                  : 'bg-orange-500/15 text-orange-300 border border-orange-500/30'
              }`}>
                {Number(weightChange) < 0 ? <TrendingDown className="w-3 h-3" /> : <TrendingUp className="w-3 h-3" />}
                {Math.abs(weightChange)} kg ({sortedWeights[1]?.date ? 'önceki tartıya göre' : ''})
              </span>
            ) : (
              <span className="text-xs text-gray-500">Kilo kaydı bekleniyor</span>
            )}
          </div>

          <p className="text-[11px] text-gray-400 mt-2">
            Toplam {healthData.weights.length} tartı kaydı mevcut
          </p>
        </div>

        {/* Metric 4: Workouts & Burns */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-white/10">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
              Antrenman & Aktivite
            </span>
            <Activity className="w-4 h-4 text-indigo-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {totalWorkoutBurn}
            </span>
            <span className="text-xs text-gray-400 font-medium">kcal yakıldı</span>
          </div>

          <div className="mt-4 flex items-center justify-between text-xs text-gray-400">
            <span>Toplam Süre:</span>
            <span className="text-white font-semibold">{totalWorkoutMinutes} dakika</span>
          </div>

          <p className="text-[11px] text-gray-500 mt-2">
            Toplam {healthData.workouts.length} kayıtlı egzersiz
          </p>
        </div>

      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-2 overflow-x-auto">
        <button
          onClick={() => setActiveTab('foods')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'foods'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Utensils className="w-3.5 h-3.5" />
          <span>Öğün Günlüğü ({healthData.foods.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('workouts')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'workouts'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Activity className="w-3.5 h-3.5" />
          <span>Egzersizler ({healthData.workouts.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('weights')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'weights'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Scale className="w-3.5 h-3.5" />
          <span>Kilo Geçmişi ({healthData.weights.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('report')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'report'
              ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Printer className="w-3.5 h-3.5" />
          <span>Sağlık Raporu & Çıktı</span>
        </button>
      </div>

      {/* Tab 1: Food Logs Content */}
      {activeTab === 'foods' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
              Kayıtlı Öğünler ({dateFoods.length} öğün seçili tarihte: {selectedDate})
            </h2>
            <span className="text-xs text-gray-500">Tüm tarihlerde toplam {healthData.foods.length} öğün</span>
          </div>

          {dateFoods.length === 0 ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5">
              <Utensils className="w-8 h-8 text-gray-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-gray-300">Bu tarihte kayıtlı öğün bulunamadı.</p>
              <p className="text-xs text-gray-500 mt-1">HealthTrack mobil uygulamasından fotoğraf veya serbest metinle öğün ekleyebilirsiniz.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dateFoods.map((food) => {
                let parsedItems = [];
                if (food.itemsJson) {
                  try {
                    parsedItems = typeof food.itemsJson === 'string' ? JSON.parse(food.itemsJson) : food.itemsJson;
                  } catch (_) {}
                }

                return (
                  <div key={food.id} className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-emerald-500/30 transition-all">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          {food.date}
                        </span>
                        <h3 className="text-base font-bold text-white mt-1.5">
                          {food.summary || food.rawText}
                        </h3>
                        {food.summary && food.rawText && food.summary !== food.rawText && (
                          <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">"{food.rawText}"</p>
                        )}
                      </div>

                      <div className="text-right shrink-0">
                        <span className="text-lg font-black text-orange-400">
                          {food.totalCalories || 0}
                        </span>
                        <span className="text-[10px] text-gray-400 block -mt-1">kcal</span>
                      </div>
                    </div>

                    {/* Breakdown items */}
                    {Array.isArray(parsedItems) && parsedItems.length > 0 && (
                      <div className="mt-4 pt-3 border-t border-white/5 space-y-2">
                        {parsedItems.map((item, idx) => (
                          <div key={idx} className="flex items-center justify-between text-xs text-gray-300">
                            <span>• {item.name || item.foodName} ({item.amount || item.portion || '1 porsiyon'})</span>
                            <div className="flex items-center gap-3 font-mono text-[11px] text-gray-400">
                              <span>P: {Math.round(item.protein || item.proteinG || 0)}g</span>
                              <span>K: {Math.round(item.carbs || item.carbsG || 0)}g</span>
                              <span>Y: {Math.round(item.fat || item.fatG || 0)}g</span>
                              <span className="text-orange-400 font-semibold">{item.calories} kcal</span>
                            </div>
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

      {/* Tab 2: Workouts Content */}
      {activeTab === 'workouts' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
              Egzersiz & Spor Kayıtları
            </h2>
            <span className="text-xs text-gray-500">Toplam {healthData.workouts.length} kayıt</span>
          </div>

          {healthData.workouts.length === 0 ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5">
              <Activity className="w-8 h-8 text-gray-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-gray-300">Henüz kayıtlı egzersiz bulunmuyor.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {healthData.workouts.map((w) => (
                <div key={w.id} className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-indigo-500/30 transition-all">
                  <div className="flex items-center justify-between text-xs text-gray-400 mb-2">
                    <span className="font-semibold">{w.date}</span>
                    <span className="text-indigo-400 font-semibold">{w.durationMinutes || 0} dakika</span>
                  </div>

                  <h3 className="text-base font-bold text-white mb-2">
                    {w.rawText}
                  </h3>

                  <div className="flex items-center justify-between pt-3 border-t border-white/5 text-xs">
                    <span className="text-gray-400">Yakılan Enerji:</span>
                    <span className="text-base font-black text-orange-400">{w.caloriesBurned || 0} kcal</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Weights Content */}
      {activeTab === 'weights' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
              Tartı & Kilo Geçmişi
            </h2>
            <span className="text-xs text-gray-500">Toplam {healthData.weights.length} kayıt</span>
          </div>

          {sortedWeights.length === 0 ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5">
              <Scale className="w-8 h-8 text-gray-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-gray-300">Henüz kilo kaydı girilmemiş.</p>
            </div>
          ) : (
            <div className="glass-panel rounded-2xl border border-white/10 overflow-hidden">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-gray-400 uppercase font-semibold">
                    <th className="py-3.5 px-4">Tarih</th>
                    <th className="py-3.5 px-4">Kilo (kg)</th>
                    <th className="py-3.5 px-4">Notlar / Açıklama</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {sortedWeights.map((wt) => (
                    <tr key={wt.id} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 px-4 font-semibold text-white">{wt.date}</td>
                      <td className="py-3 px-4">
                        <span className="font-bold text-sm text-emerald-400">{wt.weightKg} kg</span>
                      </td>
                      <td className="py-3 px-4 text-gray-400">{wt.notes || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 4: Export / Printable Medical Report */}
      {activeTab === 'report' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between no-print">
            <div>
              <h2 className="text-base font-bold text-white">Diyetisyen & Doktor Rapor Görünümü</h2>
              <p className="text-xs text-gray-400">Bu raporu PDF olarak yazdırabilir veya CSV formatında indirebilirsiniz.</p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={handleExportCSV}
                className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-white flex items-center gap-2 transition-all"
              >
                <Download className="w-4 h-4 text-emerald-400" />
                <span>CSV Olarak İndir</span>
              </button>

              <button
                onClick={() => window.print()}
                className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-xs font-semibold text-white flex items-center gap-2 shadow-lg hover:shadow-glow-health transition-all"
              >
                <Printer className="w-4 h-4" />
                <span>Raporu Yazdır / PDF Kaydet</span>
              </button>
            </div>
          </div>

          {/* Printable Document Box */}
          <div className="glass-panel rounded-3xl p-8 border border-white/10 space-y-6 print:border-none print:p-0">
            <div className="border-b border-white/10 pb-4 flex justify-between items-start">
              <div>
                <h1 className="text-xl font-bold text-white">Bireysel Sağlık & Beslenme İlerleme Raporu</h1>
                <p className="text-xs text-gray-400 mt-1">
                  Kullanıcı: <strong className="text-white">{user?.name || user?.username}</strong> | Rapor Tarihi: {new Date().toLocaleDateString('tr-TR')}
                </p>
              </div>
              <div className="text-right text-xs text-gray-400">
                <span className="font-bold text-emerald-400">HealthTrack Ekosistemi</span>
                <p className="text-[10px]">Neon Cloud Database Verified</p>
              </div>
            </div>

            {/* Summary Grid */}
            <div className="grid grid-cols-3 gap-4 text-xs">
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">Güncel Kilo</span>
                <span className="text-base font-bold text-white">{latestWeight ? `${latestWeight} kg` : 'Veri yok'}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">Günlük Kalori Hedefi</span>
                <span className="text-base font-bold text-white">{targetCalories} kcal</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">Toplam Egzersiz Kaydı</span>
                <span className="text-base font-bold text-white">{healthData.workouts.length} seans</span>
              </div>
            </div>

            {/* Recent Foods Table */}
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-gray-300 mb-2">
                Son Kaydedilen Öğünler
              </h3>
              <div className="rounded-xl border border-white/10 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-white/5 text-gray-400">
                    <tr>
                      <th className="p-2.5">Tarih</th>
                      <th className="p-2.5">Öğün Özeti</th>
                      <th className="p-2.5 text-right">Kalori</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {healthData.foods.slice(0, 10).map((f) => (
                      <tr key={f.id}>
                        <td className="p-2.5 text-gray-400">{f.date}</td>
                        <td className="p-2.5 text-white font-medium">{f.summary || f.rawText}</td>
                        <td className="p-2.5 text-right text-orange-400 font-mono font-semibold">{f.totalCalories} kcal</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Doctor Note Disclaimer */}
            <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 text-[11px] text-gray-400">
              * Bu rapor, kullanıcının HealthTrack mobil uygulaması üzerinden kaydettiği biyo-telemetri ve besin tüketim verilerini içerir. Tıbbi tanı yerine geçmez, danışman hekim ve diyetisyen değerlendirmesi için hazırlanmıştır.
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

