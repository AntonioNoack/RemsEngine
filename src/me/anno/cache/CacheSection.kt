package me.anno.cache

import me.anno.Build
import me.anno.Time.nanoTime
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertFail
import me.anno.utils.async.Callback
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.maps.KeyPairMap
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

open class CacheSection(val name: String) : Comparable<CacheSection> {

    val cache = HashMap<Any?, CacheEntry>(512)
    val dualCache = KeyPairMap<Any?, Any?, CacheEntry>(512)

    override fun compareTo(other: CacheSection): Int {
        return name.compareTo(other.name)
    }

    fun clear() {
        LOGGER.warn("Clearing cache {}", name)
        synchronized(cache) {
            for (it in cache.values) it.destroy()
            cache.clear()
        }
        synchronized(dualCache) {
            dualCache.forEach { _, _, v -> v.destroy() }
            dualCache.clear()
        }
    }

    private fun runAsync(name: String, runnable: () -> Unit) {
        LOGGER.debug("Started {}", name)
        thread(name = name, block = runnable)
    }

    private fun waitAsync(name: String, runnable: () -> Unit) {
        thread(name = name, block = runnable)
    }

    fun remove(filter: (Any?, CacheEntry) -> Boolean): Int {
        return synchronized(cache) {
            cache.removeIf { (k, v) ->
                if (filter(k, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeDual(filter: (Any?, Any?, CacheEntry) -> Boolean): Int {
        return synchronized(dualCache) {
            dualCache.removeIf { k1, k2, v ->
                if (filter(k1, k2, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeEntry(key: Any): ICacheData? {
        return synchronized(cache) {
            val v = cache.remove(key)
            v?.destroy()
            v?.data
        }
    }

    fun removeEntry(key1: Any, key2: Any) {
        synchronized(dualCache) {
            dualCache.removeIf { k1, k2, v ->
                if (k1 == key1 && k2 == key2) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeFileEntry(file: FileReference) = removeEntry(file, file.lastModified)

    fun <R : ICacheData> getFileEntry(
        file: FileReference, allowDirectories: Boolean,
        timeout: Long, asyncGenerator: Boolean,
        generator: (FileReference, Long) -> R?
    ): R? {
        val validFile = getValidFile(file, allowDirectories) ?: return null
        return getDualEntry(validFile, validFile.lastModified, timeout, asyncGenerator, generator)
    }

    fun <R : ICacheData> getFileEntryAsync(
        file: FileReference, allowDirectories: Boolean,
        timeout: Long, asyncGenerator: Boolean,
        generator: (FileReference, Long) -> R?,
        callback: Callback<R>
    ) {
        val validFile = getValidFile(file, allowDirectories) ?: return callback.err(null)
        getDualEntryAsync(validFile, validFile.lastModified, timeout, asyncGenerator, generator, callback)
    }

    fun getValidFile(file: FileReference, allowDirectories: Boolean): FileReference? {
        return when {
            !allowDirectories && file is InnerFolder -> {
                val alias = file.alias ?: return null
                getValidFile(alias, false)
            }
            file == InvalidRef -> null
            !file.exists -> {
                LOGGER.warn("[$name] Skipped loading $file, is missing")
                null
            }
            !allowDirectories && file.isDirectory -> {
                LOGGER.warn("[$name] Skipped loading $file, is a folder")
                null
            }
            else -> file
        }
    }

    fun <V> getEntry(
        file: FileReference, allowDirectories: Boolean,
        key: V, timeout: Long, asyncGenerator: Boolean,
        generator: (FileReference, V) -> ICacheData?
    ): ICacheData? {
        if (!file.exists || (!allowDirectories && file.isDirectory)) return null
        return getDualEntry(file, key, timeout, asyncGenerator, generator)
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun getEntryWithoutGenerator(key: Any?, delta: Long = 1L): ICacheData? {
        val entry = synchronized(cache) { cache[key] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry.data
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun getDualEntryWithoutGenerator(key1: Any, key2: Any, delta: Long = 1L): ICacheData? {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry.data
    }

    /**
     * returns whether a value is present
     * */
    fun hasEntry(key: Any, delta: Long = 1L): Boolean {
        val entry = synchronized(cache) { cache[key] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue()
    }

    /**
     * returns whether a value is present
     * */
    fun hasDualEntry(key1: Any, key2: Any, delta: Long = 1L): Boolean {
        val entry = synchronized(dualCache) { dualCache[key1, key2] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue()
    }

    /**
     * returns whether a value is present
     * */
    fun hasFileEntry(key: FileReference, delta: Long = 1L): Boolean =
        hasDualEntry(key, key.lastModified, delta)

    fun override(key: Any?, newValue: ICacheData?, timeoutMillis: Long) {
        checkKey(key)
        val oldValue = synchronized(cache) {
            val entry = CacheEntry(timeoutMillis)
            entry.data = newValue
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    fun overrideDual(key0: Any?, key1: Any?, newValue: ICacheData?, timeoutMillis: Long) {
        checkKey(key0)
        checkKey(key1)
        val oldValue = synchronized(dualCache) {
            val entry = CacheEntry(timeoutMillis)
            entry.data = newValue
            val oldValue = dualCache[key0, key1]
            dualCache[key0, key1] = entry
            oldValue
        }
        oldValue?.destroy()
    }

    private fun <K, R> generateSafely(key: K, generator: (K) -> R): R? {
        try {
            return generator(key)
        } catch (_: IgnoredException) {
        } catch (e: FileNotFoundException) {
            warnFileMissing(e)
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
        return null
    }

    private fun <V, W, R : ICacheData> generateDualSafely(key0: V, key1: W, generator: (V, W) -> R?): R? {
        var data: R? = null
        try {
            data = generator(key0, key1)
        } catch (_: IgnoredException) {
        } catch (e: FileNotFoundException) {
            warnFileMissing(e)
        } catch (e: Exception) {
            LOGGER.warn(e)
        }
        return data
    }

    private fun warnFileMissing(e: FileNotFoundException) {
        LOGGER.warn("FileNotFoundException: {}", e.message)
    }

    private fun checkKey(key: Any?) {
        if (Build.isDebug && key != null) {
            @Suppress("KotlinConstantConditions")
            if (key != key) assertFail("${key::class.simpleName}.equals() is incorrect!")
            if (key.hashCode() != key.hashCode()) assertFail("${key::class.simpleName}.hashCode() is inconsistent!")
        }// else we assume that it's fine
    }

    private val limiter = AtomicInteger()
    fun <V, R : ICacheData> getEntryLimited(
        key: V, timeout: Long, asyncGenerator: Boolean,
        limit: Int, generator: (V) -> R
    ): R? {
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithIfNotGeneratingCallback(key, timeout, asyncGenerator, {
                val value = generateSafely(key, generator)
                limiter.decrementAndGet()
                value
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            @Suppress("UNCHECKED_CAST")
            getEntryWithoutGenerator(key, 1L) as? R
        }
    }

    fun <V, R : ICacheData> getEntryLimited(
        key: V, timeout: Long, queue: ProcessingQueue?,
        limit: Int, generator: (V) -> R
    ): R? {
        val accessTry = limiter.getAndIncrement()
        return if (accessTry < limit) {
            // we're good to go, and can make our request
            getEntryWithIfNotGeneratingCallback(key, timeout, queue, {
                val value = generateSafely(key, generator)
                limiter.decrementAndGet()
                value
            }) { limiter.decrementAndGet() }
        } else {
            limiter.decrementAndGet()
            // get the value without generator
            @Suppress("UNCHECKED_CAST")
            getEntryWithoutGenerator(key, 1L) as? R
        }
    }

    fun <V, W, R : ICacheData> getDualEntryWithIfNotGeneratingCallback(
        key0: V, key1: W,
        timeoutMillis: Long, asyncGenerator: Boolean,
        generator: (key0: V, key1: W) -> R?,
        ifNotGenerating: (() -> Unit)?
    ): R? {

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
                val name = "$name<$key0,$key1>"
                runAsync(name) {
                    try {
                        entry.data = generateDualSafely(key0, key1, generator)
                        if (entry.hasBeenDestroyed) {
                            LOGGER.warn("Value for $name<$key0,$key1> was directly destroyed")
                            entry.data?.destroy()
                        }
                    } catch (_: IgnoredException) {
                    }
                    LOGGER.debug("Finished {}", name)
                }
            } else {
                entry.data = generateDualSafely(key0, key1, generator)
            }
        } else ifNotGenerating?.invoke()

        waitMaybe(asyncGenerator, entry, key0, key1)
        @Suppress("UNCHECKED_CAST")
        return if (entry.hasBeenDestroyed) null else entry.data as? R
    }

    fun <V, R : ICacheData> getEntryWithIfNotGeneratingCallback(
        key: V, timeoutMillis: Long,
        asyncGenerator: Boolean,
        generator: (V) -> R?,
        ifNotGenerating: (() -> Unit)?
    ): R? {

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
                val name = "$name<$key>"
                runAsync(name) {
                    entry.data = generateSafely(key, generator)
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for {}<{}> was directly destroyed", name, key)
                        entry.data?.destroy()
                    }
                    LOGGER.debug("Finished {}", name)
                }
            } else {
                val value = generateSafely(key, generator)
                entry.data = value as? ICacheData
            }
        } else ifNotGenerating?.invoke()

        waitMaybe(asyncGenerator, entry, key, null)
        @Suppress("UNCHECKED_CAST")
        return if (entry.hasBeenDestroyed) null else entry.data as? R
    }

    fun <V, R : ICacheData> getEntryAsync(
        key: V, timeoutMillis: Long,
        asyncGenerator: Boolean,
        generator: (V) -> R?,
        resultCallback: Callback<R>
    ) {

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
                val name = "$name<$key>"
                runAsync(name) {
                    val value = generateSafely(key, generator)
                    entry.data = value
                    LOGGER.debug("Finished {}", name)
                    entry.callback(null, resultCallback)
                }
            } else {
                val value = generateSafely(key, generator)
                entry.data = value
                entry.callback(null, resultCallback)
            }
        } else {
            if (entry.hasValue()) {
                entry.callback(null, resultCallback)
            } else {
                waitAsync("WaitingFor<$key>") {
                    entry.waitForValueOrThrow(key)
                    entry.callback(null, resultCallback)
                }
            }
        }
    }

    fun <V, W, R : ICacheData> getDualEntryAsync(
        key0: V, key1: W, timeoutMillis: Long,
        asyncGenerator: Boolean,
        generator: (V, W) -> R?,
        resultCallback: Callback<R>
    ) {

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
                val name = "$name<$key0,$key1>"
                runAsync(name) {
                    entry.data = generateDualSafely(key0, key1, generator)
                    LOGGER.debug("Finished {}", name)
                    entry.callback(null, resultCallback)
                }
            } else {
                entry.data = generateDualSafely(key0, key1, generator)
                entry.callback(null, resultCallback)
            }
        } else {
            if (entry.hasValue()) {
                entry.callback(null, resultCallback)
            } else {
                entry.waitForValueOrTimeout(resultCallback)
            }
        }
    }

    private fun waitMaybe(async: Boolean, entry: CacheEntry, key0: Any?, key1: Any?) {
        if (!async) {
            // else no need/sense in waiting
            if (entry.waitForValueReturnWhetherIncomplete()) {
                val key = if (key1 == null) key0 else key0 to key1
                onLongWaitStart(key, entry)
                entry.waitForValueOrThrow(key)
                onLongWaitEnd(key, entry)
            }
        }
    }

    private fun onLongWaitStart(key: Any?, entry: CacheEntry) {
        LOGGER.warn("Waiting for $name[$key] by ${entry.generatorThread.name} from ${Thread.currentThread().name}")
    }

    private fun onLongWaitEnd(key: Any?, entry: CacheEntry) {
        LOGGER.warn("Finished waiting for $name[$key] by ${entry.generatorThread.name} from ${Thread.currentThread().name}")
    }

    fun <V, R : ICacheData> getEntryWithIfNotGeneratingCallback(
        key: V, timeoutMillis: Long,
        queue: ProcessingQueue?,
        generator: (V) -> R?,
        ifNotGenerating: (() -> Unit)?
    ): R? {

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
                    entry.data = generateSafely(key, generator)
                    if (entry.hasBeenDestroyed) {
                        LOGGER.warn("Value for $name<$key> was directly destroyed")
                        entry.data?.destroy()
                    }
                }
            } else {
                entry.data = generateSafely(key, generator)
            }
        } else ifNotGenerating?.invoke()

        if (!async && entry.generatorThread != Thread.currentThread()) {
            entry.waitForValueOrThrow(key)
        }
        @Suppress("UNCHECKED_CAST")
        return if (entry.hasBeenDestroyed) null else entry.data as? R
    }

    fun <V, R : ICacheData> getEntry(key: V, timeoutMillis: Long, asyncGenerator: Boolean, generator: (V) -> R?): R? =
        getEntryWithIfNotGeneratingCallback(key, timeoutMillis, asyncGenerator, generator, null)

    fun <V, W, R : ICacheData> getDualEntry(
        key0: V, key1: W, timeoutMillis: Long, asyncGenerator: Boolean,
        generator: (V, W) -> R?
    ): R? = getDualEntryWithIfNotGeneratingCallback(key0, key1, timeoutMillis, asyncGenerator, generator, null)

    fun <V> getEntry(key: V, timeoutMillis: Long, queue: ProcessingQueue?, generator: (V) -> ICacheData?): ICacheData? =
        getEntryWithIfNotGeneratingCallback(key, timeoutMillis, queue, generator, null)

    fun update() {
        synchronized(cache) {
            // avoiding allocations for clean memory debugging XD
            cache.removeIf { (_, value) ->
                if (nanoTime > value.timeoutNanoTime) {
                    value.destroy()
                    true
                } else false
            }
        }
        synchronized(dualCache) {
            dualCache.removeIf { _, _, v ->
                if (nanoTime > v.timeoutNanoTime) {
                    v.destroy()
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
        @InternalAPI
        val caches = ConcurrentSkipListSet<CacheSection>()

        @JvmStatic // typically non-CacheSection caches, that still need regular updating
        private val updateListeners = ArrayList<() -> Unit>()

        @JvmStatic
        private val clearListeners = ArrayList<() -> Unit>()

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
            val filter = { file: Any?, _: Any?, _: Any? ->
                (file is FileReference && file.absolutePath.startsWith(path, true)) ||
                        (file is String && file.startsWith(path, true))
            }
            val removed = ArrayList<IndexedValue<String>>()
            for (cache in caches) {
                val numRemovedEntries = cache.removeDual(filter)
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
            Reference.invalidateListeners += CacheSection::invalidateFiles
        }

        @JvmStatic
        private val LOGGER = LogManager.getLogger(CacheSection::class)
    }
}