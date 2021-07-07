package me.anno.mesh.assimp

import org.joml.Matrix4f

class Bone(
    val id: Int, val name: String,
    val offsetMatrix: Matrix4f // inverse bind pose (?)
) {

    lateinit var parent: Bone

    val skinningMatrix = Matrix4f()

}
