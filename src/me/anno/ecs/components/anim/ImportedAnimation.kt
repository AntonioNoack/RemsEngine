package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import org.joml.Matrix4x3f

class ImportedAnimation : Animation() {

    var joints = ArrayList<Joint>()

    class Joint {

        var positions = FloatArray(0)
        var rotations = FloatArray(0)
        var scales = FloatArray(0)

    }

    override fun getMatrices(entity: Entity, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        TODO("Not yet implemented")
    }

    override val className: String = "ImportedAnimation"

}