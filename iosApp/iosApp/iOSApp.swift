import SwiftUI
import FoundationModels
import ComposeApp
@main
struct PulseApp: App {
    init() {
        setupAiBridge()
        setupLocalLlmBridge()
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

@available(iOS 26.0, *)
@Generable
struct IntentResult: Codable {
    @Guide(.anyOf(["task", "note", "event"]))
    var type: String
    var title: String
    var hasDeadline: Bool
    @Guide(description: "YYYY-MM-DD formatında, belirtilmemişse null")
    var deadline: String?
    @Guide(description: "HH:mm formatında, belirtilmemişse null")
    var time: String?
}

func setupAiBridge() {
    if #available(iOS 26.0, *) {
        IosAiBridge.shared.isAvailable = {
            return .init(bool: SystemLanguageModel.default.availability == .available)
        }

        IosAiBridge.shared.parse = { input, completion in
            Task {
                do {
                    let session = LanguageModelSession(instructions: """
                        Sen bir görev asistanısın. Kullanıcı mesajını analiz et:
                        1. Eğer bir saat aralığı (örn: '8 9 arası') veya etkinlik (toplantı, ders) içeriyorsa 'event' olarak sınıflandır.
                        2. Eğer yapılması/yetişilmesi gereken bir iş ise (örn: 'ödev yap', 'elma al') 'task' olarak sınıflandır.
                        3. Zaman içermeyen genel metinleri 'note' olarak sınıflandır.
                        BAŞLIK: Kullanıcının cümlesini kopyalama, maksimum 3 kelimeyle özetle (örn: 'Yarın saat 8 9 arası tenis dersim var' -> 'Tenis Dersi').
                        ZAMAN: Tarih veya saat ifadesi varsa 'deadline' ve 'time' alanlarını doldur ve 'hasDeadline' değerini true yap. Yoksa false yap.
                    """)
                    let result = try await session.respond(to: input, generating: IntentResult.self)
                    let data = try JSONEncoder().encode(result.content)
                    _ = completion(String(data: data, encoding: .utf8))
                } catch {
                    print("Apple Intelligence Error: \(error.localizedDescription)")
                    print("Apple Intelligence Full Error: \(error)")
                    _ = completion(nil)
                }
            }
        }
    } else {
        IosAiBridge.shared.isAvailable = { return .init(bool: false) }
        IosAiBridge.shared.parse = { input, completion in
            _ = completion(nil)
        }
    }
}