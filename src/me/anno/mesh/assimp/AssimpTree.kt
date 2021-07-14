package me.anno.mesh.assimp

import org.joml.Matrix4f
import org.lwjgl.assimp.AIMatrix4x4
import org.lwjgl.assimp.AINode

object AssimpTree {

    fun convert(m: AIMatrix4x4): Matrix4f {
        return Matrix4f(
            m.a1(), m.b1(), m.c1(), m.d1(),
            m.a2(), m.b2(), m.c2(), m.d2(),
            m.a3(), m.b3(), m.c3(), m.d3(),
            m.a4(), m.b4(), m.c4(), m.d4()
        )
    }

}