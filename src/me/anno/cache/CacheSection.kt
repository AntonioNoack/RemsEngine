package me.anno.cache

import me.anno.Build
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.Reference
import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.utils.assertions.assertFail
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentSkipListSet

open class CacheSection<Key, Value : Any>(val name: String) : Comparable<CacheSection<*, *>> {

    val cache = HashMap<Key, AsyncCacheData<Value>>(512)

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

    @Suppress("unused")
    fun forEach(callback: (Key, AsyncCacheData<Value>) -> Unit) {
        synchronized(cache) {
            cache.forEach(callback)
        }
    }

    fun removeIf(filter: (Key, AsyncCacheData<Value>) -> Boolean): Int {
        return synchronized(cache) {
            cache.removeIf { (k, v) ->
                if (filter(k, v)) {
                    v.destroy()
                    true
                } else false
            }
        }
    }

    fun removeEntry(key: Key, delete: Boolean = true): AsyncCacheData<Value>? {
        return synchronized(cache) {
            val value = cache.remove(key)
            if (delete) value?.destroy()
            value
        }
    }

    /**
     * get the value, without generating it if it doesn't exist;
     * delta is added to its timeout, when necessary, so it stays loaded
     * */
    fun getEntryWithoutGenerator(key: Key, delta: Long = 0L): AsyncCacheData<Value>? {
        val entry = synchronized(cache) { cache[key] } ?: return null
        if (delta > 0L) entry.update(delta)
        return entry
    }

    /**
     * returns whether a value is present
     * */
    fun hasEntry(key: Key, delta: Long = 0L): Boolean {
        val entry = synchronized(cache) { cache[key] } ?: return false
        if (delta > 0L) entry.update(delta)
        return entry.hasValue
    }

    fun setValue(key: Key, newValue: Value, timeoutMillis: Long) {
        checkKey(key)
        val oldValue = synchronized(cache) {
            val entry = AsyncCacheData<Value>()
            entry.update(timeoutMillis)
            entry.value = newValue
            cache.put(key, entry)
        }
        oldValue?.destroy()
    }

    fun <KeyI : Key> getEntry(
        key: KeyI, timeoutMillis: Long,
        generator: (KeyI, AsyncCacheData<Value>) -> Unit
    ): AsyncCacheData<Value> = getEntry(key, timeoutMillis, null, generator)

    fun <KeyI : Key> getEntry(
        key: KeyI, timeoutMillis: Long,
        queue: ProcessingQueue?, generator: (KeyI, AsyncCacheData<Value>) -> Unit
    ): AsyncCacheData<Value> {
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
        }

        return entry
    }

    fun update() {
        synchronized(cache) {
            // avoiding allocations for clean memory debugging XD
            cache.removeIf { (_, value) ->
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
            CacheTime.updateTime()
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