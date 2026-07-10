package com.yusufteker.pulse.server.routes

import com.yusufteker.pulse.server.database.DatabaseFactory.dbQuery
import com.yusufteker.pulse.server.database.tables.RefreshTokenEntity
import com.yusufteker.pulse.server.database.tables.RefreshTokensTable
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.server.database.tables.UsersTable
import com.yusufteker.pulse.server.security.HashingService
import com.yusufteker.pulse.server.security.TokenService
import com.yusufteker.pulse.shared.api.AuthRequest
import com.yusufteker.pulse.shared.api.AuthResponse
import com.yusufteker.pulse.shared.api.RefreshTokenRequest
import com.yusufteker.pulse.shared.api.RegisterRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.or
import java.time.Instant

/**
 * Defines all Authentication endpoints.
 */
fun Route.authRoutes() {
    route("/auth") {
        
        // --- 1. REGISTER ENDPOINT ---
        post("/register") { // auth/register geldiğinde
            // Client'tan (Uygulamadan) gelen JSON verisini Kotlin objesine dönüştürüyoruz.
            val request = call.receive<RegisterRequest>()
            
            // Veritabanında (Exposed ORM kullanarak) bu email veya username daha önce alınmış mı kontrol ediyoruz.
            val existingUser = dbQuery {
                UserEntity.find { 
                    (UsersTable.email eq request.email) or (UsersTable.username eq request.username)
                }.firstOrNull()
            }
            if (existingUser != null) {
                if (existingUser.email == request.email) {
                    call.respond(HttpStatusCode.Conflict, "Email already in use")
                } else {
                    call.respond(HttpStatusCode.Conflict, "Username already in use")
                }
                return@post
            }

            // Güvenlik: Gelen düz metin şifreyi BCrypt ile geri döndürülemez şekilde hashliyoruz.
            val hashedPassword = HashingService.hashPassword(request.password)
            
            // Veritabanına yeni bir kullanıcı kaydediyoruz.
            val newUser = dbQuery {
                UserEntity.new {
                    name = request.name
                    username = request.username
                    email = request.email
                    passwordHash = hashedPassword
                    createdAt = Instant.now()
                }
            }

            // Kayıt olan kullanıcı için hemen yetkilendirme (Access) ve yenileme (Refresh) token'ları üretiyoruz.
            // accessToken -> userId, email,expirationDate,  hashli secret içerir. 2 saat geçerlidir.
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

            // İşlem başarılı! Uygulamaya token'ları ve kullanıcı bilgilerini dönüyoruz.
            call.respond(HttpStatusCode.Created, AuthResponse(accessToken, refreshToken, newUser.id.value, newUser.name, newUser.avatarId))
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
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
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
            call.respond(HttpStatusCode.OK, AuthResponse(accessToken, refreshToken, user.id.value, user.name, user.avatarId))
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
                call.respond(HttpStatusCode.Unauthorized, "Invalid or expired refresh token")
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
            call.respond(HttpStatusCode.OK, AuthResponse(newAccessToken, request.refreshToken, user.id.value, user.name, user.avatarId))
        }

        // --- 4. PROTECTED ENDPOINT (Sadece giriş yapmış kullanıcılar girebilir) ---
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
                    
                    val followersCount = com.yusufteker.pulse.server.database.tables.FollowerEntity.find { com.yusufteker.pulse.server.database.tables.FollowersTable.followedId eq userId }.count().toInt()
                    val followingCount = com.yusufteker.pulse.server.database.tables.FollowerEntity.find { com.yusufteker.pulse.server.database.tables.FollowersTable.followerId eq userId }.count().toInt()
                    val postsCount = com.yusufteker.pulse.server.database.tables.PostEntity.find { com.yusufteker.pulse.server.database.tables.PostsTable.authorId eq userId }.count().toInt()

                    com.yusufteker.pulse.shared.api.UserProfileResponse(
                        id = user.id.value,
                        name = user.name,
                        username = user.username,
                        email = user.email,
                        avatarId = user.avatarId,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        postsCount = postsCount
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

                val request = call.receive<com.yusufteker.pulse.shared.api.UpdateProfileRequest>()

                // name ve avatarId güncellemesini veritabanında yapıyoruz.
                val user = dbQuery {
                    val entity = UserEntity.findById(userId)
                    if (entity != null) {
                        entity.name = request.name
                        entity.avatarId = request.avatarId
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
