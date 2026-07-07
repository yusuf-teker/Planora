import Foundation
import LocalLLMClient
import LocalLLMClientLlama
import ComposeApp

// Android'deki Gemma 3 1B planıyla tutarlı, QAT Q4 — küçük ve telefon için hızlı.
private let localModel = LLMSession.DownloadModel.llama(
    id: "lmstudio-community/gemma-3-1B-it-qat-GGUF",
    model: "gemma-3-1B-it-QAT-Q4_0.gguf",
    parameter: .init(
        temperature: 0.2,   // JSON çıkarımı için düşük randomness
        topK: 40,
        topP: 0.9,
        options: .init(responseFormat: .json) // llama.cpp grammar ile JSON'a zorluyor
    )
)

private var localSession: LLMSession?

func setupLocalLlmBridge() {
    IosAiBridge.shared.downloadLocalModel = { onProgress, completion in
        Task {
            do {
                try await localModel.downloadModel { progress in
                    _ = onProgress(KotlinFloat(value: Float(progress)))
                }
                localSession = LLMSession(model: localModel)
                _ = completion(true)
            } catch {
                print("Local LLM indirme hatası: \(error)")
                _ = completion(false)
            }
        }
    }

    IosAiBridge.shared.generateLocal = { prompt, completion in
        Task {
            do {
                if localSession == nil {
                    localSession = LLMSession(model: localModel)
                }
                let response = try await localSession!.respond(to: prompt)
                _ = completion(response)
            } catch {
                print("Local LLM generate hatası: \(error)")
                _ = completion(nil)
            }
        }
    }
}
