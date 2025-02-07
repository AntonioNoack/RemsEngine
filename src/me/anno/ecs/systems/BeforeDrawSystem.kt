package me.anno.ecs.systems

import me.anno.ecs.Component

object BeforeDrawSystem : UpdateByClassSystem(false) {
    override fun isInstance(component: Component): Boolean = component is OnBeforeDraw
    override fun getPriority(sample: Component): Int = (sample as? OnBeforeDraw)?.priority() ?: 0
    override fun update(sample: Component, instances: List<Component>) {
        (sample as? OnBeforeDraw)?.beforeDraw(instances)
    }
}