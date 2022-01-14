package me.anno.ecs.components.physics

import org.joml.Vector3d

class BodyWithScale<BodyType>(val body: BodyType, val scale: Vector3d) {
    operator fun component1() = body
    operator fun component2() = scale
}