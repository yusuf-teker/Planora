package com.yusufteker.planora.server.routes

import com.yusufteker.planora.server.database.DatabaseFactory.dbQuery
import com.yusufteker.planora.server.database.tables.RefreshTokenEntity
import com.yusufteker.planora.server.database.tables.RefreshTokensTable
import com.yusufteker.planora.server.database.tables.UserEntity
import com.yusufteker.planora.server.database.tables.UsersTable
import com.yusufteker.planora.server.database.tables.isPremiumActive
import com.yusufteker.planora.server.security.HashingService
import com.yusufteker.planora.server.security.TokenService
import com.yusufteker.planora.server.service.EmailService
import com.yusufteker.planora.server.util.respondError
import com.yusufteker.planora.shared.api.ApiErrorCode
import com.yusufteker.planora.shared.api.AuthRequest
import com.yusufteker.planora.shared.api.AuthResponse
import com.yusufteker.planora.shared.api.RefreshTokenRequest
import com.yusufteker.planora.shared.api.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Defines all Authentication endpoints.
 */
fun Route.authRoutes() {
    route("/auth") {
        
        // --- 0. SEND REGISTER CODE ENDPOINT ---
        post("/send-register-code") {
            val request = call.receive<com.yusufteker.planora.shared.api.SendRegisterCodeRequest>()
            val cleanEmail = request.email.trim().lowercase()
            val cleanUsername = request.username.trim().lowercase()

            val existingUser = dbQuery {
                UserEntity.find { 
                    (UsersTable.email.lowerCase() eq cleanEmail) or (UsersTable.username.lowerCase() eq cleanUsername)
                }.firstOrNull()
            }
            if (existingUser != null) {
                if (existingUser.email.equals(cleanEmail, ignoreCase = true)) {
                    call.respondError(HttpStatusCode.Conflict, ApiErrorCode.EMAIL_IN_USE, "Email already in use")
                } else {
                    call.respondError(HttpStatusCode.Conflict, ApiErrorCode.USERNAME_IN_USE, "Username already in use")
                }
                return@post
            }

            val verificationCode = (100000..999999).random().toString()
            val expiresAt = Instant.now().plus(java.time.Duration.ofMinutes(15))

            dbQuery {
                com.yusufteker.planora.server.database.tables.EmailVerificationTokenEntity.find {
                    com.yusufteker.planora.server.database.tables.EmailVerificationTokensTable.email eq cleanEmail
                }.forEach { it.delete() }

                com.yusufteker.planora.server.database.tables.EmailVerificationTokenEntity.new {
                    this.email = cleanEmail
                    this.token = verificationCode
                    this.expiresAt = expiresAt
                    this.createdAt = Instant.now()
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                EmailService.sendRegistrationVerificationEmail(cleanEmail, verificationCode)
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Verification code sent to email"))
        }

        // --- 1. REGISTER ENDPOINT ---
        post("/register") { // auth/register geldiğinde
            val request = call.receive<RegisterRequest>()
            val cleanEmail = request.email.trim().lowercase()
            val cleanUsername = request.username.trim().lowercase()

            // Veritabanında email veya username daha önce alınmış mı kontrol ediyoruz.
            val existingUser = dbQuery {
                UserEntity.find { 
                    (UsersTable.email.lowerCase() eq cleanEmail) or (UsersTable.username.lowerCase() eq cleanUsername)
                }.firstOrNull()
            }
            if (existingUser != null) {
                if (existingUser.email.equals(cleanEmail, ignoreCase = true)) {
                    call.respondError(HttpStatusCode.Conflict, ApiErrorCode.EMAIL_IN_USE, "Email already in use")
                } else {
                    call.respondError(HttpStatusCode.Conflict, ApiErrorCode.USERNAME_IN_USE, "Username already in use")
                }
                return@post
            }

            // Doğrulama kodunu kontrol ediyoruz.
            val tokenEntity = dbQuery {
                com.yusufteker.planora.server.database.tables.EmailVerificationTokenEntity.find {
                    (com.yusufteker.planora.server.database.tables.EmailVerificationTokensTable.email eq cleanEmail) and
                    (com.yusufteker.planora.server.database.tables.EmailVerificationTokensTable.token eq request.code)
                }.firstOrNull()
            }

            if (tokenEntity == null) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code")
                return@post
            }

            if (tokenEntity.expiresAt.isBefore(Instant.now())) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.EXPIRED_VERIFICATION_CODE, "Expired verification code")
                return@post
            }

            // Güvenlik: Gelen düz metin şifreyi BCrypt ile hashliyoruz.
            val hashedPassword = HashingService.hashPassword(request.password)
            
            // Veritabanına yeni bir kullanıcı kaydediyoruz.
            val newUser = dbQuery {
                tokenEntity.delete() // Kullanılan token'ı sil
                UserEntity.new {
                    name = request.name
                    username = request.username
                    email = request.email
                    passwordHash = hashedPassword
                    createdAt = Instant.now()
                }
            }

            // Kayıt olan kullanıcı için hemen yetkilendirme (Access) ve yenileme (Refresh) token'ları üretiyoruz.
            val accessToken = TokenService.generateAccessToken(newUser.id.value, newUser.email)
            val refreshToken = TokenService.generateRefreshToken()

            // Refresh token'ı bir sonraki yenileme işlemi için veritabanına kaydediyoruz.
            dbQuery {
                RefreshTokenEntity.new {
                    user = newUser
                    token = refreshToken
                    expiresAt = Instant.now().plusMillis(TokenService.REFRESH_TOKEN_EXPIRATION)
                    createdAt = Instant.now()
                }
            }

            // Kayıt olan kullanıcıya hoş geldin e-postasını arka planda gönderiyoruz.
            CoroutineScope(Dispatchers.IO).launch {
                EmailService.sendWelcomeEmail(newUser.email, newUser.name)
            }

            // İşlem başarılı! Uygulamaya token'ları ve kullanıcı bilgilerini dönüyoruz.
            val (isPrem, premUntil) = dbQuery { newUser.isPremiumActive() to newUser.premiumUntil?.toString() }
            call.respond(HttpStatusCode.Created, AuthResponse(accessToken, refreshToken, newUser.id.value, newUser.name, newUser.username, newUser.avatarId, newUser.profileImageUrl, isPremium = isPrem, premiumUntil = premUntil))
        }

        // --- 2. LOGIN ENDPOINT ---
        post("/login") {
            val request = call.receive<AuthRequest>() // Kullanıcı identifier ve şifre gönderir

            // Veritabanından identifier'a (email veya username) göre kullanıcıyı arıyoruz.
            val user = dbQuery {
                UserEntity.find { 
                    (UsersTable.email eq request.identifier) or (UsersTable.username eq request.identifier)
                }.firstOrNull()
            }

            // Kullanıcı yoksa veya uygulamanın gönderdiği şifrenin hash'i DB'deki hash ile eşleşmiyorsa hata dönüyoruz.
            if (user == null || !HashingService.verifyPassword(request.password, user.passwordHash)) {
                call.respondError(HttpStatusCode.Unauthorized, ApiErrorCode.INVALID_CREDENTIALS, "Invalid credentials")
                return@post
            }
            // email ve şifre doğru ise token üretiyoruz ve kullanıcıya dönüyoruz.
            val accessToken = TokenService.generateAccessToken(user.id.value, user.email)
            val refreshToken = TokenService.generateRefreshToken()

            //refresh tokenı veritabanına kaydediyoruz. (Refresh tokenlar DB'de tutulur, Access tokenlar tutulmaz)
            dbQuery {
                RefreshTokenEntity.new {
                    this.user = user
                    token = refreshToken
                    expiresAt = Instant.now().plusMillis(TokenService.REFRESH_TOKEN_EXPIRATION)
                    createdAt = Instant.now()
                }
            }
            // İşlem başarılı! Uygulamaya token'ları ve kullanıcı bilgilerini dönüyoruz.
            val (isPrem, premUntil) = dbQuery { user.isPremiumActive() to user.premiumUntil?.toString() }
            call.respond(HttpStatusCode.OK, AuthResponse(accessToken, refreshToken, user.id.value, user.name, user.username, user.avatarId, user.profileImageUrl, isPremium = isPrem, premiumUntil = premUntil))
        }

        // --- 3. REFRESH TOKEN ENDPOINT ---
        // Uygulamadaki Access Token (2 saat) süresi dolduğunda, uygulama otomatik olarak (Ktor Auth Plugin ile) bu endpoint'e gelir.
        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()

            // Client -> Server: Refresh Token gönderir. Server bu tokenı veritabanında arar.
            val refreshTokenEntity = dbQuery {
                RefreshTokenEntity.find { RefreshTokensTable.token eq request.refreshToken }.firstOrNull()
            }


            // Token veritabanında yoksa veya süresi dolmuşsa geçersiz kılıyoruz.
            if (refreshTokenEntity == null || refreshTokenEntity.expiresAt.isBefore(Instant.now())) {
                call.respondError(HttpStatusCode.Unauthorized, ApiErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token")
                return@post
            }

            // Token geçerli ise, ilişkili kullanıcıyı buluyoruz.
            val user = dbQuery { refreshTokenEntity.user }

            // Eski token geçerli olduğu için kullanıcıya yeni tokenlar veriyoruz.
            // Eski token geçerli olduğu için kullanıcıya yeni tokenlar veriyoruz.
            val newAccessToken = TokenService.generateAccessToken(user.id.value, user.email)
            
            dbQuery {
                // Refresh Token'ın süresini uzatıyoruz, ancak token'ı değiştirmiyoruz (Token Rotation kapatıldı).
                // Bu sayede eşzamanlı atılan isteklerde (Concurrency) refresh token'ın silinmesi kaynaklı 401 hataları önlenir.
                refreshTokenEntity.expiresAt = Instant.now().plusMillis(TokenService.REFRESH_TOKEN_EXPIRATION)
            }

            // İşlem başarılı! Uygulamaya yeni token'ları ve kullanıcı bilgilerini dönüyoruz.
            val (isPrem, premUntil) = dbQuery { user.isPremiumActive() to user.premiumUntil?.toString() }
            call.respond(HttpStatusCode.OK, AuthResponse(newAccessToken, request.refreshToken, user.id.value, user.name, user.username, user.avatarId, user.profileImageUrl, isPremium = isPrem, premiumUntil = premUntil))
        }

        // --- 4. FORGOT PASSWORD ENDPOINT ---
        post("/forgot-password") {
            val request = call.receive<com.yusufteker.planora.shared.api.ForgotPasswordRequest>()
            val cleanEmail = request.email.trim().lowercase()
            
            val user = dbQuery {
                UserEntity.find { UsersTable.email.lowerCase() eq cleanEmail }.firstOrNull()
            }

            // Güvenlik: Kullanıcı bulunamazsa da aynı mesajı dönerek e-posta keşfini (enumeration) engelliyoruz.
            if (user != null) {
                // 6 haneli rastgele OTP kodu üret
                val resetCode = (100000..999999).random().toString()
                val expiresAt = Instant.now().plus(java.time.Duration.ofMinutes(15))

                dbQuery {
                    // Kullanıcının daha önceki aktif reset token'larını temizle
                    com.yusufteker.planora.server.database.tables.PasswordResetTokenEntity.find {
                        com.yusufteker.planora.server.database.tables.PasswordResetTokensTable.userId eq user.id.value
                    }.forEach { it.delete() }

                    // Yeni token kaydet
                    com.yusufteker.planora.server.database.tables.PasswordResetTokenEntity.new {
                        this.user = user
                        this.token = resetCode
                        this.expiresAt = expiresAt
                        this.createdAt = Instant.now()
                    }
                }

                // E-posta gönderimini arka planda (asenkron) yapıyoruz ki mobil uygulama beklemede (loading) kalmasın
                CoroutineScope(Dispatchers.IO).launch {
                    EmailService.sendPasswordResetEmail(user.email, resetCode)
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "If an account with this email exists, a password reset code has been sent."))
        }

        // --- 5. VERIFY RESET CODE ENDPOINT ---
        post("/verify-reset-code") {
            val request = call.receive<com.yusufteker.planora.shared.api.VerifyResetCodeRequest>()
            val cleanEmail = request.email.trim().lowercase()
            
            val user = dbQuery {
                UserEntity.find { UsersTable.email.lowerCase() eq cleanEmail }.firstOrNull()
            }

            if (user == null) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_RESET_CODE, "Invalid reset code or email")
                return@post
            }

            val resetTokenEntity = dbQuery {
                com.yusufteker.planora.server.database.tables.PasswordResetTokenEntity.find {
                    (com.yusufteker.planora.server.database.tables.PasswordResetTokensTable.userId eq user.id.value) and
                    (com.yusufteker.planora.server.database.tables.PasswordResetTokensTable.token eq request.code)
                }.firstOrNull()
            }

            if (resetTokenEntity == null || resetTokenEntity.expiresAt.isBefore(Instant.now())) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.EXPIRED_RESET_CODE, "Invalid or expired reset code")
                return@post
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Code verified successfully"))
        }

        // --- 6. RESET PASSWORD ENDPOINT ---
        post("/reset-password") {
            val request = call.receive<com.yusufteker.planora.shared.api.ResetPasswordRequest>()
            val cleanEmail = request.email.trim().lowercase()

            val user = dbQuery {
                UserEntity.find { UsersTable.email.lowerCase() eq cleanEmail }.firstOrNull()
            }

            if (user == null) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.INVALID_RESET_CODE, "Invalid reset code or email")
                return@post
            }

            val resetTokenEntity = dbQuery {
                com.yusufteker.planora.server.database.tables.PasswordResetTokenEntity.find {
                    (com.yusufteker.planora.server.database.tables.PasswordResetTokensTable.userId eq user.id.value) and
                    (com.yusufteker.planora.server.database.tables.PasswordResetTokensTable.token eq request.code)
                }.firstOrNull()
            }

            if (resetTokenEntity == null || resetTokenEntity.expiresAt.isBefore(Instant.now())) {
                call.respondError(HttpStatusCode.BadRequest, ApiErrorCode.EXPIRED_RESET_CODE, "Invalid or expired reset code")
                return@post
            }

            // Yeni şifreyi BCrypt ile hashle ve güncelle
            val newHashedPassword = HashingService.hashPassword(request.newPassword)
            dbQuery {
                user.passwordHash = newHashedPassword
                resetTokenEntity.delete() // Kullanılan token'ı sil
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Password reset successfully"))
        }

        // --- 7. PROTECTED ENDPOINT (Sadece giriş yapmış kullanıcılar girebilir) ---

        // `authenticate("auth-jwt")` bloğu, Security.kt içerisinde ayarladığımız kuralı çalıştırır.
        // Gelen Header'da "Bearer <token>" yoksa Ktor otomatik olarak 401 Unauthorized döner, aşağıdaki kod hiç çalışmaz.
        authenticate("auth-jwt") {
            get("/me") {

                //GET /auth/me
                //Authorization: Bearer eyJhbGciOi...
                //Client tarafından gönderilen token'ı Ktor otomatik olarak doğrular. Token geçersizse 401 döner.

                // Token doğrulandıysa içindeki şifreli veriyi (Payload) okuyabiliriz.
                //JWTPrincipal( // Token içinde userId ve email var. Bunları okuyabiliriz.
                //    payload = {
                //        userId = 1,
                //        email = "yusuf@gmail.com"
                //    }
                //)
                val principal = call.principal<JWTPrincipal>()


                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val userStats = dbQuery {
                    val user = UserEntity.findById(userId) ?: return@dbQuery null
                    
                    val followersCount = com.yusufteker.planora.server.database.tables.FollowerEntity.find { com.yusufteker.planora.server.database.tables.FollowersTable.followedId eq userId }.count().toInt()
                    val followingCount = com.yusufteker.planora.server.database.tables.FollowerEntity.find { com.yusufteker.planora.server.database.tables.FollowersTable.followerId eq userId }.count().toInt()
                    val postsCount = com.yusufteker.planora.server.database.tables.PostEntity.find { com.yusufteker.planora.server.database.tables.PostsTable.authorId eq userId }.count().toInt()

                    com.yusufteker.planora.shared.api.UserProfileResponse(
                        id = user.id.value,
                        name = user.name,
                        username = user.username,
                        email = user.email,
                        avatarId = user.avatarId,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        postsCount = postsCount,
                        profileImageUrl = user.profileImageUrl,
                        isPremium = user.isPremiumActive(),
                        premiumUntil = user.premiumUntil?.toString()
                    )
                }

                if (userStats != null) {
                    call.respond(HttpStatusCode.OK, userStats)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            put("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }

                val request = call.receive<com.yusufteker.planora.shared.api.UpdateProfileRequest>()

                // name ve avatarId güncellemesini veritabanında yapıyoruz.
                val user = dbQuery {
                    val entity = UserEntity.findById(userId)
                    if (entity != null) {
                        entity.name = request.name
                        entity.avatarId = request.avatarId
                        if (request.profileImageUrl != null) {
                            entity.profileImageUrl = request.profileImageUrl
                        }
                    }
                    entity
                }

                if (user != null) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Profile updated successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
