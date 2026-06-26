package com.yusufteker.pulse.server.database

import com.yusufteker.pulse.server.database.tables.PostEntity
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.server.security.HashingService
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object DatabaseSeeder {
    fun seed() {
        transaction {

            println("Users: ${UserEntity.count()}")
            println("Posts: ${PostEntity.count()}")
            val dummyUser = UserEntity.new {
                name = "Dummy User"
                email = "dummy@pulse.com"
                passwordHash = HashingService.hashPassword("password")
                createdAt = Instant.now()
            }



            if (PostEntity.count() == 0L) {
                for (i in 1..50) {
                    PostEntity.new {
                        author = dummyUser
                        content =
                            "This is a dummy post #$i for the feed. Testing the paging functionality in Compose Multiplatform."
                        createdAt = Instant.now().minusSeconds((50 - i) * 3600L)
                        likesCount = (1..100).random()
                        commentsCount = (0..20).random()
                    }
                }
            }
            println("Seed completed.")

        }
    }
}
