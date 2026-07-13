import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

fun main() {
    val map = mapOf(1 to "Ali", 2 to "Veli")
    val str = Json.encodeToString(map)
    println("Encoded: $str")
    val decoded = Json.decodeFromString<Map<Int, String>>(str)
    println("Decoded: $decoded")
}
