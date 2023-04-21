package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty

// todo light beams: when inside the cone, from that view, then add a little brightness

// todo static storage of things, e.g., for retargeting
// todo skeleton identity shall be defined by same names-array (for finding retargetings)

// todo drag item into inspector, and when clicking on something, let it show up the the last recently used? would allow to drag things from one to the other :3

/////////////////////////////////////////////////////////////////////////////////////////


open class MeshComponent() : MeshComponentBase() {

    constructor(mesh: FileReference) : this() {
        this.name = mesh.nameWithoutExtension
        this.mesh = mesh
    }

    constructor(mesh: Mesh) : this(mesh.ref)

    @SerializedProperty
    @Type("Mesh/Reference")
    var mesh: FileReference = InvalidRef
        set(value) {
            if (field != value) {
                field = value
                invalidateAABB()
            }
        }

    override fun getMesh(): Mesh? = MeshCache[mesh]

    override fun onUpdate(): Int {
        super.onUpdate()
        // keep the mesh loaded
        mesh = getReference(mesh)
        return 1
    }

    override fun ensureBuffer() {
        super.ensureBuffer()
        getMesh()?.ensureBuffer()
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
        dst.mesh = mesh
    }

    override val className: String get() = "MeshComponent"

}