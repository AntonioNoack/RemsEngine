package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3f

class AmbientLight : Component() {

    var color = Vector3f(0.1f)

    override fun clone(): AmbientLight {
        val clone = AmbientLight()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AmbientLight
        clone.color = color
    }

    override val className: String = "AmbientLight"

}