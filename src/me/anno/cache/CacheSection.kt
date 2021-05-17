package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.io.FileReference
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.ShutdownException
import me.anno.utils.Threads.threadWithName
import me.anno.utils.hpc.ProcessingQueue
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max

open class CacheSection(val name: String) : Comparable<CacheSection> {

    val cache = HashMap<Any, CacheEntry>(512)

    override fun compareTo(other: CacheSection): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        GFX.checkIsGFXThread()
        synchronized(cache) {
            cache.values.forEach { it.destroy() }
            cache.clear()
        }
    }

    fun remove(filter: (Map.Entry<Any, CacheEntry>) -> Boolean) {
        synchronized(cache) {
            val toRemove = cache.filter(filter)
            cache.remove(toRemove)
            toRemove.values.forEach { it.destroy() }
        }
    }

    fun <V> getEntry(
        file: FileReference,
        allowDirectories: Boolean,
        key: V,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (Pair<FileReference, V>) -> ICacheData
    ): ICacheData? {
        val meta = LastModifiedCache[file]
        if (!meta.exists || (!allowDirectories && meta.isDirectory)) return null
        return getEntry(file to key, timeout, asyncGenerator, generator)
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
            val entry = CacheEntry(timeout, gameTime)
            entry.data = data
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    private fun <V> generateSavely(key: V, generator: (V) -> ICacheData?): ICacheData? {
        var data: ICacheData? = null
        try {
            data = generator(key)
        } catch (e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: ${e.message}")
        } catch (e: Exception) {
            if (e is ShutdownException) throw e
            else e.printStackTrace()
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
                val value = generateSavely(key, generator)
                limiter.decrementAndGet()
                value
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
                val value = generateSavely(key, generator)
                limiter.decrementAndGet()
                value
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            getEntryWithoutGenerator(key)
        }
    }

    fun <V> getEntryWithCallback(
        key: V, timeout: Long, asyncGenerator: Boolean, generator: (V) -> ICacheData?, ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")

        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        var needsGenerator = false
        val entry = synchronized(cache) {
            cache.getOrPut(key) {
                needsGenerator = true
                CacheEntry(timeout, gameTime)
            }
        }

        entry.lastUsed = gameTime

        if (needsGenerator) {
            if (asyncGenerator) {
                threadWithName("$name<$key>") {
                    entry.data = generateSavely(key, generator)
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else entry.data = generateSavely(key, generator)
        } else ifNotGenerating?.invoke()

        if (!asyncGenerator) entry.waitForValue()
        return if (entry.hasBeenDestroyed) {
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
        var needsGenerator = false
        val entry = synchronized(cache) {
            cache.getOrPut(key) {
                needsGenerator = true
                CacheEntry(timeout, gameTime)
            }
        }

        entry.lastUsed = gameTime

        if (needsGenerator) {
            if (queue != null) {
                queue += {
                    entry.data = generateSavely(key, generator)
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else entry.data = generateSavely(key, generator)
        }else ifNotGenerating?.invoke()

        if (queue == null) entry.waitForValue()
        return if (entry.hasBeenDestroyed) {
            getEntryWithCallback(key, timeout, queue, generator, null)
        } else entry.data

    }

    fun <V> getEntry(key: V, timeout: Long, asyncGenerator: Boolean, generator: (V) -> ICacheData?): ICacheData? {
        return getEntryWithCallback(key, timeout, asyncGenerator, generator, null)
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
        thread { caches += this }
    }

    companion object {

        private val caches = ConcurrentSkipListSet<CacheSection>()

        fun updateAll() {
            caches.forEach {
                it.update()
            }
        }

        fun clearAll() {
            caches.forEach {
                it.clear()
            }
            root.listOfAll.forEach { it.clearCache() }
            LastModifiedCache.clear()
        }

        private val LOGGER = LogManager.getLogger(CacheSection::class)
    }

}