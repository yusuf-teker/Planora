package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FollowersTable : IntIdTable("followers") {
    val followerId = reference("follower_id", UsersTable)
    val followedId = reference("followed_id", UsersTable)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(followerId, followedId)
    }
}

class FollowerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FollowerEntity>(FollowersTable)

    var follower by UserEntity referencedOn FollowersTable.followerId
    var followed by UserEntity referencedOn FollowersTable.followedId
    var createdAt by FollowersTable.createdAt
}
