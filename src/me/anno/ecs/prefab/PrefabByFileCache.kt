package me.anno.ecs.prefab

import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.cache.LRUCache
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache.getPrefabInstance
import me.anno.ecs.prefab.PrefabCache.getPrefabInstanceAsync
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.io.saveable.Saveable
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * base class for caches of types saved in Prefabs like Entities, Components, Materials, Skeletons, Animations and such
 * */
abstract class PrefabByFileCache<V : ICacheData>(val clazz: KClass<V>, name: String) : CacheSection(name) {

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

    val lru = LRUCache<FileReference, V>(16).register()
    var timeoutMillis = 10_000L
    var allowDirectories = false

    operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val i0 = lru[ref]
        if (i0 !== Unit) {
            return clazz.safeCast(i0)
        }
        ensureClasses()
        if (ref is InnerTmpPrefabFile) { // avoid the CacheSection, so our values don't get destroyed
            val safeCast = clazz.safeCast(ref.prefab._sampleInstance)
            if (safeCast != null) return safeCast
        }
        val instance = getPrefabInstance(ref, maxPrefabDepth, async)
        val value = if (instance != null) {
            getFileEntry(ref, allowDirectories, timeoutMillis, async) { ref1, _ ->
                castInstance(instance, ref1) // may be heavy -> must be cached
            }
        } else null
        if (value != null || !async) lru[ref] = value
        return value
    }

    fun getAsync(ref: FileReference?, callback: Callback<V?>) {
        if (ref == null || ref == InvalidRef) {
            callback.ok(null)
            return
        }
        val i0 = lru[ref]
        if (i0 !== Unit) {
            callback.ok(clazz.safeCast(i0))
            return
        }
        ensureClasses()
        getPrefabInstanceAsync(ref, maxPrefabDepth) { instance, err0 ->
            if (instance != null) {
                getFileEntryAsync(ref, allowDirectories, timeoutMillis, true, { ref1, _ ->
                    castInstance(instance, ref1) // may be heavy -> must be cached
                }, { value, err1 ->
                    if (value != null) {
                        lru[ref] = value
                        callback.ok(value)
                    } else callback.err(err1)
                })
            } else callback.err(err0)
        }
    }

    open fun castInstance(instance: Saveable?, ref: FileReference): V? {
        val value = clazz.safeCast(instance)
        if (instance != null && value == null) {
            LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
        }
        return value
    }
}