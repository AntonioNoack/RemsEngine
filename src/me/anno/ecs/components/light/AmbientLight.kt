package me.anno.ecs.components.light

import me.anno.ecs.Component
import org.joml.Vector3f

class AmbientLight: Component() {

    var color = Vector3f(0.1f)

    override val className: String = "AmbientLight"

}