package com.yusufteker.pulse.server

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun testMapSerialization() {
        val map = mapOf(1 to "Ali", 2 to "Veli")
        val str = Json.encodeToString(map)
        println("Encoded: $str")
        val decoded = Json.decodeFromString<Map<Int, String>>(str)
        println("Decoded: $decoded")
        assertEquals(map, decoded)
    }
}
