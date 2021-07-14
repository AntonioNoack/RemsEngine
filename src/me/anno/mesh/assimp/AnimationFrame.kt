package me.anno.mesh.assimp

import org.joml.Matrix4f

class AnimationFrame(boneCount: Int) {

    val matrices = Array(boneCount) { Matrix4f() }

    override fun toString(): String {
        // matrices.joinToString("\n"){ "\t${it.print()}"}
        return matrices.size.toString()
    }

}