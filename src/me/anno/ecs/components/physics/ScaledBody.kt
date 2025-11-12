package me.anno.ecs.components.physics

import org.joml.Vector3d
import org.joml.Vector3f

class ScaledBody<InternalRigidbody, ExternalRigidbody>(
    val internal: InternalRigidbody,
    val external: ExternalRigidbody,
    val scale: Vector3f,
    val centerOfMass: Vector3d
) {
    operator fun component1() = internal
    operator fun component2() = external
    operator fun component3() = scale
    operator fun component4() = centerOfMass
}