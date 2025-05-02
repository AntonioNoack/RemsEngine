package me.anno.ecs.systems

import me.anno.ecs.systems.Systems.forAllSystems

/**
 * Component, that shall have a function be called each (simulation) frame
 * */
interface OnBeforeDraw {
    fun onBeforeDraw()
    fun beforeDraw(instances: List<OnBeforeDraw>) {
        for (instance in instances) {
            instance.onBeforeDraw()
        }
    }

    fun priority(): Int = 100 // lower values get drawn first; sorted on per-class-basis only

    companion object {
        fun onBeforeDrawing() {
            forAllSystems(OnBeforeDraw::class) { system ->
                system.beforeDraw(listOf(system))
            }
        }
    }
}