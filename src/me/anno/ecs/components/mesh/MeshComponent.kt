package me.anno.ecs.components.mesh

import me.anno.cache.FileCacheValue
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.files.FileReference

open class MeshComponent() : MeshComponentBase() {

    companion object {
        // using the flat cube as a default mesh seems a good choice :)
        private val defaultMeshRef = flatCube.ref
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
    var meshFile: FileReference
        get() = cachedMesh.file
        set(value) {
            if (cachedMesh.file != value) {
                cachedMesh.file = value
                invalidateBounds()
            }
        }

    private val cachedMesh = FileCacheValue(defaultMeshRef, MeshCache::getEntry)

    override fun getMeshOrNull(): IMesh? = cachedMesh.value
    override fun getMesh(): IMesh? = cachedMesh.waitFor().waitFor()

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