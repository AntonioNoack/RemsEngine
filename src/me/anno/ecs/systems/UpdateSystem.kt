package me.anno.ecs.systems

import me.anno.ecs.Component

object UpdateSystem : UpdateByClassSystem(true) {
    override fun isInstance(component: Component): Boolean = component is Updatable
    override fun getPriority(sample: Component): Int = (sample as? Updatable)?.priority() ?: 0
    override fun update(sample: Component, instances: Collection<Component>) {
        (sample as? Updatable)?.update(instances)
    }
}