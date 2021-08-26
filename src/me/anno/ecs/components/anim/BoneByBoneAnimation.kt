package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.mesh.assimp.AnimationFrame
import org.joml.Matrix4f
import org.joml.Matrix4x3f

class BoneByBoneAnimation() : Animation() {

    val joints = ArrayList<Joint>()

    class Joint {
        val matrices = ArrayList<Matrix4x3f>()
    }

    override fun getMatrices(entity: Entity, time: Float, dst: Array<Matrix4x3f>): Array<Matrix4x3f>? {
        TODO("Not yet implemented")
    }

    override val className: String = "BoneByBoneAnimation"

}