import SwiftUI
import WidgetKit
import FoundationModels
import ComposeApp
import FirebaseCore
import FirebaseMessaging

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        
        UNUserNotificationCenter.current().delegate = self
        
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { _, _ in }
        )
        
        application.registerForRemoteNotifications()
        Messaging.messaging().delegate = self
        
        return true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }
    
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token: \(String(describing: fcmToken))")
        if let token = fcmToken {
            IosNotificationBridge.shared.setFcmToken(token: token)
        }
    }
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable : Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        if let type = userInfo["type"] as? String {
            IosNotificationBridge.shared.handlePushData(type: type)
        }
        
        completionHandler(.newData)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        if let type = userInfo["type"] as? String {
            IosNotificationBridge.shared.handlePushData(type: type)
        }
        completionHandler([[.banner, .list, .sound]])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let type = userInfo["type"] as? String {
            IosNotificationBridge.shared.handlePushData(type: type)
        }
        completionHandler()
    }
}

@main
struct PlanoraApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        setupAiBridge()
        setupWidgetBridge()
    }
    var body: some Scene {
        WindowGroup { 
            ContentView()
                .onOpenURL { url in
                    DeepLinkManager.shared.emitLink(link: url.absoluteString)
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                    IosWidgetBridge.shared.syncWidgetData()
                }
        }
    }
}

func setupWidgetBridge() {
    IosWidgetBridge.shared.onWidgetDataReloadRequested = {
        WidgetCenter.shared.reloadAllTimelines()
    }
    IosWidgetBridge.shared.syncWidgetData()
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