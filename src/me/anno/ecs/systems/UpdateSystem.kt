package me.anno.ecs.systems

import me.anno.ecs.Component

class UpdateSystem : UpdateByClassSystem() {
    override fun isInstance(component: Component): Boolean = component is Updatable
    override fun update(sample: Component, instances: Collection<Component>) {
        (sample as Updatable).update(instances)
    }
}