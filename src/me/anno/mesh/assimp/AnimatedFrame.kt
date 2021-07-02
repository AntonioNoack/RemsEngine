package me.anno.mesh.assimp

import org.joml.Matrix4f
import org.joml.Matrix4x3f

class AnimatedFrame {
    val matrices = ArrayList<Matrix4x3f>()
    fun setMatrix(index: Int, matrix: Matrix4f) {
        for (i in matrices.size..index) {
            matrices.add(Matrix4x3f())
        }
        matrices[index].set(matrix)
        // if(index == 10) println(matrix) // samples
    }

    override fun toString(): String {
        return matrices.size.toString()//matrices.joinToString("\n"){ "\t${it.print()}"}
    }
}