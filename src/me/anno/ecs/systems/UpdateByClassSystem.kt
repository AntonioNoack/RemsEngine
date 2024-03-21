package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import kotlin.reflect.KClass

abstract class UpdateByClassSystem : System() {

    private val components = HashMap<KClass<*>, HashSet<Component>>()
    override fun onEnable(component: Component) {
        if (isInstance(component)) {
            components.getOrPut(component::class) { HashSet() }.add(component)
        }
    }

    override fun onDisable(component: Component) {
        if (isInstance(component)) {
            components[component::class]?.remove(component)
        }
    }

    abstract fun isInstance(component: Component): Boolean
    abstract fun update(sample: Component, instances: Collection<Component>)

    override fun onUpdate() {
        for ((_, instances) in components) {
            val sample = instances.firstOrNull() ?: continue
            update(sample, instances)
        }
    }

    override fun onEnable(childSystem: System) {
        val child = (childSystem as? UpdateByClassSystem)?.components ?: return
        for ((clazz, componentsByChild) in child) {
            val comp = components.getOrPut(clazz) { HashSet() }
            comp.addAll(componentsByChild)
        }
    }

    override fun onDisable(childSystem: System) {
        val child = (childSystem as? UpdateByClassSystem)?.components ?: return
        for ((clazz, componentsByChild) in child) {
            val comp = components[clazz] ?: continue
            comp.removeAll(componentsByChild)
        }
    }
}