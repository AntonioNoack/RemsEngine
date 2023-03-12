package me.anno.mesh.assimp

import org.joml.Matrix4x3f
import org.lwjgl.assimp.AIMatrix4x4

object AssimpTree {

    fun convert(m: AIMatrix4x4): Matrix4x3f {
        return Matrix4x3f(
            m.a1(), m.b1(), m.c1(),
            m.a2(), m.b2(), m.c2(),
            m.a3(), m.b3(), m.c3(),
            m.a4(), m.b4(), m.c4(),
        )
    }

}