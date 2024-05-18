package me.anno.ecs.prefab

import me.anno.cache.CacheSection
import me.anno.cache.LRUCache
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache.getPrefabInstance
import me.anno.ecs.prefab.PrefabCache.getPrefabInstanceAsync
import me.anno.engine.ECSRegistry
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.Callback
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * base class for caches of types saved in Prefabs like Entities, Components, Materials, Skeletons, Animations and such
 * */
abstract class PrefabByFileCache<V : Saveable>(val clazz: KClass<V>) {

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

    val lru = LRUCache<FileReference, V>(16)

    fun update() {
        lru.clear()
    }

    operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val i0 = lru[ref]
        @Suppress("unchecked_cast")
        if (i0 !== Unit) return i0 as? V
        ensureClasses()
        val instance = getPrefabInstance(ref, maxPrefabDepth, async)
        val value = castInstance(instance, ref)
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
            @Suppress("unchecked_cast")
            callback.ok(i0 as? V)
            return
        }
        ensureClasses()
        getPrefabInstanceAsync(ref, maxPrefabDepth) { instance, err ->
            err?.printStackTrace()
            val value = castInstance(instance, ref)
            lru[ref] = value
            callback.ok(value)
        }
    }

    open fun castInstance(instance: Saveable?, ref: FileReference): V? {
        val value = clazz.safeCast(instance)
        if (instance != null && value == null) {
            LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
        }
        return value
    }

    init {
        CacheSection.registerOnUpdate(::update)
    }
}