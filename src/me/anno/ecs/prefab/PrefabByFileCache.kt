package me.anno.ecs.prefab

import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache.getPrefabInstance
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

open class PrefabByFileCache<V : ISaveable>(val clazz: KClass<V>) {

    companion object {
        private val LOGGER = LogManager.getLogger(PrefabByFileCache::class)
    }

    operator fun get(ref: FileReference?) = get(ref, false)
    operator fun get(ref: FileReference?, default: V) = get(ref, false) ?: default

    fun getPrefab(ref: FileReference?, depth: Int, async: Boolean): Prefab? {
        if (ref == null || ref == InvalidRef) return null
        return PrefabCache.getPrefab(ref, depth, async)
    }

    open operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val instance = getPrefabInstance(ref, maxPrefabDepth, async) ?: return null
        @Suppress("unchecked_cast")
        return if (clazz.isInstance(instance)) instance as V else {
            LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
            null
        }
    }

}