package me.anno.ecs.systems

import me.anno.ecs.Component

/**
 * Component, that shall have a function be called each (simulation) frame
 * */
interface OnBeforeDraw {
    fun onBeforeDraw()
    fun beforeDraw(instances: Collection<Component>) {
        for (instance in instances) {
            (instance as? OnBeforeDraw)?.onBeforeDraw()
        }
    }

    fun priority(): Int = 100 // lower values get drawn first; sorted on per-class-basis only
}