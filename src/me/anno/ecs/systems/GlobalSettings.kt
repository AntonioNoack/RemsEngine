package me.anno.ecs.systems

import me.anno.ecs.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Registry for global settings.
 * */
object GlobalSettings : UpdateByClassSystem(), Updatable {

    override fun isInstance(component: Component): Boolean = component is GlobalSetting
    override fun update(sample: Component, instances: List<Component>) {}
    override fun getPriority(sample: Component): Int = 0

    fun <V> getOrNull(clazz: KClass<V>): V? where V : Component, V : GlobalSetting {
        val minPriority = getComponents(clazz).maxByOrNull { (it as GlobalSetting).priority }
        @Suppress("UNCHECKED_CAST")
        return minPriority as? V
    }

    fun <V> getDefault(clazz: KClass<V>): V where V : Component, V : GlobalSetting {
        val instance = defaultInstances.getOrPut(clazz) { clazz.createInstance() }
        @Suppress("UNCHECKED_CAST")
        return instance as V
    }

    operator fun <V> get(clazz: KClass<V>): V where V : Component, V : GlobalSetting {
        return getOrNull(clazz) ?: getDefault(clazz)
    }

    override fun update(instances: List<Updatable>) = execute()

    val defaultInstances = HashMap<KClass<*>, Component>()
}