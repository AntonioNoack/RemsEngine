package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.annotations.DebugProperty
import kotlin.reflect.KClass

abstract class UpdateByClassSystem(val isOnUpdate: Boolean) : System() {

    @DebugProperty
    val numRegisteredClasses: Int
        get() = components.count { it.value.isNotEmpty() }

    @DebugProperty
    val numRegisteredInstances: Int
        get() = components.values.sumOf { it.size }

    private val components = HashMap<KClass<*>, HashSet<Component>>()
    override fun setContains(component: Component, contains: Boolean) {
        if (isInstance(component)) {
            val clazz = component::class
            if (contains) {
                components.getOrPut(clazz, ::HashSet).add(component)
            } else {
                components[clazz]?.remove(component)
            }
        }
    }

    override fun clear() {
        components.clear()
    }

    abstract fun isInstance(component: Component): Boolean
    abstract fun update(sample: Component, instances: Collection<Component>)

    override fun onUpdate() {
        if (isOnUpdate) execute()
    }

    override fun onBeforeDrawing() {
        if (!isOnUpdate) execute()
    }

    private fun execute() {
        for ((_, instances) in components) {
            val sample = instances.firstOrNull() ?: continue
            update(sample, instances)
        }
    }
}