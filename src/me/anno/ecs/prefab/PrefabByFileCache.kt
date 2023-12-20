package me.anno.ecs.prefab

import me.anno.cache.LRUCache
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache.getPrefabInstance
import me.anno.engine.ECSRegistry
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

/**
 * base class for caches of types saved in Prefabs like Entities, Components, Materials, Skeletons, Animations and such
 * */
abstract class PrefabByFileCache<V : ISaveable>(val clazz: KClass<V>) {

    companion object {
        private val LOGGER = LogManager.getLogger(PrefabByFileCache::class)
        fun ensureClasses() {
            if ("Entity" !in ISaveable.objectTypeRegistry) {
                LOGGER.warn("Please call ECSRegistry.init() yourself!")
                ECSRegistry.initMeshes()
            }
        }
        fun ensureMeshClasses() {
            if ("Entity" !in ISaveable.objectTypeRegistry) {
                ECSRegistry.initMeshes()
            }
        }
    }

    operator fun get(ref: FileReference?) = get(ref, false)
    operator fun get(ref: FileReference?, default: V) = get(ref, false) ?: default

    val lru = LRUCache<FileReference, V>(16)

    fun update() {
        lru.clear()
    }

    open operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val i0 = lru[ref]
        @Suppress("unchecked_cast")
        if (i0 !== Unit) return i0 as? V
        ensureClasses()
        val instance = getPrefabInstance(ref, maxPrefabDepth, async)
        val value = if (instance != null) {
            @Suppress("unchecked_cast")
            if (clazz.isInstance(instance)) {
                instance as V
            } else {
                LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
                null
            }
        } else null
        lru[ref] = value
        return value
    }
}