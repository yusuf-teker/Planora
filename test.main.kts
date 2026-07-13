@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val map = mapOf(1 to "Ali", 2 to "Veli")
val str = Json.encodeToString(map)
println("Encoded: $str")
try {
    val decoded = Json.decodeFromString<Map<Int, String>>(str)
    println("Decoded: $decoded")
} catch (e: Exception) {
    println("Decode error: ${e.message}")
}
