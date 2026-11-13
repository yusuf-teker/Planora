package com.yusufteker.planora.server.database.tables

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Exposed table mapping for per-app remote version & force update configuration (`app_configs`).
 */
object AppConfigsTable : IdTable<String>("app_configs") {
    override val id: Column<EntityID<String>> = varchar("app_id", 50).entityId()
    override val primaryKey = PrimaryKey(id)

    val minSupportedVersion = varchar("min_supported_version", 30).default("1.0.0")
    val latestVersion = varchar("latest_version", 30).default("1.0.0")
    val forceUpdateTitleTr = varchar("force_update_title_tr", 200).default("Zorunlu Güncelleme")
    val forceUpdateTitleEn = varchar("force_update_title_en", 200).default("Update Required")
    val forceUpdateMessageTr = text("force_update_message_tr")
    val forceUpdateMessageEn = text("force_update_message_en")
    val storeUrlAndroid = varchar("store_url_android", 500).default("")
    val storeUrlIos = varchar("store_url_ios", 500).default("")
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

class AppConfigEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AppConfigEntity>(AppConfigsTable)

    var minSupportedVersion by AppConfigsTable.minSupportedVersion
    var latestVersion by AppConfigsTable.latestVersion
    var forceUpdateTitleTr by AppConfigsTable.forceUpdateTitleTr
    var forceUpdateTitleEn by AppConfigsTable.forceUpdateTitleEn
    var forceUpdateMessageTr by AppConfigsTable.forceUpdateMessageTr
    var forceUpdateMessageEn by AppConfigsTable.forceUpdateMessageEn
    var storeUrlAndroid by AppConfigsTable.storeUrlAndroid
    var storeUrlIos by AppConfigsTable.storeUrlIos
    var updatedAt by AppConfigsTable.updatedAt
}

