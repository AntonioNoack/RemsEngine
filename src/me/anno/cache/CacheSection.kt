package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.cache.instances.LastModifiedCache
import me.anno.gpu.GFX.gameTime
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.sleepShortly
import me.anno.utils.Threads.threadWithName
import me.anno.io.FileReference
import me.anno.utils.hpc.ProcessingQueue
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.concurrent.thread
import kotlin.math.max

open class CacheSection(val name: String) : Comparable<CacheSection> {

    val cache = HashMap<Any, CacheEntry>(512)
    private val lockedKeys = HashSet<Any>(16)
    private val lockedBy = HashMap<Any, String>(16)

    override fun compareTo(other: CacheSection): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        synchronized(cache) {
            cache.values.forEach { it.destroy() }
            cache.clear()
            lockedKeys.clear() // mmh...
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
        synchronized(cache) {
            return cache[key]?.run {
                lastUsed = gameTime
                data
            }
        }
    }

    fun free(key: Any) {
        lock(key, false)
        val entry = synchronized(cache) {
            cache.remove(key)
        }
        entry?.destroy()
        unlock(key)
    }

    private fun lock(key: Any, asyncGenerator: Boolean): Unit? {
        if (asyncGenerator) {
            // sleeping on the OpenGL-Thread isn't healthy, because we pose tons of requests
            /*for (i in 0 until 10) {
                synchronized(lockedKeys) {
                    if (lockedKeys.add(key)) {
                        lockedBy[key] = Thread.currentThread().name
                        return Unit
                    } // else: somebody else is using the cache ;p
                }
                sleepShortly(true)
            }*/
            /*synchronized(lockedBy) {
                LOGGER.info("$name:$key is locked by ${lockedBy[key]}, wanted by ${Thread.currentThread().name}")
            }*/
            synchronized(lockedKeys) {
                if (lockedKeys.add(key)) {
                    lockedBy[key] = Thread.currentThread().name
                    return Unit
                } // else: somebody else is using the cache ;p
            }
            return null
        } else {
            while (true) {
                synchronized(lockedKeys) {
                    if (lockedKeys.add(key)) {
                        lockedBy[key] = Thread.currentThread().name
                        return Unit
                    }
                }
                sleepShortly(true)
            }
        }
    }

    private fun unlock(key: Any) {
        synchronized(lockedKeys) { lockedKeys.remove(key) }
    }

    private fun put(key: Any, data: ICacheData?, timeout: Long) {
        synchronized(cache) { cache[key] = CacheEntry(data, timeout, gameTime) }
    }

    fun override(key: Any, data: ICacheData?, timeout: Long) {
        synchronized(cache) {
            val oldValue = cache.put(key, CacheEntry(data, timeout, gameTime))
            oldValue?.destroy()
        }
    }

    private fun <V> generate(key: V, generator: (V) -> ICacheData): ICacheData? {
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

    private fun getDirectly(key: Any): Any? {
        val cached: CacheEntry?
        synchronized(cache) { cached = cache[key] }
        if (cached != null) {
            cached.lastUsed = gameTime
            unlock(key)
            return cached.data
        }
        return Unit
    }

    fun <V> getEntry(key: V, timeout: Long, asyncGenerator: Boolean, generator: (V) -> ICacheData): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")

        // new, async cache
        // only the key needs to be locked, not the whole cache

        lock(key, asyncGenerator) ?: return null

        val cached = getDirectly(key)
        if (cached != Unit) return cached as ICacheData?

        return if (asyncGenerator) {
            threadWithName("$name<$key>") {
                val data = generate(key, generator)
                put(key, data, timeout)
                unlock(key)
            }
            null
        } else {
            val data = generate(key, generator)
            put(key, data, timeout)
            unlock(key)
            data
        }

    }

    fun <V> getEntry(key: V, timeout: Long, queue: ProcessingQueue?, generator: (V) -> ICacheData): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")

        // new, async cache
        // only the key needs to be locked, not the whole cache

        lock(key, queue != null) ?: return null

        val cached = getDirectly(key)
        if (cached != Unit) return cached as ICacheData?

        return if (queue != null) {
            queue += {
                val data = generate(key, generator)
                put(key, data, timeout)
                unlock(key)
            }
            null
        } else {
            val data = generate(key, generator)
            put(key, data, timeout)
            unlock(key)
            data
        }

    }

    fun update() {
        val minTimeout = 300L
        val time = gameTime
        synchronized(cache) {
            val toRemove =
                cache.filter { (_, entry) -> time - entry.lastUsed > max(entry.timeout, minTimeout) * 1_000_000 }
            toRemove.forEach {
                cache.remove(it.key)
                it.value.destroy()
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