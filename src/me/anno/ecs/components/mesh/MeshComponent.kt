package me.anno.ecs.components.mesh

import me.anno.cache.FileCacheValue
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

open class MeshComponent(meshFile: FileReference) : MeshComponentBase() {

    constructor() : this(InvalidRef)

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

    private val cachedMesh = FileCacheValue(meshFile, MeshCache::getEntry)

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