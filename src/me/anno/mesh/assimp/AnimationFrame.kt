package me.anno.mesh.assimp

import org.joml.Matrix4x3f

class AnimationFrame(boneCount: Int) {

    val skinningMatrices = Array(boneCount) { Matrix4x3f() }

    override fun toString(): String {
        // matrices.joinToString("\n"){ "\t${it.print()}"}
        return skinningMatrices.size.toString()
    }

}