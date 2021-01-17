package me.anno.cache

import me.anno.cache.data.ICacheData
import me.anno.gpu.GFX.gameTime
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

open class CacheSection(val name: String): Comparable<CacheSection> {

    val cache = HashMap<Any, CacheEntry>(512)
    private val lockedKeys = HashSet<Any>(512)

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

    fun getEntry(
        file: File,
        allowDirectories: Boolean,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> ICacheData
    ): ICacheData? {
        if (!file.exists() || (!allowDirectories && file.isDirectory)) return null
        return getEntry(file to key, timeout, asyncGenerator, generator)
    }

    fun getEntry(
        major: String,
        minor: String,
        sub: Int,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> ICacheData
    ): ICacheData? {
        return getEntry(Triple(major, minor, sub), timeout, asyncGenerator, generator)
    }

    fun getEntry(key: Any, timeout: Long, asyncGenerator: Boolean, generator: () -> ICacheData): ICacheData? {

        // old, sync cache
        /*if(false){// key is FBStack.FBKey -> all textures are missing... why ever...
            synchronized(cache){
                val cached = cache[key]
                if(cached != null){
                    cached.lastUsed = GFX.gameTime
                    return cached.data
                }
                var data: CacheData? = null
                try {
                    data = generator()
                } catch (e: FileNotFoundException){
                    LOGGER.warn("FileNotFoundException: ${e.message}")
                } catch (e: Exception){
                    e.printStackTrace()
                }
                synchronized(cache){
                    cache[key] = CacheEntry(data, timeout, GFX.gameTime)
                }
                return data
            }
        }*/

        // new, async cache
        // only the key needs to be locked, not the whole cache

        if (asyncGenerator) {
            synchronized(lockedKeys) {
                if (key !in lockedKeys) {
                    lockedKeys += key
                } else {
                    return null
                } // somebody else is using the cache ;p
            }
        } else {
            var hasKey = false
            while (!hasKey) {
                synchronized(lockedKeys) {
                    if (key !in lockedKeys) {
                        lockedKeys += key
                        hasKey = true
                    }
                }
                if (hasKey) break
                Thread.sleep(0, 1000)
            }
        }

        val cached: CacheEntry?
        synchronized(cache) { cached = cache[key] }
        if (cached != null) {
            cached.lastUsed = gameTime
            synchronized(lockedKeys) { lockedKeys.remove(key) }
            return cached.data
        }

        return if (asyncGenerator) {
            thread {
                var data: ICacheData? = null
                try {
                    data = generator()
                } catch (e: FileNotFoundException) {
                    LOGGER.warn("FileNotFoundException: ${e.message}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                synchronized(cache) { cache[key] = CacheEntry(data, timeout, gameTime) }
                synchronized(lockedKeys) { lockedKeys.remove(key) }
            }
            null
        } else {
            var data: ICacheData? = null
            try {
                data = generator()
            } catch (e: FileNotFoundException) {
                LOGGER.warn("FileNotFoundException: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            synchronized(cache) { cache[key] = CacheEntry(data, timeout, gameTime) }
            synchronized(lockedKeys) { lockedKeys.remove(key) }
            data
        }

    }

    fun update() {
        val minTimeout = 300L
        val time = gameTime
        synchronized(cache) {
            val toRemove =
                cache.filter { (_, entry) -> abs(entry.lastUsed - time) > max(entry.timeout, minTimeout) * 1_000_000 }
            toRemove.forEach {
                cache.remove(it.key)
                it.value.destroy()
            }
        }
    }

    init { thread { caches += this } }

    companion object {

        private val caches = ConcurrentSkipListSet<CacheSection>()

        fun updateAll() {
            caches.forEach {
                it.update()
            }
        }

        fun clearAll(){
            caches.forEach {
                it.clear()
            }
        }

        private val LOGGER = LogManager.getLogger(CacheSection::class)
    }

}