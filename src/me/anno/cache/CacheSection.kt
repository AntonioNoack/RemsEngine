package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.io.files.FileReference
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.ShutdownException
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

open class CacheSection(val name: String) : Comparable<CacheSection> {

    val cache = HashMap<Any, CacheEntry>(512)
    val dualCache = KeyPairMap<Any, Any, CacheEntry>(512)

    override fun compareTo(other: CacheSection): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache $name")
        GFX.checkIsGFXThread()
        synchronized(cache) {
            for(it in cache.values) it.destroy()
            cache.clear()
        }
    }

    fun remove(filter: (Map.Entry<Any, CacheEntry>) -> Boolean): Int {
        synchronized(cache) {
            val toRemove = cache.filter(filter)
            cache.remove(toRemove)
            for (value in toRemove.values) {
                value.destroy()
            }
            return toRemove.values.size
        }
    }

    fun removeDual(filter: (Any, Any, CacheEntry) -> Boolean): Int {
        synchronized(dualCache) {
            return dualCache.removeIf { k1, k2, v ->
                if (filter(k1, k2, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeEntry(key: Any) {
        synchronized(cache) {
            cache.remove(cache)?.destroy()
        }
    }

    fun getFileEntry(
        file: FileReference,
        allowDirectories: Boolean,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (FileReference, Long) -> ICacheData
    ): ICacheData? {
        if (!file.exists || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file, file.lastModified, timeout, asyncGenerator, generator)
    }

    fun <V> getEntry(
        file: FileReference,
        allowDirectories: Boolean,
        key: V,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (FileReference, V) -> ICacheData
    ): ICacheData? {
        if (!file.exists || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file, key, timeout, asyncGenerator, generator)
    }

    fun getEntry(
        major: String,
        minor: String,
        sub: Int,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (Triple<String, String, Int>) -> ICacheData
    ): ICacheData? {
        return getEntry(Triple(major, minor, sub), timeout, asyncGenerator, generator)
    }

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun getEntryWithoutGenerator(key: Any): ICacheData? {
        val value = synchronized(cache) { cache[key] } ?: return null
        value.lastUsed = gameTime
        return value.data
    }

    fun free(key: Any) {
        val entry = synchronized(cache) { cache.remove(key) }
        entry?.destroy()
    }

    fun override(key: Any, data: ICacheData?, timeout: Long) {
        checkKey(key)
        val oldValue = synchronized(cache) {
            val entry = CacheEntry(timeout)
            entry.data = data
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    private fun <V> generateSafely(key: V, generator: (V) -> ICacheData?): Any? {
        var data: ICacheData? = null
        try {
            data = generator(key)
        } catch (e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: ${e.message}")
        } catch (e: Exception) {
            if (e is ShutdownException) throw e
            else return e
        }
        return data
    }

    private fun <V, W> generateSafely(key0: V, key1: W, generator: (V, W) -> ICacheData?): Any? {
        var data: ICacheData? = null
        try {
            data = generator(key0, key1)
        } catch (e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: ${e.message}")
        } catch (e: Exception) {
            if (e is ShutdownException) throw e
            else return e
        }
        return data
    }

    private fun checkKey(key: Any) {
        if (key != key) throw IllegalStateException("Key must equal itself")
        if (key.hashCode() != key.hashCode()) throw IllegalStateException("Hash-function of a key must be the same")
    }

    private val limiter = AtomicInteger()
    fun <V> getEntryLimited(
        key: V,
        timeout: Long,
        asyncGenerator: Boolean,
        limit: Int,
        generator: (V) -> ICacheData
    ): ICacheData? {
        if (key == null) return null
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithCallback(key, timeout, asyncGenerator, {
                val value = generateSafely(key, generator)
                limiter.decrementAndGet()
                if (value is Exception) throw value
                value as? ICacheData
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            getEntryWithoutGenerator(key)
        }
    }

    fun <V> getEntryLimited(
        key: V,
        timeout: Long,
        queue: ProcessingQueue?,
        limit: Int,
        generator: (V) -> ICacheData
    ): ICacheData? {
        if (key == null) return null
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithCallback(key, timeout, queue, {
                val value = generateSafely(key, generator)
                limiter.decrementAndGet()
                if (value is Exception) throw value
                value as? ICacheData
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            getEntryWithoutGenerator(key)
        }
    }

    fun <V, W> getEntryWithCallback(
        key0: V,
        key1: W,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (key0: V, key1: W) -> ICacheData?,
        ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key0 == null || key1 == null) throw IllegalStateException("Key must not be null")

        checkKey(key0)
        checkKey(key1)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = synchronized(dualCache) {
            val entry = dualCache.getOrPut(key0, key1) { _, _ -> CacheEntry(timeout) }
            if (entry.hasBeenDestroyed) entry.reset(timeout)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.lastUsed = gameTime

        if (needsGenerator) {
            entry.hasGenerator = true
            if (asyncGenerator) {
                threadWithName("$name<$key0,$key1>") {
                    val value = generateSafely(key0, key1, generator)
                    if (value is Exception) throw value
                    value as? ICacheData
                    entry.data = value as? ICacheData
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key0,$key1> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else {
                val value = generateSafely(key0, key1, generator)
                if (value is Exception) throw value
                entry.data = value as? ICacheData
            }
        } else ifNotGenerating?.invoke()

        if (!asyncGenerator) {
            LOGGER.warn("Waiting for $name[$key0, $key1] from thread ${Thread.currentThread().name}")
            entry.waitForValue(key0 to key1)
        }
        return if (entry.hasBeenDestroyed) {
            getEntryWithCallback(key0, key1, timeout, asyncGenerator, generator, null)
        } else entry.data

    }

    fun <V> getEntryWithCallback(
        key: V, timeout: Long, asyncGenerator: Boolean,
        generator: (V) -> ICacheData?,
        ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")

        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = synchronized(cache) {
            val entry = cache.getOrPut(key) { CacheEntry(timeout) }
            if (entry.hasBeenDestroyed) entry.reset(timeout)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.lastUsed = gameTime

        if (needsGenerator) {
            entry.hasGenerator = true
            if (asyncGenerator) {
                threadWithName("$name<$key>") {
                    val value = generateSafely(key, generator)
                    if (value is Exception) throw value
                    value as? ICacheData
                    entry.data = value as? ICacheData
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else {
                val value = generateSafely(key, generator)
                if (value is Exception) throw value
                entry.data = value as? ICacheData
            }
        } else ifNotGenerating?.invoke()

        if (!asyncGenerator) {
            LOGGER.warn("Waiting for $name[$key] from thread ${Thread.currentThread().name}")
            entry.waitForValue(key)
        }
        return if (entry.hasBeenDestroyed) {
            LOGGER.warn("Entry for $name[$key] has been destroyed, retrying")
            // if (maxDepth < 0) throw RuntimeException("Cache for key '$key' is broken, always is destroyed!")
            getEntryWithCallback(key, timeout, asyncGenerator, generator, null)
        } else entry.data

    }

    fun <V> getEntryWithCallback(
        key: V, timeout: Long, queue: ProcessingQueue?, generator: (V) -> ICacheData?, ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")
        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = synchronized(cache) {
            val entry = cache.getOrPut(key) { CacheEntry(timeout) }
            if (entry.hasBeenDestroyed) entry.reset(timeout)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.lastUsed = gameTime

        if (needsGenerator) {
            entry.hasGenerator = true
            if (queue != null) {
                queue += {
                    val value = generateSafely(key, generator)
                    if (value is Exception) throw value
                    entry.data = value as? ICacheData
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else {
                val value = generateSafely(key, generator)
                if (value is Exception) throw value
                entry.data = value as? ICacheData
            }
        } else ifNotGenerating?.invoke()

        if (queue == null) entry.waitForValue(key)
        return if (entry.hasBeenDestroyed) {
            getEntryWithCallback(key, timeout, queue, generator, null)
        } else entry.data

    }

    fun <V> getEntry(key: V, timeout: Long, asyncGenerator: Boolean, generator: (V) -> ICacheData?): ICacheData? {
        return getEntryWithCallback(key, timeout, asyncGenerator, generator, null)
    }

    fun <V, W> getEntry(
        key0: V,
        key1: W,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (V, W) -> ICacheData?
    ): ICacheData? {
        return getEntryWithCallback(key0, key1, timeout, asyncGenerator, generator, null)
    }

    fun <V> getEntry(key: V, timeout: Long, queue: ProcessingQueue?, generator: (V) -> ICacheData?): ICacheData? {
        return getEntryWithCallback(key, timeout, queue, generator, null)
    }

    fun update() {
        val minTimeout = 300L
        val time = gameTime
        synchronized(cache) {
            val toRemove =
                cache.filter { (_, entry) -> time - entry.lastUsed > max(entry.timeout, minTimeout) * 1_000_000 }
            for (it in toRemove) {
                val v2 = cache.remove(it.key)
                    ?: throw IllegalStateException("This cannot be null, except we have race-conditions")
                try {
                    v2.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    init {
        caches += this
    }

    companion object {

        private val caches = ConcurrentSkipListSet<CacheSection>()

        fun updateAll() {
            for (cache in caches) cache.update()
        }

        fun clearAll() {
            for (cache in caches) cache.clear()
            root.findFirstInAll { it.clearCache(); false }
            LastModifiedCache.clear()
        }

        private val LOGGER = LogManager.getLogger(CacheSection::class)

    }

}