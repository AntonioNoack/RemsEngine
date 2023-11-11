package me.anno.ecs.components.mesh.fur

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.io.files.FileReference

class FurMeshRenderer : MeshComponent {

    constructor() : super()
    constructor(mesh: Mesh) : super(mesh)
    constructor(meshFile: FileReference) : super(meshFile)

    @Suppress("SetterBackingFieldAssignment")
    var furMaterial = FurMaterial()
        set(value) {
            value.copyInto(field)
        }

    init {
        materials = listOf(furMaterial.ref)
    }

    override fun getMeshOrNull(): Mesh? {
        val meshInstance = super.getMeshOrNull()
        meshInstance?.proceduralLength = furMaterial.numShells
        return meshInstance
    }
}