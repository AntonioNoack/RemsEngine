package me.anno.ecs.components.mesh.decal

import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.mesh.Shapes

class DecalMeshComponent : MeshComponentBase() {

    val material = DecalMaterial()

    init {
        materials = listOf(material.ref)
    }

    override fun getMeshOrNull() = Shapes.smoothCube.back
}