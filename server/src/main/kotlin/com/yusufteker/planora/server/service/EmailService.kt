package com.yusufteker.planora.server.service

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Service for sending emails using Google Apps Script Web App (HTTPS Proxy over Port 443).
 *
 * Bypasses cloud provider (Render, etc.) SMTP port blocks completely.
 */
object EmailService {

    private val dotenv = Dotenv.configure().ignoreIfMissing().load()

    private val resendApiKey: String?
        get() = System.getenv("RESEND_API_KEY") ?: dotenv["RESEND_API_KEY"]

    private val scriptUrl: String?
        get() = System.getenv("GMAIL_SCRIPT_URL") ?: dotenv["GMAIL_SCRIPT_URL"]

    // Google Apps Script redirects HTTP POST (302), so ALWAYS follow redirects.
    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    /**
     * Sends a custom email to the specified recipient using Resend API or Google Script.
     *
     * @param toEmail Recipient email address.
     * @param subject Email subject line.
     * @param htmlContent HTML body content of the email.
     * @return `true` if email request was dispatched successfully, `false` otherwise.
     */
    suspend fun sendEmail(toEmail: String, subject: String, htmlContent: String): Boolean {
        val apiKey = resendApiKey
        if (!apiKey.isNullOrEmpty()) {
            return sendViaResend(toEmail, subject, htmlContent, apiKey)
        }

        val url = scriptUrl
        if (url.isNullOrEmpty()) {
            System.err.println("EmailService: Neither RESEND_API_KEY nor GMAIL_SCRIPT_URL is set.")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val escapedHtml = htmlContent
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")

                val escapedSubject = subject
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")

                val jsonPayload = """
                    {
                        "secret": "planora_mail_secret_123",
                        "to": "$toEmail",
                        "subject": "$escapedSubject",
                        "html": "$escapedHtml"
                    }
                """.trimIndent()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299 && !response.body().contains("Sayfa Bulunamadı")) {
                    println("EmailService: Email successfully sent to $toEmail via Script. Response: ${response.body()}")
                    true
                } else {
                    System.err.println("EmailService: Failed to send email via Script. Code: ${response.statusCode()}, Body: ${response.body()}")
                    false
                }
            } catch (e: Exception) {
                System.err.println("EmailService: Exception while sending email via Google Script: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun sendViaResend(toEmail: String, subject: String, htmlContent: String, apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val escapedHtml = htmlContent
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")

                val escapedSubject = subject
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")

                val jsonPayload = """
                    {
                        "from": "Planora <onboarding@resend.dev>",
                        "to": ["$toEmail"],
                        "subject": "$escapedSubject",
                        "html": "$escapedHtml"
                    }
                """.trimIndent()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $apiKey")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() in 200..299) {
                    println("EmailService: Email successfully sent to $toEmail via Resend. Response: ${response.body()}")
                    true
                } else {
                    System.err.println("EmailService: Resend API error. Code: ${response.statusCode()}, Body: ${response.body()}")
                    false
                }
            } catch (e: Exception) {
                System.err.println("EmailService: Exception while sending email via Resend: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Sends a password reset OTP code email to the specified recipient.
     *
     * @param toEmail Recipient email address.
     * @param resetCode 6-digit OTP code generated for password reset.
     * @return `true` if email was dispatched successfully, `false` otherwise.
     */
    suspend fun sendPasswordResetEmail(toEmail: String, resetCode: String): Boolean {
        val subject = "Planora - Şifre Sıfırlama Kodu / Password Reset Code"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <h2 style="color: #6200EE; text-align: center;">Planora App</h2>
                <p>Merhaba,</p>
                <p>Planora hesabınız için bir şifre sıfırlama talebinde bulundunuz. Şifrenizi değiştirmek için aşağıdaki 6 haneli doğrulama kodunu kullanabilirsiniz:</p>
                <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 8px; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #333;">
                    $resetCode
                </div>
                <p style="margin-top: 20px;">Bu kod <strong>15 dakika</strong> boyunca geçerlidir. Eğer şifre sıfırlama talebinde bulunmadıysanız bu e-postayı dikkate almayınız.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                <p style="font-size: 12px; color: #888; text-align: center;">Planora Team</p>
            </div>
        """.trimIndent()

        return sendEmail(toEmail, subject, htmlContent)
    }

    /**
     * Sends an email verification OTP code to a user registering a new account.
     *
     * @param toEmail Recipient email address.
     * @param verificationCode 6-digit OTP verification code.
     * @return `true` if email was dispatched successfully, `false` otherwise.
     */
    suspend fun sendRegistrationVerificationEmail(toEmail: String, verificationCode: String): Boolean {
        val subject = "Planora - Hesap Doğrulama Kodu / Account Verification Code"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <h2 style="color: #6200EE; text-align: center;">Planora App</h2>
                <p>Merhaba,</p>
                <p>Planora kayıt işleminizi tamamlamak için aşağıdaki 6 haneli e-posta doğrulama kodunu kullanabilirsiniz:</p>
                <div style="background-color: #f5f5f5; padding: 15px; text-align: center; border-radius: 8px; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #333;">
                    $verificationCode
                </div>
                <p style="margin-top: 20px;">Bu kod <strong>15 dakika</strong> boyunca geçerlidir. Eğer siz kayıt olmadıysanız bu e-postayı dikkate almayınız.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                <p style="font-size: 12px; color: #888; text-align: center;">Planora Team</p>
            </div>
        """.trimIndent()

        return sendEmail(toEmail, subject, htmlContent)
    }


    /**
     * Sends an account welcome / verification email to a newly registered user.
     *
     * @param toEmail Recipient email address.
     * @param userName Name of the user.
     * @return `true` if email was dispatched successfully, `false` otherwise.
     */
    suspend fun sendWelcomeEmail(toEmail: String, userName: String): Boolean {
        val subject = "Planora'a Hoş Geldiniz! / Welcome to Planora!"
        val htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;">
                <h2 style="color: #6200EE; text-align: center;">Planora App</h2>
                <p>Merhaba <strong>$userName</strong>,</p>
                <p>Planora ailesine katıldığınız için teşekkür ederiz! Hesabınız başarıyla oluşturuldu.</p>
                <p>Artık görevlerinizi yönetebilir, oda oluşturabilir ve ekibinizle senkronize çalışabilirsiniz.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                <p style="font-size: 12px; color: #888; text-align: center;">Planora Team</p>
            </div>
        """.trimIndent()

        return sendEmail(toEmail, subject, htmlContent)
    }
}
