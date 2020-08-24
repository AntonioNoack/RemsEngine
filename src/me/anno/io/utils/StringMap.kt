package me.anno.io.utils

import me.anno.gpu.texture.FilteringMode
import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigEntry
import me.anno.ui.editor.explorer.toAllowedFilename
import me.anno.utils.OS
import org.joml.Vector3f
import java.io.File

/**
 * can be used for config easily :D
 * */
open class StringMap(
    capacity: Int = 16,
    val sortKeysWhenSaving: Boolean = true,
    val saveDefaultValues: Boolean = false
    ): ConfigEntry(), MutableMap<String, Any?> {

    var wasChanged = false
    val map = HashMap<String, Any?>(capacity)

    override fun getClassName(): String = "SMap"
    override fun getApproxSize(): Int = 1_000_000
    override fun save(writer: BaseWriter) {
        super.save(writer)
        if(!map.containsKey("notice")) writer.writeString("notice", "#thisIsJSON")
        // sorting keys for convenience
        val leMap = if(sortKeysWhenSaving) map.toSortedMap() else map
        for((name, value) in leMap){
            writer.writeSomething(this, name, value, saveDefaultValues)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        if(name != "notice") map[name] = value
    }

    operator fun get(key: String, addIfMissing: Any?): Any? {
        val value = map[key]
        return if(value == null){
            map[key] = addIfMissing
            addIfMissing
        } else value
    }

    override operator fun get(key: String) = map[key]
    operator fun set(key: String, value: Any?){
        wasChanged = true
        map[key] = value
    }

    override fun containsKey(key: String) = map.containsKey(key)
    override fun containsValue(value: Any?) = map.containsValue(value)

    override val entries get() = map.entries
    override val keys get() = map.keys
    override val values get() = map.values
    override val size get() = map.size

    override fun clear() = map.clear()
    override fun isEmpty() = map.isEmpty()
    override fun put(key: String, value: Any?): Any? = map.put(key, value)
    override fun putAll(from: Map<out String, Any?>) = map.putAll(from)
    override fun remove(key: String): Any? = map.remove(key)

    operator fun get(key: String, default: String): String {
        return when(val value = this[key]){
            is String -> value
            null -> default
            else -> value.toString()
        }
    }

    fun parseFile(str0: String): File {
        var str = str0
        if(str.startsWith("~") && OS.isWindows){
            str = "%HOMEPATH%/${str.substring(1)}"
        }
        // make this file valid; no matter what
        str = toAllowedFilename(str) ?: "tmp"
        return File(str)
    }

    operator fun get(key: String, default: File): File {
        return when(val value = this[key]){
            is File -> value
            is String -> parseFile(value)
            null -> default
            else -> parseFile(value.toString())
        }
    }

    operator fun get(key: String, default: Float): Float {
        return when(val value = this[key]){
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is String -> value.toFloatOrNull() ?: default
            null -> default
            else -> value.toString().toFloatOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Double): Double {
        return when(val value = this[key]){
            is Float -> value.toDouble()
            is Double -> value
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            null -> default
            else -> value.toString().toDoubleOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Int): Int {
        return when(val value = this[key]){
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            null -> default
            else -> value.toString().toIntOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Vector3f): Vector3f {
        return when(val value = this[key]){
            is Vector3f -> value
            else -> default
        }
    }

    operator fun get(key: String, default: Boolean): Boolean {
        return when(val value = this[key]){
            is Boolean -> value
            is Int -> value != 0
            is Long -> value != 0L
            is Float -> !value.isNaN() && value != 0f
            is Double -> !value.isNaN() && value != 0.0
            is String -> {
                when(value.toLowerCase()){
                    "true", "t" -> true
                    "false", "f" -> false
                    else -> default
                }
            }
            null -> default
            else -> value.toString().toBoolean()
        }
    }

    operator fun get(key: String, default: FilteringMode): FilteringMode {
        return when(val value = this[key]){
            is Boolean -> if(value) FilteringMode.NEAREST else FilteringMode.LINEAR
            is Int -> default.find(value)
            is String -> {
                when(value.toLowerCase()){
                    "true", "t", "nearest" -> FilteringMode.NEAREST
                    "false", "f", "linear" -> FilteringMode.LINEAR
                    "cubic", "bicubic" -> FilteringMode.CUBIC
                    else -> default
                }
            }
            else -> default
        }
    }

    fun addAll(map: Map<String, Any>): StringMap {
        putAll(map)
        return this
    }

    override fun isDefaultValue() = false

}