import WidgetKit
import SwiftUI
import AppIntents

struct WidgetTaskItem: Identifiable, Codable {
    let id: String
    let title: String
    let timeString: String
    let type: String
    let isCompleted: Bool
}

struct WidgetDataPayload: Codable {
    let todayHeader: String?
    let weekHeader: String?
    let tabToday: String?
    let tabWeek: String?
    let noTasksText: String?
    let todayDateSub: String?
    let todayTasks: [WidgetTaskItem]?
    let weekTasks: [WidgetTaskItem]?
}

struct PlanoraWidgetEntry: TimelineEntry {
    let date: Date
    let payload: WidgetDataPayload?
}

@available(iOS 16.0, *)
struct SelectWidgetTabIntent: AppIntent {
    static var title: LocalizedStringResource = "Sekme Seç"
    static var description = IntentDescription("Bugün ve Bu Hafta sekmeleri arasında geçiş yapar.")

    @Parameter(title: "Tab Index")
    var tabIndex: Int

    init() {
        self.tabIndex = 0
    }

    init(tabIndex: Int) {
        self.tabIndex = tabIndex
    }

    func perform() async throws -> some IntentResult {
        let defaults = UserDefaults(suiteName: "group.com.yusufteker.planora") ?? UserDefaults.standard
        defaults.set(tabIndex, forKey: "selected_widget_tab")
        UserDefaults.standard.set(tabIndex, forKey: "selected_widget_tab")
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct PlanoraWidgetProvider: TimelineProvider {
    private let suiteName = "group.com.yusufteker.planora"
    private let widgetDataKey = "widget_data_json"

    func placeholder(in context: Context) -> PlanoraWidgetEntry {
        PlanoraWidgetEntry(date: Date(), payload: sampleData())
    }

    func getSnapshot(in context: Context, completion: @escaping (PlanoraWidgetEntry) -> Void) {
        let entry = PlanoraWidgetEntry(date: Date(), payload: loadPayload() ?? sampleData())
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PlanoraWidgetEntry>) -> Void) {
        let entry = PlanoraWidgetEntry(date: Date(), payload: loadPayload())
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date())!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        completion(timeline)
    }

    private func loadPayload() -> WidgetDataPayload? {
        let groupDefaults = UserDefaults(suiteName: suiteName)
        let jsonString = groupDefaults?.string(forKey: widgetDataKey) ?? UserDefaults.standard.string(forKey: widgetDataKey)
        guard let json = jsonString, let data = json.data(using: .utf8) else {
            return nil
        }
        return try? JSONDecoder().decode(WidgetDataPayload.self, from: data)
    }

    private func sampleData() -> WidgetDataPayload {
        WidgetDataPayload(
            todayHeader: "Bugün",
            weekHeader: "Bu Hafta",
            tabToday: "Bugün",
            tabWeek: "Hafta",
            noTasksText: "Planlanmış görev yok",
            todayDateSub: "1 Ağustos, Cumartesi",
            todayTasks: [
                WidgetTaskItem(id: "1", title: "Ekip Toplantısı", timeString: "14:00 - 15:30", type: "EVENT", isCompleted: false),
                WidgetTaskItem(id: "2", title: "Raporu Hazırla", timeString: "17:00", type: "TASK", isCompleted: false)
            ],
            weekTasks: []
        )
    }
}

struct PlanoraWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: PlanoraWidgetEntry

    var selectedTab: Int {
        let groupDefaults = UserDefaults(suiteName: "group.com.yusufteker.planora")
        return groupDefaults?.integer(forKey: "selected_widget_tab") ?? UserDefaults.standard.integer(forKey: "selected_widget_tab")
    }

    var body: some View {
        let currentTasks = (selectedTab == 0 ? entry.payload?.todayTasks : entry.payload?.weekTasks) ?? []

        switch family {
        case .systemSmall:
            SmallWidgetView(entry: entry, currentTasks: currentTasks)
        default:
            MediumOrLargeWidgetView(entry: entry, family: family, selectedTab: selectedTab, currentTasks: currentTasks)
        }
    }
}

// Dedicated compact UI for systemSmall family to avoid text collisions & overcrowded buttons
struct SmallWidgetView: View {
    var entry: PlanoraWidgetEntry
    var currentTasks: [WidgetTaskItem]

    var nextTask: WidgetTaskItem? {
        currentTasks.first { !$0.isCompleted } ?? currentTasks.first
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Header
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(entry.payload?.todayHeader ?? "Bugün")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.white)
                    Text(entry.payload?.todayDateSub ?? DateFormatter.localizedString(from: Date(), dateStyle: .short, timeStyle: .none))
                        .font(.system(size: 9))
                        .foregroundColor(Color(red: 0.61, green: 0.64, blue: 0.69))
                        .lineLimit(1)
                }
                Spacer()
                Image(systemName: "calendar.badge.clock")
                    .font(.system(size: 13))
                    .foregroundColor(Color(red: 0.23, green: 0.51, blue: 0.96))
            }

            Divider()
                .background(Color.white.opacity(0.12))

            if let item = nextTask {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: item.type == "EVENT" ? "calendar" : (item.isCompleted ? "checkmark.circle.fill" : "circle"))
                            .font(.system(size: 11))
                            .foregroundColor(item.type == "EVENT" ? Color(red: 0.96, green: 0.62, blue: 0.04) : (item.isCompleted ? Color(red: 0.06, green: 0.73, blue: 0.51) : Color(red: 0.23, green: 0.51, blue: 0.96)))

                        Text(item.type == "EVENT" ? "ETKİNLİK" : "GÖREV")
                            .font(.system(size: 9, weight: .black))
                            .foregroundColor(item.type == "EVENT" ? Color(red: 0.96, green: 0.62, blue: 0.04) : Color(red: 0.23, green: 0.51, blue: 0.96))

                        Spacer(minLength: 0)
                    }

                    Text(item.title)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(item.isCompleted ? Color(red: 0.42, green: 0.45, blue: 0.50) : .white)
                        .lineLimit(2)
                        .strikethrough(item.isCompleted)

                    Spacer(minLength: 0)

                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.system(size: 9))
                        Text(item.timeString)
                            .font(.system(size: 10, weight: .bold))
                    }
                    .foregroundColor(item.type == "EVENT" ? Color(red: 0.96, green: 0.62, blue: 0.04) : Color(red: 0.61, green: 0.64, blue: 0.69))
                }
                .padding(8)
                .background(Color(red: 0.13, green: 0.14, blue: 0.18))
                .cornerRadius(8)
            } else {
                VStack(spacing: 2) {
                    Spacer(minLength: 0)
                    Text("🎉")
                        .font(.system(size: 18))
                    Text(entry.payload?.noTasksText ?? "Planlanmış görev yok")
                        .font(.system(size: 10))
                        .foregroundColor(Color(red: 0.61, green: 0.64, blue: 0.69))
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                    Spacer(minLength: 0)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

// UI for systemMedium and systemLarge widget families
struct MediumOrLargeWidgetView: View {
    var entry: PlanoraWidgetEntry
    var family: WidgetFamily
    var selectedTab: Int
    var currentTasks: [WidgetTaskItem]

    var maxItems: Int {
        family == .systemLarge ? 7 : 3
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Header Section
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(selectedTab == 0 ? (entry.payload?.todayHeader ?? "Bugün") : (entry.payload?.weekHeader ?? "Bu Hafta"))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)

                    Text(entry.payload?.todayDateSub ?? DateFormatter.localizedString(from: Date(), dateStyle: .medium, timeStyle: .none))
                        .font(.system(size: 10))
                        .foregroundColor(Color(red: 0.61, green: 0.64, blue: 0.69))
                }

                Spacer()

                // Tab Switcher with AppIntent for iOS 17+
                HStack(spacing: 0) {
                    if #available(iOS 17.0, *) {
                        Button(intent: SelectWidgetTabIntent(tabIndex: 0)) {
                            Text(entry.payload?.tabToday ?? "Bugün")
                                .font(.system(size: 10, weight: selectedTab == 0 ? .bold : .medium))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(selectedTab == 0 ? Color(red: 0.23, green: 0.51, blue: 0.96) : Color.clear)
                                .foregroundColor(selectedTab == 0 ? .white : Color(red: 0.61, green: 0.64, blue: 0.69))
                                .cornerRadius(6)
                        }
                        .buttonStyle(.plain)

                        Button(intent: SelectWidgetTabIntent(tabIndex: 1)) {
                            Text(entry.payload?.tabWeek ?? "Hafta")
                                .font(.system(size: 10, weight: selectedTab == 1 ? .bold : .medium))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(selectedTab == 1 ? Color(red: 0.23, green: 0.51, blue: 0.96) : Color.clear)
                                .foregroundColor(selectedTab == 1 ? .white : Color(red: 0.61, green: 0.64, blue: 0.69))
                                .cornerRadius(6)
                        }
                        .buttonStyle(.plain)
                    } else {
                        Text(entry.payload?.tabToday ?? "Bugün")
                            .font(.system(size: 10, weight: .bold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(red: 0.23, green: 0.51, blue: 0.96))
                            .foregroundColor(.white)
                            .cornerRadius(6)
                    }
                }
                .padding(2)
                .background(Color(red: 0.14, green: 0.15, blue: 0.19))
                .cornerRadius(8)
            }

            if currentTasks.isEmpty {
                VStack(spacing: 4) {
                    Spacer()
                    Text("🎉")
                        .font(.system(size: 22))
                    Text(entry.payload?.noTasksText ?? "Planlanmış görev yok")
                        .font(.system(size: 11))
                        .foregroundColor(Color(red: 0.61, green: 0.64, blue: 0.69))
                    Spacer()
                }
                .frame(maxWidth: .infinity)
            } else {
                VStack(spacing: 4) {
                    ForEach(currentTasks.prefix(maxItems)) { item in
                        HStack(spacing: 6) {
                            if item.type == "EVENT" {
                                Image(systemName: "calendar")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(red: 0.96, green: 0.62, blue: 0.04))
                            } else if item.isCompleted {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))
                            } else {
                                Image(systemName: "circle")
                                    .font(.system(size: 12))
                                    .foregroundColor(Color(red: 0.23, green: 0.51, blue: 0.96))
                            }

                            Text(item.title)
                                .font(.system(size: 11))
                                .lineLimit(1)
                                .foregroundColor(item.isCompleted ? Color(red: 0.42, green: 0.45, blue: 0.50) : (item.type == "EVENT" ? Color(red: 0.99, green: 0.95, blue: 0.78) : Color(red: 0.95, green: 0.96, blue: 0.96)))
                                .strikethrough(item.isCompleted)

                            Spacer(minLength: 4)

                            Text(item.timeString)
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(item.type == "EVENT" ? Color(red: 0.96, green: 0.62, blue: 0.04) : Color(red: 0.61, green: 0.64, blue: 0.69))
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(Color(red: 0.13, green: 0.14, blue: 0.18))
                        .cornerRadius(6)
                    }
                }
            }
        }
    }
}

struct PlanoraWidget: Widget {
    let kind: String = "PlanoraWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PlanoraWidgetProvider()) { entry in
            if #available(iOS 17.0, *) {
                PlanoraWidgetEntryView(entry: entry)
                    .containerBackground(Color(red: 0.08, green: 0.09, blue: 0.12), for: .widget)
            } else {
                PlanoraWidgetEntryView(entry: entry)
                    .background(Color(red: 0.08, green: 0.09, blue: 0.12))
            }
        }
        .configurationDisplayName("Planora Ajanda")
        .description("Bugün ve bu haftaya ait görev ile etkinlikleriniz.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
