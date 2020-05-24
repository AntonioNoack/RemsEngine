package me.anno.io.utils

import me.anno.io.base.BaseWriter
import me.anno.io.config.ConfigEntry

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

}