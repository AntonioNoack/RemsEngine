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
open class SimpleCache(val name: String, var timeoutMillis: Long) : Comparable<SimpleCache> {

    val cache = HashMap<Any?, CacheEntry>(512)

    override fun compareTo(other: SimpleCache): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        for (it in cache.values) it.destroy()
        cache.clear()
    }

    fun remove(filter: (Any?, CacheEntry) -> Boolean): Int {
        return cache.removeIf { (k, v) ->
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

    fun <V, R : ICacheData> getEntry(key: V, generator: (V) -> R?): R? {

        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = cache.getOrPut(key) { CacheEntry(timeoutMillis) }
        if (entry.hasBeenDestroyed) entry.reset(timeoutMillis)

        val needsGenerator = entry.needsGenerator
        entry.update(timeoutMillis)

        if (needsGenerator) {
            entry.hasGenerator = true
            entry.data = generateSafely(key, generator)
        }

        @Suppress("UNCHECKED_CAST")
        return if (entry.hasBeenDestroyed) null else entry.data as? R
    }

    fun update() {
        // avoiding allocations for clean memory debugging XD
        cache.removeIf { (_, value) ->
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