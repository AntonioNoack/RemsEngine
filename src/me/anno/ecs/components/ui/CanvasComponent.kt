package me.anno.ecs.components.ui

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable

class CanvasComponent: Component() {

    override fun clone(): PrefabSaveable {
        TODO("Not yet implemented")
    }

    override val className get() = "CanvasComponent"

}