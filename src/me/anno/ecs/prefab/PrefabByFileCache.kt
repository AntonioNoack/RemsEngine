package me.anno.ecs.prefab

import me.anno.ecs.prefab.PrefabCache.getPrefabPair
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

    fun getPrefab(ref: FileReference?, chain: MutableSet<FileReference>?, async: Boolean): Prefab? {
        if (ref == null || ref == InvalidRef) return null
        return getPrefabPair(ref, chain, async)?.prefab
    }

    open operator fun get(ref: FileReference?, async: Boolean): V? {
        if (ref == null || ref == InvalidRef) return null
        val pair = getPrefabPair(ref, HashSet(), async)
        val instance = pair?.instance ?: return null
        @Suppress("unchecked_cast")
        return if (clazz.isInstance(instance)) instance as V else {
            LOGGER.warn("Requested $ref as $clazz, but only found ${instance.className}")
            null
        }
    }

}