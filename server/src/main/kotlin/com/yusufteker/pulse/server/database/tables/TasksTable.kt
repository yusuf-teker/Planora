package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskVisibility

/**
 * Kullanıcıların oluşturduğu Görev (Task), Not (Note) veya Etkinlikleri (Event) tutan tablo.
 * Bu tablo, takvime yerleşecek olan planların merkezidir.
 */
object TasksTable : IdTable<String>("tasks") {
    // Görevin benzersiz kimliği. Çevrimdışı (offline) yaratılabilmesi için UUID formatında tutulur.
    override val id: Column<EntityID<String>> = varchar("id", 100).entityId()
    
    // Görevi veya notu oluşturan kullanıcının kimliği (Sahibi).
    val creatorId = reference("creator_id", UsersTable).index()
    
    // Planın başlığı (Örn: "Doktora gidilecek", "Market alışverişi")
    val title = varchar("title", 255)
    
    // Varsa görevle ilgili uzun açıklamalar veya notlar.
    val description = text("description").nullable()
    
    // Planın başlama zamanı (Unix Timestamp). Takvimde hangi gün ve saatte gösterileceğini belirler.
    val startTime = long("start_time").index()
    
    // Planın bitiş zamanı. Sadece belli bir saatte biten etkinlikler veya deadline'lar için kullanılır, opsiyoneldir.
    val endTime = long("end_time").nullable()
    
    // Kaydın tipi. (NOTE: Sadece yazılı not, TASK: Yapılacak iş, EVENT: Takvim etkinliği)
    val type = enumerationByName("type", 50, TaskType::class)
    
    // Görevin durumu. (PENDING: Bekliyor, IN_PROGRESS: Yapılıyor, COMPLETED: Tamamlandı)
    val status = enumerationByName("status", 50, TaskStatus::class)
    
    // Gizlilik durumu. 
    // PRIVATE: Sadece oluşturan kişi görebilir. 
    // ROOM_SHARED: Seçilen Plan Odalarındaki üyeler de takvimlerinde bu görevi görebilir.
    val visibility = enumerationByName("visibility", 50, TaskVisibility::class)
    
    // Düzenli tekrar eden bir plan mı? (Örn: Haftada 3 gün)
    val isRecurring = bool("is_recurring").default(false)
    
    // Tekrar kuralı (Örn: RRULE formatı "FREQ=WEEKLY;BYDAY=MO,WE,FR")
    val recurrenceRule = varchar("recurrence_rule", 255).nullable()
    
    // Saati kesin mi yoksa "yaklaşık/esnek" bir zamanda mı yapılacak?
    val isFlexible = bool("is_flexible").default(false)
    
    // İsteğe bağlı mı yoksa yapılması zorunlu mu?
    val isOptional = bool("is_optional").default(false)
    
    // Süresi geçtiğinde ertelenebilir mi, yoksa kesin tarihli (deadline) mi?
    val isPostponable = bool("is_postponable").default(true)
    
    // Tüm gün etkinliği mi?
    val isAllDay = bool("is_all_day").default(false)
    
    // AI tarafından üretilen meta veriler (JSON string)
    val aiMetadata = text("ai_metadata").nullable()
    
    // Bildirim süreleri (JSON list of ints)
    val reminders = text("reminders").nullable()
    
    // Tipe özel veriler (Note, Event, Task) (JSON string for ItemDetails)
    val specificDetails = text("specific_details").nullable()
    
    // Ortak etiketler (JSON list of strings)
    val tags = text("tags").nullable()
    
    // Renk (Örn: #FF0000)
    val color = varchar("color", 50).nullable()
    
    // Üst öğe ID'si (Örn: Bu bir alt görevse, bağlı olduğu Event'in veya Task'ın ID'si)
    val parentId = varchar("parent_id", 100).nullable()

    override val primaryKey = PrimaryKey(id)
}

class TaskEntity(id: EntityID<String>) : org.jetbrains.exposed.dao.Entity<String>(id) {
    companion object : org.jetbrains.exposed.dao.EntityClass<String, TaskEntity>(TasksTable)

    var creator by UserEntity referencedOn TasksTable.creatorId
    var title by TasksTable.title
    var description by TasksTable.description
    var startTime by TasksTable.startTime
    var endTime by TasksTable.endTime
    var type by TasksTable.type
    var status by TasksTable.status
    var visibility by TasksTable.visibility
    var isRecurring by TasksTable.isRecurring
    var recurrenceRule by TasksTable.recurrenceRule
    var isFlexible by TasksTable.isFlexible
    var isOptional by TasksTable.isOptional
    var isPostponable by TasksTable.isPostponable
    var isAllDay by TasksTable.isAllDay
    
    var aiMetadata by TasksTable.aiMetadata
    var reminders by TasksTable.reminders
    var specificDetails by TasksTable.specificDetails
    var tags by TasksTable.tags
    var color by TasksTable.color
    var parentId by TasksTable.parentId
}
