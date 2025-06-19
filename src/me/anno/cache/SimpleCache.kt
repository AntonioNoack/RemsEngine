package me.anno.cache

import me.anno.Build
import me.anno.Time.nanoTime
import me.anno.cache.CacheSection.Companion.generateSafely
import me.anno.cache.CacheSection.Companion.registerCache
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager

/**
 * Single-Threaded access only
 * */
open class SimpleCache<K, V: Any>(val name: String, var timeoutMillis: Long) : Comparable<SimpleCache<*, *>> {

    val values = HashMap<K, AsyncCacheData<V>>(512)

    override fun compareTo(other: SimpleCache<*, *>): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        for (it in values.values) it.destroy()
        values.clear()
    }

    fun remove(filter: (K, AsyncCacheData<V>) -> Boolean): Int {
        return values.removeIf { (k, v) ->
            if (filter(k, v)) {
                v.destroy()
                true
            } else false
        }
    }

    private fun checkKey(key: Any?) {
        if (Build.isDebug && key != null) {
            @Suppress("KotlinConstantConditions")
            if (key != key) assertFail("${key::class.simpleName}.equals() is incorrect!")
            if (key.hashCode() != key.hashCode()) assertFail("${key::class.simpleName}.hashCode() is inconsistent!")
        }// else we assume that it's fine
    }

    fun getEntry(key: K, generator: (K, AsyncCacheData<V>) -> Unit): AsyncCacheData<V> {

        checkKey(key)

        var entry = values[key]
        val isGenerating = entry == null || entry.hasBeenDestroyed
        if (isGenerating) {
            entry = AsyncCacheData()
            values[key] = entry
        }

        entry.update(timeoutMillis)

        if (isGenerating) {
            generateSafely(key, entry, generator)
        }

        return entry
    }

    fun update() {
        // avoiding allocations for clean memory debugging XD
        values.removeIf { (_, value) ->
            if (nanoTime > value.timeoutNanoTime) {
                value.destroy()
                true
            } else false
        }
    }

    init {
        registerCache(::update, ::clear)
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(SimpleCache::class)
    }
}