package com.yusufteker.pulse.server.service

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Service for sending emails using the Resend API (https://resend.com).
 *
 * Utilizes Java's built-in [HttpClient] to perform HTTP POST requests
 * to Resend's REST API endpoint.
 */
object ResendEmailService {

    private val dotenv = Dotenv.configure().ignoreIfMissing().load()
    private val httpClient = HttpClient.newHttpClient()

    private val apiKey: String?
        get() = System.getenv("RESEND_API_KEY") ?: dotenv["RESEND_API_KEY"]

    private val fromEmail: String
        get() = System.getenv("RESEND_FROM_EMAIL") ?: dotenv["RESEND_FROM_EMAIL"] ?: "Pulse <onboarding@resend.dev>"

    /**
     * Sends a password reset OTP code email to the specified recipient.
     *
     * @param toEmail Recipient email address.
     * @param resetCode 6-digit OTP code generated for password reset.
     * @return `true` if email was dispatched successfully, `false` otherwise.
     */
    suspend fun sendPasswordResetEmail(toEmail: String, resetCode: String): Boolean {
        val key = apiKey
        if (key.isNull_or_blank()) {
            System.err.println("ResendEmailService: RESEND_API_KEY is not set. Cannot send password reset email.")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val subject = "Pulse - Şifre Sıfırlama Kodu / Password Reset Code"
                val htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                        <h2 style="color: #6200EE; text-align: center;">Pulse App</h2>
                        <p>Merhaba,</p>
                        <p>Pulse hesabınız için bir şifre sıfırlama talebinde bulundunuz. Şifrenizi değiştirmek için aşağıdaki 6 haneli doğrulama kodunu kullanabilirsiniz:</p>
                        <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 8px; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #333;">
                            $resetCode
                        </div>
                        <p style="margin-top: 20px;">Bu kod <strong>15 dakika</strong> boyunca geçerlidir. Eğer şifre sıfırlama talebinde bulunmadıysanız bu e-postayı dikkate almayınız.</p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                        <p style="font-size: 12px; color: #888; text-align: center;">Pulse Team</p>
                    </div>
                """.trimIndent()

                val escapedHtml = htmlContent
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")

                val jsonPayload = """
                    {
                        "from": "$fromEmail",
                        "to": ["$toEmail"],
                        "subject": "$subject",
                        "html": "$escapedHtml"
                    }
                """.trimIndent()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer $key")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    println("ResendEmailService: Email successfully sent to $toEmail. Response: ${response.body()}")
                    true
                } else {
                    System.err.println("ResendEmailService: Failed to send email. Code: ${response.statusCode()}, Body: ${response.body()}")
                    false
                }
            } catch (e: Exception) {
                System.err.println("ResendEmailService: Exception while sending email: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.isBlank()
    }
}
