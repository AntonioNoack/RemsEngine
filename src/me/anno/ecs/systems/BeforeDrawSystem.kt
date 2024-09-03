package me.anno.ecs.systems

import me.anno.ecs.Component

object BeforeDrawSystem : UpdateByClassSystem(false) {
    override fun isInstance(component: Component): Boolean = component is OnBeforeDraw
    override fun update(sample: Component, instances: Collection<Component>) {
        (sample as? OnBeforeDraw)?.beforeDraw(instances)
    }
}