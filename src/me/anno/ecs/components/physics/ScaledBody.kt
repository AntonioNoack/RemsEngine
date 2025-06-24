package me.anno.ecs.components.physics

import org.joml.Vector3d

class ScaledBody<InternalRigidbody, ExternalRigidbody>(
    val internal: InternalRigidbody,
    val external: ExternalRigidbody,
    val scale: Vector3d,
    val centerOfMass: Vector3d
) {
    operator fun component1() = internal
    operator fun component2() = external
    operator fun component3() = scale
    operator fun component4() = centerOfMass
}