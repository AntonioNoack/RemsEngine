package me.anno.ecs.prefab

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.cache.ICacheData
import me.anno.cache.LRUCache
import me.anno.cache.NullCacheData
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
 * base class for caches of types saved in Prefabs like Entities, Components, Materials, Skeletons, Animations and such
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

    operator fun get(ref: FileReference?) = get(ref, false)
    operator fun get(ref: FileReference?, default: V) = get(ref, false) ?: default

    val lru = LRUCache<FileKey, V>(16).register()
    val lru1 = LRUCache<FileKey, AsyncCacheData<V>>(16).register()

    var timeoutMillis = 10_000L
    var allowDirectories = false

    operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val fileKey = ref.getFileKey()
        val i0 = lru[fileKey]
        if (i0 !== Unit) {
            return clazz.safeCast(i0)
        }
        ensureClasses()
        if (ref is InnerTmpPrefabFile) { // avoid the CacheSection, so our values don't get destroyed
            val safeCast = clazz.safeCast(ref.prefab._sampleInstance)
            if (safeCast != null) return safeCast
        }
        val instance = PrefabCache[ref, maxPrefabDepth].waitFor()?.sample
        val value = if (instance != null) {
            getFileEntry(ref, allowDirectories, timeoutMillis) { key, result ->
                result.value = castInstance(instance, key.file) // may be heavy -> must be cached
            }.waitFor(async)
        } else null
        if (value != null || !async) lru[fileKey] = value
        return value
    }

    fun get1(ref: FileReference?): AsyncCacheData<V> {
        if (ref == null || ref == InvalidRef) return NullCacheData.get()
        val fileKey = ref.getFileKey()
        val i0 = lru1[fileKey]
        if (i0 !== Unit) {
            @Suppress("UNCHECKED_CAST")
            return i0 as AsyncCacheData<V>
        }
        ensureClasses()
        if (ref is InnerTmpPrefabFile) { // avoid the CacheSection, so our values don't get destroyed
            val safeCast = clazz.safeCast(ref.prefab._sampleInstance)
            if (safeCast != null) return AsyncCacheData(safeCast)
        }
        val value = PrefabCache[ref, maxPrefabDepth].mapNext2 { pair ->
            val instance = pair.sample
            if (instance != null) {
                getFileEntry(ref, allowDirectories, timeoutMillis) { key, result ->
                    result.value = castInstance(instance, key.file) // may be heavy -> must be cached
                }
            } else NullCacheData.get()
        }
        lru1[fileKey] = value
        return value
    }

    open fun castInstance(instance: Saveable?, ref: FileReference): V? {
        val value = clazz.safeCast(instance)
        if (instance != null && value == null) {
            LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
        }
        return value
    }
}