package com.yusufteker.pulse.server.database

import com.yusufteker.pulse.server.database.tables.PostEntity
import com.yusufteker.pulse.server.database.tables.UserEntity
import com.yusufteker.pulse.server.database.tables.CommentEntity
import com.yusufteker.pulse.server.security.HashingService
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object DatabaseSeeder {
    fun seed() {
        transaction {

            println("Users: ${UserEntity.count()}")
            println("Posts: ${PostEntity.count()}")
            
            var dummyUser = UserEntity.all().find { it.email == "dummy@pulse.com" }
            if (dummyUser == null) {
                dummyUser = UserEntity.new {
                    name = "Dummy User"
                    username = "dummy_user"
                    email = "dummy@pulse.com"
                    passwordHash = HashingService.hashPassword("password")
                    createdAt = Instant.now()
                }
            }

            var secondaryUser = UserEntity.all().find { it.email == "test@pulse.com" }
            if (secondaryUser == null) {
                secondaryUser = UserEntity.new {
                    name = "Test User"
                    username = "test_user"
                    email = "test@pulse.com"
                    passwordHash = HashingService.hashPassword("password")
                    createdAt = Instant.now()
                    avatarId = "avatar_3"
                }
            }

            if (PostEntity.count() == 0L) {
                for (i in 1..50) {
                    val commentsC = (0..5).random()
                    val newPost = PostEntity.new {
                        author = dummyUser
                        content =
                            "This is a dummy post #$i for the feed. Testing the paging functionality in Compose Multiplatform."
                        createdAt = Instant.now().minusSeconds((50 - i) * 3600L)
                        likesCount = (1..100).random()
                        commentsCount = commentsC
                    }
                    
                    // Add some comments
                    val addedComments = mutableListOf<CommentEntity>()
                    for(j in 1..commentsC) {
                        val c = CommentEntity.new {
                            author = dummyUser
                            post = newPost
                            content = "This is a great dummy comment #$j for post #$i"
                            createdAt = Instant.now().minusSeconds(j * 600L)
                        }
                        addedComments.add(c)
                    }
                    
                    // Add a reply to the first comment if exists
                    if (addedComments.isNotEmpty()) {
                        CommentEntity.new {
                            author = dummyUser
                            post = newPost
                            parentComment = addedComments.first()
                            content = "I totally agree with this! (Reply)"
                            createdAt = Instant.now()
                        }
                        newPost.commentsCount += 1
                    }
                }
            }
            // Fix fake comment counts from previous seeders
            exec("UPDATE posts SET comments_count = (SELECT COUNT(*) FROM comments WHERE comments.post_id = posts.id)")
            
            // Seed Followers (Dummy follows Test, Test follows Dummy)
            if (com.yusufteker.pulse.server.database.tables.FollowerEntity.count() == 0L) {
                com.yusufteker.pulse.server.database.tables.FollowerEntity.new {
                    follower = dummyUser!!
                    followed = secondaryUser!!
                    createdAt = Instant.now()
                }
                com.yusufteker.pulse.server.database.tables.FollowerEntity.new {
                    follower = secondaryUser!!
                    followed = dummyUser!!
                    createdAt = Instant.now()
                }
            }

            // Seed Bookmarks (Dummy bookmarks first 5 posts)
            if (com.yusufteker.pulse.server.database.tables.BookmarkEntity.count() == 0L) {
                val posts = PostEntity.all().limit(5).toList()
                posts.forEach { post ->
                    com.yusufteker.pulse.server.database.tables.BookmarkEntity.new {
                        user = dummyUser!!
                        this.post = post
                        createdAt = Instant.now()
                    }
                }
            }
            
            println("Seed completed.")
        }
    }
}
