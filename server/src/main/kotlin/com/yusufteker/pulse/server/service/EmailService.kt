package com.yusufteker.pulse.server.service

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Service for sending emails using SMTP (e.g., Gmail SMTP).
 */
object EmailService {

    private val dotenv = Dotenv.configure().ignoreIfMissing().load()

    private val smtpHost: String
        get() = System.getenv("SMTP_HOST") ?: dotenv["SMTP_HOST"] ?: "smtp.gmail.com"

    private val smtpPort: String
        get() = System.getenv("SMTP_PORT") ?: dotenv["SMTP_PORT"] ?: "587"

    private val username: String?
        get() = System.getenv("SMTP_USERNAME") 
            ?: System.getenv("GMAIL_USERNAME") 
            ?: dotenv["SMTP_USERNAME"] 
            ?: dotenv["GMAIL_USERNAME"]

    private val password: String?
        get() = System.getenv("SMTP_PASSWORD") 
            ?: System.getenv("GMAIL_PASSWORD") 
            ?: dotenv["SMTP_PASSWORD"] 
            ?: dotenv["GMAIL_PASSWORD"]

    /**
     * Sends a password reset OTP code email to the specified recipient using SMTP.
     *
     * @param toEmail Recipient email address.
     * @param resetCode 6-digit OTP code generated for password reset.
     * @return `true` if email was dispatched successfully, `false` otherwise.
     */
    suspend fun sendPasswordResetEmail(toEmail: String, resetCode: String): Boolean {
        val user = username
        val pass = password

        if (user.isNullOrEmpty() || pass.isNullOrEmpty()) {
            System.err.println("EmailService: SMTP_USERNAME or SMTP_PASSWORD is not set. Cannot send password reset email.")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", smtpHost)
                    put("mail.smtp.port", smtpPort)
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(user, pass)
                    }
                })

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

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(user, "Pulse App"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    setSubject(subject, "UTF-8")
                    setContent(htmlContent, "text/html; charset=UTF-8")
                }

                Transport.send(message)
                println("EmailService: Email successfully sent to $toEmail via SMTP")
                true
            } catch (e: Exception) {
                System.err.println("EmailService: Exception while sending email via SMTP: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
}
