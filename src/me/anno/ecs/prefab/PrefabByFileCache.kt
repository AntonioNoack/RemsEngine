package me.anno.ecs.prefab

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.ICacheData
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.io.saveable.Saveable
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Base class for caches of types saved in Prefabs like Entities, Components, Materials, Skeletons, Animations and such.
 * If a resource is of another type, conversion/"casting" can be applied. Casting can be expensive, so it is cached.
 * */
abstract class PrefabByFileCache<V : ICacheData>(val clazz: KClass<V>, name: String) :
    CacheSection<FileKey, V>(name) {

    companion object {
        private val LOGGER = LogManager.getLogger(PrefabByFileCache::class)
        fun ensureClasses() {
            if ("Entity" !in Saveable.objectTypeRegistry) {
                LOGGER.warn("Please call ECSRegistry.init() yourself!")
                ECSRegistry.init()
            }
        }
    }

    var timeoutMillis = 10_000L
    var allowDirectories = false

    /**
     * Quick-Path with LRU-Cache
     * */
    operator fun get(ref: FileReference?): V? {
        if (ref == null || ref == InvalidRef) return null
        // avoid the CacheSection, so our values don't get destroyed
        if (ref is InnerTmpPrefabFile) {
            val safeCast = clazz.safeCast(ref.prefab._sampleInstance)
            if (safeCast != null) return safeCast
        }
        return getFileEntry(ref, allowDirectories, timeoutMillis, generator).value
    }

    fun getEntry(ref: FileReference?): AsyncCacheData<V> {
        if (ref == null || ref == InvalidRef) return AsyncCacheData.empty()
        if (ref is InnerTmpPrefabFile) {
            // avoid the CacheSection, so our values don't get destroyed
            val safeCast = clazz.safeCast(ref.prefab.getSampleInstance())
            if (safeCast != null) return AsyncCacheData(safeCast)
        }
        return getFileEntry(ref, allowDirectories, timeoutMillis, generator)
    }

    // cached to avoid dynamic allocations
    private val generator = { key: FileKey, result: AsyncCacheData<V> ->
        ensureClasses()
        PrefabCache[key.file, maxPrefabDepth].waitFor { pair ->
            result.value = castInstance(pair?.sample, key.file)
        }
    }

    open fun castInstance(instance: Saveable?, ref: FileReference): V? {
        val value = clazz.safeCast(instance)
        if (instance != null && value == null) {
            val message = "Requested $ref as $clazz, but only found ${instance.className}"
            LOGGER.warn(message)
            RuntimeException(message).printStackTrace()
        }
        return value
    }
}