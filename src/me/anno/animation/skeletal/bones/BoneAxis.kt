package me.anno.animation.skeletal.bones

import me.anno.utils.Maths.clamp
import org.joml.Vector3f

class BoneAxis(
        val values: Vector3f,
        val min: Float,
        val max: Float,
        val axis: Int) {

    constructor(bone: Bone, axis: Int) : this(bone.rotation, bone.minRotation[axis], bone.maxRotation[axis], axis)

    var value
        get() = values[axis]
        set(value) {
            val clamped = clamp(value, min, max)
            when(axis){
                0 -> values.x = clamped
                1 -> values.y = clamped
                2 -> values.z = clamped
            }
        }

}