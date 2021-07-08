package me.anno.mesh.assimp

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.MeshComponent
import org.joml.Matrix4f

class AssimpModel : Component() {

    val meshes = ArrayList<MeshComponent>()
    val transform = Matrix4f()

    override val className get() = "AssimpModel"

}