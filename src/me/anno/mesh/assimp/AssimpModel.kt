package me.anno.mesh.assimp

import me.anno.ecs.Component
import org.joml.Matrix4f

class AssimpModel: Component() {

    val meshes = ArrayList<AssimpMesh>()
    val transform = Matrix4f()

    override val className get() = "AssimpModel"

}