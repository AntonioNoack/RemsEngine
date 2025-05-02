package me.anno.ecs.systems

import me.anno.ecs.Component

object BeforeDrawSystem : UpdateByClassSystem(), OnBeforeDraw {
    override fun isInstance(component: Component): Boolean = component is OnBeforeDraw
    override fun getPriority(sample: Component): Int = (sample as? OnBeforeDraw)?.priority() ?: 0
    override fun update(sample: Component, instances: List<Component>) {
        @Suppress("UNCHECKED_CAST")
        (sample as? OnBeforeDraw)?.beforeDraw(instances as List<OnBeforeDraw>)
    }

    override fun onBeforeDraw() {
        execute()
    }
}