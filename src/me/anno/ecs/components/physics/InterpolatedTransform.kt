package me.anno.ecs.components.physics

import org.joml.Quaternionf
import org.joml.Vector3d

/**
 * Physics engines use position + rotation only, no scale.
 * Also, physics should run at a fixed rate, while varying FPS would make it run out of sync.
 * This class shall provide correctly interpolated values.
 *
 * The physics engine shall calculate a frame into the future, so we avoid extrapolation.
 * */
open class InterpolatedTransform(var time1: Long) {

    val position0 = Vector3d()
    val position1 = Vector3d()
    val rotation0 = Quaternionf()
    val rotation1 = Quaternionf()

    fun push(newTime: Long) {
        position0.set(position1)
        rotation0.set(rotation1)
        time1 = newTime
    }

    fun interpolate(targetTime: Long, invDt: Float, dstPosition: Vector3d, dstRotation: Quaternionf) {
        val t = (targetTime - time1) * invDt + 1f
        position0.mix(position1, t.toDouble(), dstPosition)
        rotation0.slerp(rotation1, t, dstRotation)
    }
}