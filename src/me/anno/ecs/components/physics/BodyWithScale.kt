package me.anno.ecs.components.physics

import org.joml.Vector3d

class BodyWithScale<InternalRigidbody, ExternalRigidbody>(
    val internal: InternalRigidbody,
    val external: ExternalRigidbody,
    val scale: Vector3d
) {
    operator fun component1() = internal
    operator fun component2() = external
    operator fun component3() = scale
}