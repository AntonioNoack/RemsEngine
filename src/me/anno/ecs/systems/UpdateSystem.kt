package me.anno.ecs.systems

import me.anno.ecs.Component

object UpdateSystem : UpdateByClassSystem(), OnUpdate {
    override fun isInstance(component: Component): Boolean = component is Updatable
    override fun getPriority(sample: Component): Int = (sample as? Updatable)?.priority() ?: 0
    override fun update(sample: Component, instances: List<Component>) {
        @Suppress("UNCHECKED_CAST")
        (sample as? Updatable)?.update(instances as List<Updatable>)
    }

    override fun onUpdate() {
        execute()
    }
}