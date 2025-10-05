package me.anno.io.utils

import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.saveable.Saveable
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.maps.Maps.removeIf
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToLong
import me.anno.utils.types.Booleans.toLong

/**
 * can be used for config easily :D
 * */
open class StringMap(
    capacity: Int = 16,
    val sortKeysWhenSaving: Boolean = true
) : Saveable(), MutableMap<String, Any?> {

    constructor() : this(16)

    constructor(data: Map<String, Any?>) : this(data.size + 16) {
        map.putAll(data)
    }

    var wasChanged = false
        private set

    private val map = HashMap<String, Any?>(capacity)

    override val className: String get() = "SMap"
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

    override fun setProperty(name: String, value: Any?) {
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

    // parameter can be called key or parameterName
    // both are requested by Map and Saveable
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override operator fun get(key: String): Any? {
        synchronized(this) {
            onSyncAccess()
            return map[key]
        }
    }

    /**
     * returns whether collection was changed
     * */
    override operator fun set(propertyName: String, value: Any?): Boolean {
        return synchronized(this) {
            onSyncAccess()
            val old = map.put(propertyName, value)
            if (old != value) {
                wasChanged = true
                true
            } else false
        }
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
        if (from === this || from.isEmpty()) return
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
            val changed = map.removeIf { it.key in keys } > 0
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

    open fun onSyncAccess() {}

    operator fun get(key: String, default: FileReference): FileReference {
        return when (val value = this[key]) {
            is FileReference -> value
            null -> {
                set(key, default)
                default
            }
            else -> getReference(value.toString().toGlobalFile())
        }
    }

    operator fun get(key: String, default: Float): Float {
        return get(key, default.toDouble()).toFloat()
    }

    operator fun get(key: String, default: Double): Double {
        val value = this[key]
        if (this[key] == null) set(key, default)
        return AnyToDouble.getDouble(value, default)
    }

    operator fun get(key: String, default: Int): Int {
        return get(key, default.toLong()).toInt()
    }

    operator fun get(key: String, default: Long): Long {
        val value = this[key]
        if (value == null) set(key, default)
        return AnyToLong.getLong(value, default)
    }

    operator fun get(key: String, default: Boolean): Boolean {
        return when (val value = this[key]) {
            is Boolean -> value
            is String -> when (value.lowercase()) {
                "true", "t", "1" -> true
                "false", "f", "0" -> false
                else -> default
            }
            null -> {
                set(key, default) // must be here for proper default
                return default
            }
            else -> get(key, default.toLong()) != 0L
        }
    }

    fun addAll(map: Map<String, Any>): StringMap {
        putAll(map)
        return this
    }

    private val sm = SaveMaybe()
    fun saveMaybe(name: String) {
        sm.saveMaybe(name, { wasChanged }, { save(name) })
    }

    fun save(name: String) {
        val str = synchronized(this) {
            wasChanged = false
            this.toString()
        }
        ConfigBasics.save(name, str)
    }

    override fun isDefaultValue() = false
}