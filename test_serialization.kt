import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }

@Serializable
data class SubTask(val id: String, val title: String, val isDone: Boolean)

@Serializable
sealed class ItemDetails {
    @Serializable
    data class Task(
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val subtasks: List<SubTask> = emptyList(),
        val deadline: Long? = null,
        val estimatedMinutes: Int? = null
    ) : ItemDetails()
}

fun main() {
    val details: ItemDetails = ItemDetails.Task(deadline = 12345L)
    val jsonString = Json.encodeToString(details)
    println("Encoded: $jsonString")
    
    val decoded = Json.decodeFromString<ItemDetails>(jsonString)
    println("Decoded: $decoded")
}
