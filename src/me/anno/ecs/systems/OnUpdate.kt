package me.anno.ecs.systems

import me.anno.ecs.Component

/**
 * Component, that shall have a function be called each (simulation) frame
 * */
interface OnUpdate : Updatable {
    override fun update(instances: Collection<Component>) {
        for (instance in instances) {
            (instance as? OnUpdate)?.onUpdate()
        }
    }

    fun onUpdate()
}