package me.anno.export.reflect

import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.maps.Maps.removeIf
import me.anno.utils.types.Arrays.indexOf2

/**
 * Uses [Kotlyn Reflect](https://github.com/AntonioNoack/KotlynReflect) instead of standard reflections library
 * to reduce exported package size
 * */
object Reflections {

    private fun eq(byte: Byte, v: Int): Boolean = byte.toInt().and(255) == v
    private fun isClassFile(name: String, bytes: ByteArray): Boolean {
        return name.endsWith(".class") && bytes.size >= 24 && // 24 bytes is the minimum size according to ChatGPT
                eq(bytes[0], 0xca) && eq(bytes[1], 0xfe) && eq(bytes[2], 0xba) && eq(bytes[3], 0xbe)
    }

    private val searchedList = listOf(
        "kotlin/reflect/",
        "kotlin/jvm/internal/Property",
        "kotlin/jvm/internal/Reflection",
        "kotlin/jvm/internal/MutableProperty",
        "kotlin/jvm/JvmClassMappingKt",
    )

    private val searched = searchedList.map { it.encodeToByteArray() }

    private fun replaceKotlinReflect(haystack: ByteArray, needle: ByteArray) {
        var nextSearchPos = 0
        while (true) {
            nextSearchPos = haystack.indexOf2(needle, nextSearchPos)
            if (nextSearchPos < 0) return
            haystack[nextSearchPos + 4] = 'y'.code.toByte()
            nextSearchPos += needle.size
        }
    }

    private fun replaceKotlinReflect(bytes: ByteArray) {
        for (searched0 in searched) {
            replaceKotlinReflect(bytes, searched0)
        }
    }

    fun replaceReflections(sources: HashMap<String, ByteArray>) {
        sources.removeIf { (key, _) -> searchedList.any2 { sl -> key.startsWith(sl) } }
        sources.replaceAll { name, bytes ->
            if (isClassFile(name, bytes)) {
                replaceKotlinReflect(bytes)
            }
            bytes
        }
    }
}