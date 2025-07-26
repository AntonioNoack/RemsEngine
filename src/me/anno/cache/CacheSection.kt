package me.anno.cache

import me.anno.Build
import me.anno.Time.nanoTime
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.Reference
import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import me.anno.utils.assertions.assertFail
import me.anno.utils.async.Callback
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

open class CacheSection<K, V : Any>(val name: String) : Comparable<CacheSection<*, *>> {

    val cache = HashMap<K, AsyncCacheData<V>>(512)

    override fun compareTo(other: CacheSection<*, *>): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        synchronized(cache) {
            for (it in cache.values) it.destroy()
            cache.clear()
        }
    }

    fun removeIf(filter: (K, AsyncCacheData<V>) -> Boolean): Int {
        return synchronized(cache) {
            cache.removeIf { (k, v) ->
                if (filter(k, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeEntry(key: K, delete: Boolean = true): AsyncCacheData<V>? {
        return synchronized(cache) {
            val v = cache.remove(key)
            if (delete) v?.destroy()
            v
        }
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun getEntryWithoutGenerator(key: K, delta: Long = 1L): AsyncCacheData<V>? {
        val entry = synchronized(cache) { cache[key] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry
    }

    /**
     * returns whether a value is present
     * */
    fun hasEntry(key: K, delta: Long = 1L): Boolean {
        val entry = synchronized(cache) { cache[key] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue
    }

    fun override(key: K, newValue: V, timeoutMillis: Long) {
        checkKey(key)
        val oldValue = synchronized(cache) {
            val entry = AsyncCacheData<V>()
            entry.update(timeoutMillis)
            entry.value = newValue
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    private val limiter = AtomicInteger()
    fun <K1S : K> getEntryLimited(
        key: K1S, timeout: Long,
        limit: Int, generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V>? {
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithIfNotGeneratingCallback(key, timeout, { k, e ->
                generateSafely(k, e, generator)
                e.waitFor { limiter.decrementAndGet() }
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            getEntryWithoutGenerator(key, 1L)
        }
    }

    fun <K1S : K> getEntryLimitedWithRetry(
        key: K1S, timeout: Long,
        limit: Int, generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> {
        return getEntryLimited(key, timeout, limit, generator) ?: run {
            // register this as a retry-mechanism
            val result = AsyncCacheData<V>()
            result.retryCallback = {
                getEntryLimited(key, timeout, limit, generator)
                    ?.waitFor(result)
            }
            result
        }
    }

    fun <K1S : K> getEntryLimited(
        key: K1S, timeout: Long, queue: ProcessingQueue?,
        limit: Int, generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V>? {
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithIfNotGeneratingCallback(key, timeout, queue, { k, e ->
                generateSafely(k, e, generator)
                e.waitFor { limiter.decrementAndGet() }
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            getEntryWithoutGenerator(key, 1L)
        }
    }

    fun <K1S : K> getEntryLimitedWithRetry(
        key: K1S, timeout: Long, queue: ProcessingQueue?,
        limit: Int, generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> {
        return getEntryLimited(key, timeout, queue, limit, generator) ?: run {
            // register this as a retry-mechanism
            val result = AsyncCacheData<V>()
            result.retryCallback = {
                getEntryLimited(key, timeout, queue, limit, generator)
                    ?.waitFor(result)
            }
            result
        }
    }

    fun <K1S : K> getEntryWithIfNotGeneratingCallback(
        key: K1S, timeoutMillis: Long,
        generator: (K1S, AsyncCacheData<V>) -> Unit,
        ifNotGenerating: (() -> Unit)?
    ): AsyncCacheData<V> {
        return getEntryWithIfNotGeneratingCallback(key, timeoutMillis, null, generator, ifNotGenerating)
    }

    fun getEntryAsync(
        key: K, timeoutMillis: Long,
        generator: (K, AsyncCacheData<V>) -> Unit,
        resultCallback: Callback<V>
    ) {
        getEntry(key, timeoutMillis, generator)
            .waitFor(resultCallback)
    }

    fun <K1S : K> getEntryWithIfNotGeneratingCallback(
        key: K1S, timeoutMillis: Long,
        queue: ProcessingQueue?,
        generator: (K1S, AsyncCacheData<V>) -> Unit,
        ifNotGenerating: (() -> Unit)?
    ): AsyncCacheData<V> {

        checkKey(key)

        // new, async cache
        // only the key needs to be locked, not the whole cache

        var isGenerating: Boolean
        val entry = synchronized(cache) {
            var entry = cache[key]
            isGenerating = entry == null || entry.hasBeenDestroyed
            if (isGenerating) {
                entry = AsyncCacheData()
                cache[key] = entry
            }
            entry!!
        }

        entry.update(timeoutMillis)

        if (isGenerating) {
            if (queue != null) {
                queue += { generateSafely(key, entry, generator) }
            } else runAsync(getTaskName(name, key)) {
                generateSafely(key, entry, generator)
            }
        } else {
            ifNotGenerating?.invoke()
        }

        return entry
    }

    fun <K1S : K> getEntry(
        key: K1S, timeoutMillis: Long,
        generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> = getEntryWithIfNotGeneratingCallback(key, timeoutMillis, generator, null)

    fun <K1S : K> getEntry(
        key: K1S, timeoutMillis: Long,
        queue: ProcessingQueue?, generator: (K1S, AsyncCacheData<V>) -> Unit
    ): AsyncCacheData<V> = getEntryWithIfNotGeneratingCallback(key, timeoutMillis, queue, generator, null)

    fun update() {
        // todo we have a target of 60 FPS, if we're running slower than that, make Cache-decay slower, too
        //  issue: we need a separate clock for the cache... 🤔
        synchronized(cache) {
            // avoiding allocations for clean memory debugging XD
            cache.removeIf { (_, value) ->
                if (nanoTime > value.timeoutNanoTime) {
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
        private val LOGGER = LogManager.getLogger(CacheSection::class)

        @JvmStatic
        @InternalAPI
        val caches = ConcurrentSkipListSet<CacheSection<*, *>>()

        @JvmStatic // typically non-CacheSection caches, that still need regular updating
        private val updateListeners = ArrayList<() -> Unit>()

        @JvmStatic
        private val clearListeners = ArrayList<() -> Unit>()

        fun getKeyName(key: Any?): String {
            return when (key) {
                is FileReference -> key.name
                is String -> if (key.length > 16) key.substring(0, 16) else key
                // what else should we support?... should we do this at all?
                else -> hash32(key)
            }
        }

        fun getTaskName(name: String, key: Any?): String {
            return if (Build.isDebug) "$name<${getKeyName(key)}>"
            else name
        }

        @JvmStatic
        fun updateAll() {
            callListeners(updateListeners)
        }

        @JvmStatic
        fun clearAll() {
            callListeners(clearListeners)
        }

        private fun callListeners(listeners: List<() -> Unit>) {
            synchronized(listeners) {
                for (i in listeners.indices) {
                    listeners[i]()
                }
            }
        }

        private fun addListener(listeners: ArrayList<() -> Unit>, listener: () -> Unit) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        fun registerCache(update: () -> Unit, clear: () -> Unit) {
            addListener(updateListeners, update)
            addListener(clearListeners, clear)
        }

        fun invalidateFiles(path: String) {
            val filter = { key: Any?, data: Any? ->
                key is FileKey && key.file.absolutePath.startsWith(path, true)
            }
            val removed = ArrayList<IndexedValue<String>>()
            for (cache in caches) {
                val numRemovedEntries = cache.removeIf(filter)
                if (numRemovedEntries > 0) {
                    removed.add(IndexedValue(numRemovedEntries, cache.name))
                }
            }
            if (removed.isNotEmpty()) {
                LOGGER.debug(
                    "Removed [${removed.joinToString("+") { it.index.toString() }}] file entries from [" +
                            "${removed.joinToString("+") { it.value }}] caches"
                )
            }
        }

        init {
            Reference.invalidateListeners += Companion::invalidateFiles
        }

        fun checkKey(key: Any?) {
            if (Build.isDebug && key != null) {
                @Suppress("KotlinConstantConditions")
                if (key != key) assertFail("${key::class.simpleName}.equals() is incorrect!")
                if (key.hashCode() != key.hashCode()) assertFail("${key::class.simpleName}.hashCode() is inconsistent!")
            }// else we assume that it's fine
        }

        fun <K1, V : Any> generateSafely(
            key: K1, entry: AsyncCacheData<V>,
            generator: (K1, AsyncCacheData<V>) -> Unit
        ) {
            try {
                generator(key, entry)
            } catch (_: IgnoredException) {
            } catch (e: FileNotFoundException) {
                warnFileMissing(e)
            } catch (e: Exception) {
                LOGGER.warn(e)
            }
        }

        fun runAsync(name: String, runnable: () -> Unit) {
            LOGGER.debug("Started {}", name)
            runOnNonGFXThread(name, runnable)
        }

        private fun warnFileMissing(e: FileNotFoundException) {
            LOGGER.warn("FileNotFoundException: {}", e.message)
        }
    }
}