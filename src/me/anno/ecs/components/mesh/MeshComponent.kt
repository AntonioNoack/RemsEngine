package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty

// todo light beams: when inside the cone, from that view, then add a little brightness

// static storage of things, e.g., for retargeting -> CacheSection.getEntry() / .getDualEntry()
// todo persistent storage of such things in files...
// todo skeleton identity shall be defined by same names-array (for finding retargetings)

// todo drag item into inspector, and when clicking on something, let it show up the the last recently used? would allow to drag things from one to the other :3

/////////////////////////////////////////////////////////////////////////////////////////


open class MeshComponent() : MeshComponentBase() {

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
    var meshFile: FileReference = InvalidRef
        set(value) {
            if (field != value) {
                field = value
                invalidateAABB()
            }
        }

    // todo why is getMeshOrNull with async not working to load prefabs properly???
    override fun getMeshOrNull(): Mesh? = MeshCache[meshFile, false]
    override fun getMesh(): Mesh? = MeshCache[meshFile, false]

    override fun onUpdate(): Int {
        super.onUpdate()
        // keep the mesh loaded
        meshFile = getReference(meshFile)
        return 1
    }

    // far into the future:
    // todo instanced animations for hundreds of humans:
    // todo bake animations into textures, and use indices + weights

    // on destroy we should maybe destroy the mesh:
    // only if it is unique, and owned by ourselves

    override fun onDestroy() {
        super.onDestroy()
        occlusionQuery?.destroy()
        occlusionQuery = null
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as MeshComponent
        dst.meshFile = meshFile
    }

    override val className: String get() = "MeshComponent"
}