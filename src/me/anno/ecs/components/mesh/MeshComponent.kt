package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Type
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty

// todo light beams: when inside the cone, from that view, then add a little brightness

// done (file) references to meshes and animations inside mesh files
//      - bird.fbx:anim:walk
//      - bird.fbx:mesh:wings
// todo static storage of things, e.g. for retargeting
// todo skeleton identity shall be defined by same names-array (for finding retargetings)

// todo drag item into inspector, and when clicking on something, let it show up the the last recently used? would allow to drag things from one to the other :3

// todo packages are mods/plugins, which are just installed with dependencies?

/////////////////////////////////////////////////////////////////////////////////////////


open class MeshComponent() : MeshComponentBase() {

    constructor(mesh: FileReference) : this() {
        this.mesh = mesh
    }

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

    override fun onVisibleUpdate(): Boolean {
        mesh = getReference(mesh)
        return true
    }

    // far into the future:
    // todo instanced animations for hundreds of humans:
    // todo bake animations into textures, and use indices + weights

    // on destroy we should maybe destroy the mesh:
    // only if it is unique, and owned by ourselves

    override fun clone(): MeshComponent {
        val clone = MeshComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshComponent
        clone.mesh = mesh
    }

    override val className get() = "MeshComponent"

}