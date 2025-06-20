package me.anno.ecs.prefab

import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.saveable.Saveable
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class PrefabPair(val reference: FileReference, var value: Saveable?) : ICacheData {

    val prefab: Prefab?
        get() {
            val value = value
            return (value as? Prefab)
                ?: (value as? PrefabSaveable)?.getOrCreatePrefab()
        }

    val sample: Saveable?
        get() {
            val value = value
            return when (value) {
                is Prefab -> value.getSampleInstance()
                else -> value
            }
        }

    fun newInstance(): Saveable? {
        val base = sample ?: return null
        val clone = base.clone()
        if (clone is PrefabSaveable) {
            // unlink it for safety
            clone.prefab = null
        }
        return clone
    }

    fun <V : Saveable> newInstance(clazz: KClass<V>): V? {
        return clazz.safeCast(newInstance())
    }

    override fun destroy() {
        super.destroy()
        (value as? PrefabSaveable)?.destroy()
        FileWatch.removeWatchDog(reference)
    }

    override fun toString(): String {
        return "PrefabPair[$value]"
    }
}