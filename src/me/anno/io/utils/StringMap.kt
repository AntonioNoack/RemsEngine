package me.anno.io.utils

import me.anno.Engine
import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.texture.Filtering
import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigBasics
import me.anno.io.config.ConfigEntry
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import me.anno.utils.hpc.ProcessingQueue
import org.joml.Vector3f
import java.io.File
import java.util.*
import kotlin.math.min

/**
 * can be used for config easily :D
 * */
open class StringMap(
    capacity: Int = 16,
    val sortKeysWhenSaving: Boolean = true
) : ConfigEntry(), MutableMap<String, Any?> {

    constructor(data: Map<String, Any?>) : this(data.size + 16) {
        map.putAll(data)
    }

    var wasChanged = false
    val map = HashMap<String, Any?>(capacity)

    override val className get() = "SMap"
    override val approxSize get() = 1_000_000
    override fun save(writer: BaseWriter) {
        super.save(writer)
        // avoid locking up the program up while waiting for IO
        val mapClone = synchronized(this) { HashMap(map) }
        if (!mapClone.containsKey("notice")) writer.writeString("notice", "#thisIsJSON")
        // sorting keys for convenience
        val leMap = if (sortKeysWhenSaving) mapClone.toSortedMap() else mapClone
        for ((name, value) in leMap) {
            writer.writeSomething(this, name, value, true)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        if (name != "notice") synchronized(this) {
            onSyncAccess()
            map[name] = value
        }
    }

    open operator fun get(key: String, addIfMissing: () -> StringMap): StringMap {
        synchronized(this) {
            onSyncAccess()
            val value = map[key]
            return if (value !is StringMap) {
                val value2 = addIfMissing()
                map[key] = value2
                wasChanged = true
                value2
            } else value
        }
    }

    open operator fun get(key: String, addIfMissing: Any?): Any? {
        synchronized(this) {
            onSyncAccess()
            val value = map[key]
            return if (value == null) {
                map[key] = addIfMissing
                addIfMissing
            } else value
        }
    }

    override operator fun get(key: String): Any? {
        synchronized(this) {
            onSyncAccess()
            return map[key]
        }
    }

    override operator fun set(key: String, value: Any?): Boolean {
        synchronized(this) {
            onSyncAccess()
            val hadKey = key in map
            val old = map.put(key, value)
            if (hadKey) println("setting $key to $value, was $old")
            if (old != value) wasChanged = true
        }
        return true
    }

    override fun containsKey(key: String): Boolean {
        synchronized(this) {
            onSyncAccess()
            return map.containsKey(key)
        }
    }

    override fun containsValue(value: Any?): Boolean {
        synchronized(this) {
            onSyncAccess()
            return map.containsValue(value)
        }
    }

    override val entries get() = map.entries
    override val keys get() = map.keys
    override val values get() = map.values
    override val size get() = map.size

    override fun clear() {
        synchronized(this) {
            onSyncAccess()
            if (isNotEmpty()) wasChanged = true
            map.clear()
        }
    }

    override fun isEmpty() = synchronized(this) { map.isEmpty() }

    override fun put(key: String, value: Any?) {
        synchronized(this) {
            onSyncAccess()
            val old = map.put(key, value)
            if (old != value) wasChanged = true
        }
    }

    override fun putAll(from: Map<out String, Any?>) {
        if (from.isEmpty() || from === this) return
        synchronized(this) {
            wasChanged = true
            onSyncAccess()
            map.putAll(from)
        }
    }

    override fun remove(key: String): Any? = synchronized(this) {
        synchronized(this) {
            onSyncAccess()
            if (key in map) {
                map.remove(key)
                wasChanged = true
            }
        }
    }

    fun removeAll(keys: Collection<String>) {
        synchronized(this) {
            onSyncAccess()
            val changed = map.entries.removeIf { it.key in keys }
            if (changed) wasChanged = true
        }
    }

    operator fun get(key: String, default: String): String {
        return when (val value = this[key]) {
            is String -> value
            null -> {
                set(key, default)
                default
            }
            else -> value.toString()
        }
    }

    open fun onSyncAccess() {

    }

    fun parseFile(str0: String): File {
        var str = str0
            .replace('\\', '/')
        if (str.startsWith("~") && OS.isWindows) {
            str = "%HOMEPATH%/${str.substring(1)}"
        }

        // make this file valid; no matter what
        val str2 = str.split(":/")
        str = str2.subList(0, min(2, str2.size))
            .joinToString(":/") {
                it.split('/').joinToString("/") { name -> name.toAllowedFilename() ?: "x" }
            }

        return File(str)
    }

    @Deprecated("You should use FileReferences")
    operator fun get(key: String, default: File): File {
        return when (val value = this[key]) {
            is File -> value
            is FileReference -> value.unsafeFile
            is String -> parseFile(value)
            null -> {
                set(key, default)
                default
            }
            else -> parseFile(value.toString())
        }
    }

    operator fun get(key: String, default: FileReference): FileReference {
        return when (val value = this[key]) {
            is File -> getReference(value)
            is FileReference -> value
            is String -> getReference(parseFile(value))
            null -> {
                set(key, default)
                default
            }
            else -> getReference(parseFile(value.toString()))
        }
    }

    operator fun get(key: String, default: Float): Float {
        return when (val value = this[key]) {
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is String -> value.toFloatOrNull() ?: default
            null -> {
                set(key, default)
                default
            }
            else -> value.toString().toFloatOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Double): Double {
        return when (val value = this[key]) {
            is Float -> value.toDouble()
            is Double -> value
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            null -> {
                set(key, default)
                default
            }
            else -> value.toString().toDoubleOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Int): Int {
        return when (val value = this[key]) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            null -> {
                set(key, default)
                default
            }
            else -> value.toString().toIntOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Long): Long {
        return when (val value = this[key]) {
            is Int -> value.toLong()
            is Long -> value
            is Float -> value.toLong()
            is Double -> value.toLong()
            is String -> value.toLongOrNull() ?: default
            null -> {
                set(key, default)
                default
            }
            else -> value.toString().toLongOrNull() ?: default
        }
    }

    operator fun get(key: String, default: Vector3f): Vector3f {
        return when (val value = this[key]) {
            is Vector3f -> value
            else -> default
        }
    }

    operator fun get(key: String, default: Boolean): Boolean {
        return when (val value = this[key]) {
            is Boolean -> value
            is Int -> value != 0
            is Long -> value != 0L
            is Float -> !value.isNaN() && value != 0f
            is Double -> !value.isNaN() && value != 0.0
            is String -> {
                when (value.lowercase(Locale.getDefault())) {
                    "true", "t" -> true
                    "false", "f" -> false
                    else -> default
                }
            }
            null -> {
                set(key, default)
                default
            }
            else -> value.toString().toBoolean()
        }
    }

    operator fun get(key: String, default: Filtering): Filtering {
        return when (val value = this[key]) {
            is Boolean -> if (value) Filtering.NEAREST else Filtering.LINEAR
            is Int -> default.find(value)
            is String -> {
                when (value.lowercase(Locale.getDefault())) {
                    "true", "t", "nearest" -> Filtering.NEAREST
                    "false", "f", "linear" -> Filtering.LINEAR
                    "cubic", "bicubic" -> Filtering.CUBIC
                    else -> default
                }
            }
            else -> {
                set(key, default)
                default
            }
        }
    }

    fun addAll(map: Map<String, Any>): StringMap {
        putAll(map)
        return this
    }

    fun <V> getAnimated(key: String, type: Type): AnimatedProperty<V> {
        val v = when (val value = this[key]) {
            is AnimatedProperty<*> -> {
                return if (value.type == type) {
                    value as AnimatedProperty<V>
                } else {
                    val v2 = AnimatedProperty<V>(type)
                    v2.copyFrom(type)
                    set(key, v2)
                    v2
                }
            }
            is Int -> value
            is Long -> value
            is Float -> value
            is Double -> value
            is String -> value
            null -> type.defaultValue
            else -> value
        }
        val v2 = AnimatedProperty<V>(type)
        v2.set(v as V)
        set(key, v2)
        return v2
    }

    private val saveDelay = 1_000_000_000L
    private var lastSaveTime = gameTime - saveDelay - 1
    fun saveMaybe(name: String) {
        if (wasChanged) {
            synchronized(this) {
                if (gameTime - lastSaveTime >= saveDelay || Engine.shutdown) {// only save every 1s
                    // delay in case it needs longer
                    lastSaveTime = gameTime + 60_000_000_000L
                    savingQueue += {
                        save(name)
                        lastSaveTime = gameTime
                    }
                } else {
                    savingQueue += {
                        Thread.sleep(10)
                        saveMaybe(name)
                    }
                }
            }
        }
    }

    fun save(name: String) {
        val str = synchronized(this) {
            wasChanged = false
            this.toString()
        }
        ConfigBasics.save(name, str)
    }

    override fun isDefaultValue() = false

    companion object {
        val savingQueue = ProcessingQueue("StringMap-Saving")
    }

}