package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.io.files.FileReference
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.log2

class LODMeshComponent() : MeshComponentBase() {

    @Suppress("unused")
    constructor(meshes: List<FileReference>) : this() {
        this.meshes = meshes
    }

    @Docs("Distance for LOD index 1")
    @Range(1e-150, 1e150)
    var lod1Dist = 10.0

    @Docs("How much the distance needs to change for the next index, e.g. 2 = double")
    @Range(1.0001, 1e5)
    var scalePerLOD = 2f
        set(value) {
            field = value
            lodScale = 0.5f / log2(scalePerLOD)
        }

    @Docs("Sources for LOD meshes; if resource cannot be found, no mesh will be rendered for its range")
    @Type("List<Mesh/Reference>")
    var meshes: List<FileReference> = emptyList()

    @Docs("Which LOD should be used for the bounds; -1 = use all LODs")
    @Range(-1.0, 2e9)
    var aabbIndex = 0
        set(value) {
            if (field != value) {
                field = value
                invalidateAABB()
            }
        }

    @NotSerializedProperty
    private var lodScale = 0.5f / log2(scalePerLOD)

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        if (aabbIndex < 0) {
            for (index in meshes.indices) {
                val mesh = MeshCache[meshes[index]] ?: continue
                fillSpace(mesh, globalTransform, aabb)
            }
        } else {
            val index = clamp(aabbIndex, 0, meshes.lastIndex)
            val mesh = MeshCache[meshes[index]]
            if (mesh != null) fillSpace(mesh, globalTransform, aabb)
        }
        return true
    }

    override fun getMeshOrNull(): Mesh? {
        val pos = RenderState.cameraPosition
        val transform = transform
        val index = if (transform != null) {
            val relDistSq = globalAABB.distanceSquared(pos) / (lod1Dist * lod1Dist)
            val index = (lodBias + lodScale * log2(relDistSq.toFloat())).toInt()
            clamp(index, 0, meshes.lastIndex)
        } else 0
        val ref = meshes.getOrNull(index)
        return MeshCache[ref]
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as LODMeshComponent
        dst.lod1Dist = lod1Dist
        dst.meshes = meshes // clone the list?
    }

    companion object {
        var lodBias = 0f
    }
}