package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.files.FileReference
import me.anno.mesh.Shapes.flatCube

open class MeshComponent() : MeshComponentBase() {

    companion object {
        // using the flat cube as a default mesh seems a good choice :)
        private val defaultMeshRef = flatCube.front.ref
    }

    constructor(mesh: FileReference) : this() {
        this.name = mesh.nameWithoutExtension
        this.meshFile = mesh
    }

    constructor(mesh: FileReference, material: Material) : this(mesh, material.ref)
    constructor(mesh: FileReference, material: FileReference) : this(mesh) {
        super.materials = listOf(material)
    }

    constructor(mesh: Mesh) : this(mesh.ref)
    constructor(mesh: Mesh, material: Material) : this(mesh) {
        super.materials = listOf(material.ref)
    }

    @SerializedProperty
    @Type("Mesh/Reference")
    var meshFile: FileReference = defaultMeshRef
        set(value) {
            if (field != value) {
                field = value
                invalidateAABB()
            }
        }

    override fun getMeshOrNull(): IMesh? = MeshCache[meshFile, true]
    override fun getMesh(): IMesh? = MeshCache[meshFile, false]

    override fun destroy() {
        super.destroy()
        occlusionQuery?.destroy()
        occlusionQuery = null
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is MeshComponent) return
        dst.meshFile = meshFile
    }
}