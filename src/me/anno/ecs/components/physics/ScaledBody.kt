package me.anno.ecs.components.physics

import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f

class ScaledBody<InternalRigidbody, ExternalRigidbody>(
    val internal: InternalRigidbody,
    val external: ExternalRigidbody,
    val scale: Vector3f,
    val centerOfMass: Vector3d,
    time1: Long, transform: Matrix4x3
) : InterpolatedTransform(time1){

    init {
        Physics.convertEntityToPhysicsI(transform, position0, rotation0, scale, centerOfMass)
        position1.set(position0)
        rotation1.set(rotation0)
    }

    operator fun component1() = internal
    operator fun component2() = external
    operator fun component3() = scale
    operator fun component4() = centerOfMass
}