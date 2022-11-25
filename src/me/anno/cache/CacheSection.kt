package me.anno.cache

import me.anno.Build
import me.anno.Engine.gameTime
import me.anno.cache.instances.LastModifiedCache
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.maps.Maps
import me.anno.utils.structures.maps.Maps.removeIf2
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

open class CacheSection(val name: String) : Comparable<CacheSection> {

    val cache = HashMap<Any, CacheEntry>(512)
    val dualCache = KeyPairMap<Any, Any, CacheEntry>(512)

    override fun compareTo(other: CacheSection): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        GFX.checkIsGFXThread()
        synchronized(cache) {
            for (it in cache.values) it.destroy()
            cache.clear()
        }
        synchronized(dualCache) {
            dualCache.forEach { _, _, v -> v.destroy() }
            dualCache.clear()
        }
    }

    inline fun remove(crossinline filter: (Any, CacheEntry) -> Boolean): Int {
        synchronized(cache) {
            return cache.removeIf2 { k, v ->
                if (filter(k, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    inline fun removeDual(crossinline filter: (Any, Any, CacheEntry) -> Boolean): Int {
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
            cache.remove(key)?.destroy()
        }
    }

    fun removeDualEntry(key1: Any, key2: Any) {
        synchronized(dualCache) {
            dualCache.removeIf { k1, k2, v ->
                if (k1 == key1 && k2 == key2) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeFileEntry(file: FileReference) = removeDualEntry(file, file.lastModified)

    fun getFileEntry(
        file: FileReference,
        allowDirectories: Boolean,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: (FileReference, Long) -> ICacheData?
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
        generator: (FileReference, V) -> ICacheData?
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
        generator: (Triple<String, String, Int>) -> ICacheData?
    ) = getEntry(Triple(major, minor, sub), timeout, asyncGenerator, generator)

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun getEntryWithoutGenerator(key: Any, delta: Long = 1L): ICacheData? {
        val entry = synchronized(cache) { cache[key] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry.data
    }

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun getDualEntryWithoutGenerator(key1: Any, key2: Any, delta: Long = 1L): ICacheData? {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry.data
    }

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun hasEntry(key: Any, delta: Long = 1L): Boolean {
        val entry = synchronized(cache) { cache[key] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue
    }

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun hasDualEntry(key1: Any, key2: Any, delta: Long = 1L): Boolean {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue
    }

    /**
     * get the value, no matter whether it actually exists
     * useful for LODs, if others work as well, just are not as good
     * */
    fun hasFileEntry(key: FileReference, delta: Long = 1L) =
        hasDualEntry(key, key.lastModified, delta)

    fun override(key: Any, data: ICacheData?, timeoutMillis: Long) {
        checkKey(key)
        val oldValue = synchronized(cache) {
            val entry = CacheEntry(timeoutMillis)
            entry.data = data
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    private fun <V> generateSafely(key: V, generator: (V) -> ICacheData?): Any? {
        var data: ICacheData? = null
        try {
            data = generator(key)
        } catch (_: ShutdownException) { // shutting down anyway
        } catch (e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: {}", e.message)
        } catch (e: Exception) {
            return e
        }
        return data
    }

    private fun <V, W> generateSafely(key0: V, key1: W, generator: (V, W) -> ICacheData?): Any? {
        var data: ICacheData? = null
        try {
            data = generator(key0, key1)
        } catch (_: ShutdownException) { // shutting down anyway
        } catch (e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: {}", e.message)
        } catch (e: ShutdownException) {
            throw e
        } catch (e: Exception) {
            return e
        }
        return data
    }

    private fun checkKey(key: Any) {
        if (Build.isDebug) {
            if (key != key) throw IllegalStateException("${key::class.qualifiedName}.equals() is incorrect!")
            if (key.hashCode() != key.hashCode()) throw IllegalStateException("${key::class.qualifiedName}.hashCode() is inconsistent!")
        }// else we assume that it's fine
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
        timeoutMillis: Long,
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
            val entry = dualCache.getOrPut(key0, key1) { _, _ -> CacheEntry(timeoutMillis) }
            if (entry.hasBeenDestroyed) entry.reset(timeoutMillis)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.update(timeoutMillis)

        if (needsGenerator) {
            entry.hasGenerator = true
            if (asyncGenerator) {
                thread(name = "$name<$key0,$key1>") {
                    val value = generateSafely(key0, key1, generator)
                    entry.data = value as? ICacheData
                    if (value is Exception) throw value
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key0,$key1> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else {
                val value = generateSafely(key0, key1, generator)
                entry.data = value as? ICacheData
                if (value is Exception) throw value
            }
        } else ifNotGenerating?.invoke()

        waitMaybe(asyncGenerator, entry, key0, key1)
        return if (entry.hasBeenDestroyed) null else entry.data

    }

    fun <V> getEntryWithCallback(
        key: V, timeoutMillis: Long,
        asyncGenerator: Boolean,
        generator: (V) -> ICacheData?,
        ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")

        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = synchronized(cache) {
            val entry = cache.getOrPut(key) { CacheEntry(timeoutMillis) }
            if (entry.hasBeenDestroyed) entry.reset(timeoutMillis)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.update(timeoutMillis)

        if (needsGenerator) {
            entry.hasGenerator = true
            if (asyncGenerator) {
                thread(name = "$name<$key>") {
                    val value = generateSafely(key, generator)
                    if (value is Exception && value !is ShutdownException) throw value
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

        waitMaybe(asyncGenerator, entry, key, null)
        return if (entry.hasBeenDestroyed) null else entry.data
    }

    private fun waitMaybe(async: Boolean, entry: CacheEntry, key0: Any, key1: Any?) {
        if (!async && entry.generatorThread != Thread.currentThread()) {
            // else no need/sense in waiting
            if (entry.waitForValue2()) {
                val key = if (key1 == null) key0 else key0 to key1
                onLongWaitStart(key, entry)
                entry.waitForValue(key)
                onLongWaitEnd(key, entry)
            }
        }
    }

    private fun onLongWaitStart(key: Any, entry: CacheEntry) {
        val msg = "Waiting for $name[$key] by ${entry.generatorThread.name} " +
                "from ${Thread.currentThread().name}"
        if (Thread.currentThread() == GFX.glThread) println(msg) // extra warning
        LOGGER.warn(msg)
    }

    private fun onLongWaitEnd(key: Any, entry: CacheEntry) {
        val msg = "Finished waiting for $name[$key] by ${entry.generatorThread.name} " +
                "from ${Thread.currentThread().name}"
        if (Thread.currentThread() == GFX.glThread) println(msg) // extra warning
        LOGGER.warn(msg)
    }

    fun <V> getEntryWithCallback(
        key: V, timeoutMillis: Long,
        queue: ProcessingQueue?,
        generator: (V) -> ICacheData?,
        ifNotGenerating: (() -> Unit)?
    ): ICacheData? {

        if (key == null) throw IllegalStateException("Key must not be null")
        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        val entry = synchronized(cache) {
            val entry = cache.getOrPut(key) { CacheEntry(timeoutMillis) }
            if (entry.hasBeenDestroyed) entry.reset(timeoutMillis)
            entry
        }

        val needsGenerator = entry.needsGenerator
        entry.update(timeoutMillis)

        val async = queue != null
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

        if (!async && entry.generatorThread != Thread.currentThread()) entry.waitForValue(key)
        return if (entry.hasBeenDestroyed) null else entry.data

    }

    fun <V> getEntry(key: V, timeoutMillis: Long, asyncGenerator: Boolean, generator: (V) -> ICacheData?) =
        getEntryWithCallback(key, timeoutMillis, asyncGenerator, generator, null)

    fun <V, W> getEntry(
        key0: V, key1: W, timeoutMillis: Long, asyncGenerator: Boolean,
        generator: (V, W) -> ICacheData?
    ) = getEntryWithCallback(key0, key1, timeoutMillis, asyncGenerator, generator, null)

    fun <V> getEntry(key: V, timeoutMillis: Long, queue: ProcessingQueue?, generator: (V) -> ICacheData?) =
        getEntryWithCallback(key, timeoutMillis, queue, generator, null)

    fun update() {
        synchronized(cache) {
            // avoiding allocations for clean memory debugging XD
            cache.removeIf2(remover)
        }
        synchronized(dualCache) {
            dualCache.removeIf { _, _, v ->
                if (gameTime > v.timeoutNanoTime) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    init {
        caches += this
    }

    companion object {

        @JvmStatic
        private val remover = object : Maps.Remover<Any, CacheEntry>() {
            override fun filter(key: Any, value: CacheEntry): Boolean {
                return if (gameTime > value.timeoutNanoTime) {
                    value.destroy()
                    true
                } else false
            }
        }

        @JvmStatic
        private val caches = ConcurrentSkipListSet<CacheSection>()

        @JvmStatic
        fun updateAll() {
            for (cache in caches) cache.update()
            LastModifiedCache.update()
            MeshCache.update()
            AnimationCache.update()
            MaterialCache.update()
            SkeletonCache.update()
        }

        @JvmStatic
        fun clearAll() {
            for (cache in caches) cache.clear()
            LastModifiedCache.clear()
        }

        @JvmStatic
        private val LOGGER = LogManager.getLogger(CacheSection::class)

    }

}