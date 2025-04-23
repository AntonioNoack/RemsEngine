package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent

class FurMeshComponent(mesh: Mesh) : MeshComponent(mesh) {

    @Suppress("SetterBackingFieldAssignment")
    var furMaterial = FurMaterial()
        set(value) {
            value.copyInto(field)
        }

    init {
        materials = listOf(furMaterial.ref)
    }

    override fun getMeshOrNull(): Mesh? {
        val meshInstance = super.getMeshOrNull() as? Mesh
        meshInstance?.proceduralLength = furMaterial.numShells
        return meshInstance
    }
}