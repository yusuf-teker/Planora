# PLANORA PROJECT FAST MAP

Bu dosya yapay zekanın arama yapmadan dosyaları anında bulmasını ve istekleri saniyeler içinde tamamlamasını sağlar.

## 1. Tema & Renk Sistemi (Theme & Styling)
- **Tema Tanımları & Renk Şemaları:** `core/src/commonMain/kotlin/com/yusufteker/planora/core/theme/PlanoraTheme.kt`
- **Renk Sabitleri (Palet & Degradeler):** `core/src/commonMain/kotlin/com/yusufteker/planora/core/theme/Color.kt`
- **Tema Veri Deposu (DataStore / Preferences):** `core/src/commonMain/kotlin/com/yusufteker/planora/core/preferences/ThemePreferences.kt`
- **Degrade Metin & Animasyonlu UI Bileşenleri:** `core/src/commonMain/kotlin/com/yusufteker/planora/core/ui/components/AnimatedComponents.kt`
- **Glassmorphism Kartlar:** `core/src/commonMain/kotlin/com/yusufteker/planora/core/ui/components/GlassCard.kt`

## 2. Ana Ekranlar (Presentation Screens)
- **Ayarlar Ekranı:** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/settings/SettingsScreen.kt`
  - ViewModel: `SettingsViewModel.kt`
- **Ana Ekran (Dashboard & Takvim):** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/home/HomeScreen.kt`
  - TopBar: `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/home/components/HomeTopBar.kt`
- **Plan Odaları (Plan Rooms):** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/plan_rooms/PlanRoomsScreen.kt`
  - Oda Detay: `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/plan_room_detail/PlanRoomDetailScreen.kt`
- **Notlar (Notes):** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/notes/NotesScreen.kt`
  - Not Editör: `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/note_editor/NoteEditorScreen.kt`
- **Profil:** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/profile/ProfileScreen.kt`
- **Görev Detay / Düzenleme:** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/task_detail/TaskDetailScreen.kt` & `TaskEditorScreen.kt`
- **Etkinlik Detay / Düzenleme:** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/event_detail/EventDetailScreen.kt` & `EventEditorScreen.kt`
- **Premium Ekranı:** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/premium/PremiumScreen.kt`
- **Çöp Kutusu (Trash):** `feature-home/src/commonMain/kotlin/com/yusufteker/planora/feature/home/presentation/trash/TrashScreen.kt`
- **Root & Navigasyon:** `composeApp/src/commonMain/kotlin/com/yusufteker/planora/App.kt` & `MainScreen.kt`

## 3. Dil & Metin Dosyaları (Localization)
- **İngilizce (Varsayılan):** `core/src/commonMain/composeResources/values/strings.xml`
- **Türkçe:** `core/src/commonMain/composeResources/values-tr/strings.xml`

## 4. Hızlı Aksiyon Kuralı (Speed Protocol)
- Ufak UI/stil/metin isteklerinde global arama (`grep`) yapma, doğrudan yukarıdaki dosya yoluna git.
- Her küçük UI değişikliğinde tam `./gradlew :androidApp:compileDebugKotlin` çalıştırma; kullanıcı istemedikçe veya kritik mimari değişmedikçe tek adımda bitir.
