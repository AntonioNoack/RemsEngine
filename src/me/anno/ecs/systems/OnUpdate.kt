package me.anno.ecs.systems

import me.anno.ecs.Component

/**
 * Component, that shall have a function be called each (simulation) frame
 * */
interface OnUpdate : Updatable {
    override fun update(instances: List<Component>) {
        for (i in instances.indices) {
            val instance = instances[i] as? OnUpdate ?: break
            instance.onUpdate()
        }
    }

    fun onUpdate()
}