package com.yusufteker.pulse.server.database.tables

import org.jetbrains.exposed.sql.Table
import com.yusufteker.pulse.shared.api.TaskType
import com.yusufteker.pulse.shared.api.TaskStatus
import com.yusufteker.pulse.shared.api.TaskVisibility

/**
 * Kullanıcıların oluşturduğu Görev (Task), Not (Note) veya Etkinlikleri (Event) tutan tablo.
 * Bu tablo, takvime yerleşecek olan planların merkezidir.
 */
object TasksTable : Table("tasks") {
    // Görevin benzersiz kimliği. Çevrimdışı (offline) yaratılabilmesi için UUID formatında tutulur.
    val id = varchar("id", 36)
    
    // Görevi veya notu oluşturan kullanıcının kimliği (Sahibi).
    val creatorId = integer("creator_id").references(UsersTable.id)
    
    // Planın başlığı (Örn: "Doktora gidilecek", "Market alışverişi")
    val title = varchar("title", 255)
    
    // Varsa görevle ilgili uzun açıklamalar veya notlar.
    val description = text("description").nullable()
    
    // Planın başlama zamanı (Unix Timestamp). Takvimde hangi gün ve saatte gösterileceğini belirler.
    val startTime = long("start_time")
    
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

    override val primaryKey = PrimaryKey(id)
}
