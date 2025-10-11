package me.anno.cache

import me.anno.Build
import me.anno.cache.CacheSection.Companion.checkKey
import me.anno.cache.CacheSection.Companion.getKeyName
import me.anno.cache.CacheSection.Companion.registerCache
import me.anno.cache.CacheSection.Companion.runAsync
import me.anno.utils.InternalAPI
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet

/**
 * CacheSection, but with two keys.
 * */
open class DualCacheSection<K1, K2, V : Any>(val name: String) : Comparable<DualCacheSection<*, *, *>> {

    val dualCache = KeyPairMap<K1, K2, AsyncCacheData<V>>(512)

    override fun compareTo(other: DualCacheSection<*, *, *>): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        synchronized(dualCache) {
            dualCache.forEach { _, _, v -> v.destroy() }
            dualCache.clear()
        }
    }

    fun removeDual(filter: (K1, K2, AsyncCacheData<V>) -> Boolean): Int {
        return synchronized(dualCache) {
            dualCache.removeIf { k1, k2, v ->
                if (filter(k1, k2, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun getDualEntryWithoutGenerator(key1: K1, key2: K2, delta: Long = 1L): AsyncCacheData<V>? {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry
    }

    /**
     * returns whether a value is present
     * */
    fun hasDualEntry(key1: K1, key2: K2, delta: Long = 1L): Boolean {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue
    }

    fun setValue(key0: K1, key1: K2, newValue: V, timeoutMillis: Long) {
        checkKey(key0)
        checkKey(key1)
        val oldValue = synchronized(dualCache) {
            val entry = AsyncCacheData<V>()
            entry.update(timeoutMillis)
            entry.value = newValue
            dualCache.put(key0, key1, entry)
        }
        oldValue?.destroy()
    }

    fun <K1S : K1, K2S : K2> getDualEntry(
        key1: K1S, key2: K2S, timeoutMillis: Long,
        generator: (key1: K1S, key2: K2S, dst: AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> {

        checkKey(key1)
        checkKey(key2)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        var isGenerating = false
        val entry = synchronized(dualCache) {
            var entry = dualCache[key1, key2]
            isGenerating = entry == null || entry.hasBeenDestroyed
            if (isGenerating) {
                entry = AsyncCacheData()
                dualCache[key1, key2] = entry
            }
            entry!!
        }

        entry.update(timeoutMillis)

        if (isGenerating) {
            runAsync(getTaskName(name, key1, key2)) {
                generateDualSafely(key1, key2, entry, generator)
            }
        }

        return entry
    }

    fun update() {
        synchronized(dualCache) {
            dualCache.removeIf { _, _, value ->
                if (value.hasExpired) {
                    value.destroy()
                    true
                } else false
            }
        }
    }

    init {
        caches += this
        registerCache(::update, ::clear)
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(DualCacheSection::class)

        @JvmStatic
        @InternalAPI
        val caches = ConcurrentSkipListSet<DualCacheSection<*, *, *>>()

        fun <K1, K2, V : Any> generateDualSafely(
            key1: K1, key2: K2, entry: AsyncCacheData<V>,
            generator: (K1, K2, AsyncCacheData<V>) -> Unit
        ) {
            try {
                generator(key1, key2, entry)
            } catch (_: IgnoredException) {
            } catch (e: FileNotFoundException) {
                warnFileMissing(e)
            } catch (e: Exception) {
                LOGGER.warn(e)
            }
        }

        fun getTaskName(name: String, key1: Any?, key2: Any?): String {
            return if (Build.isDebug) "$name<${getKeyName(key1)},${getKeyName(key2)}>"
            else name
        }


        private fun warnFileMissing(e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: {}", e.message)
        }
    }
}