package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Matrices.distanceSquared
import org.joml.AABBd
import org.joml.Matrix4x3d
import kotlin.math.log2

class LODMeshComponent : MeshComponentBase() {

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
    var meshes = ArrayList<FileReference>()

    @Docs("Which LOD should be used for the bounds; -1 = use all LODs")
    @Range(-1.0, 2e9)
    var aabbIndex = -1
        set(value) {
            if (field != value) {
                field = value
                invalidateAABB()
            }
        }

    @NotSerializedProperty
    private var lodScale = 0.5f / log2(scalePerLOD)

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        if (aabbIndex < 0) {
            for (index in meshes.indices) {
                val mesh = MeshCache[meshes[index]] ?: continue
                fillSpace(mesh, globalTransform, aabb)
            }
        } else {
            val index = aabbIndex
            val mesh = MeshCache[meshes.getOrNull(clamp(index, 0, meshes.lastIndex))]
            if (mesh != null) fillSpace(mesh, globalTransform, aabb)
        }
        return true
    }

    override fun getMesh(): Mesh? {
        val pos = RenderView.camPosition
        val transform = transform
        val index = if (transform != null) {
            val lod1Dist = lod1Dist
            val relDistSq = transform.globalTransform.distanceSquared(pos) / (lod1Dist * lod1Dist)
            val index = (lodBias + lodScale * log2(relDistSq.toFloat())).toInt()
            clamp(index, 0, meshes.lastIndex)
        } else 0
        val ref = meshes.getOrNull(index)
        return MeshCache[ref]
    }

    override fun clone(): Component {
        val clone = LODMeshComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LODMeshComponent
        clone.lod1Dist = lod1Dist
        clone.meshes.clear()
        clone.meshes.addAll(meshes)
    }

    override val className = "LODMeshComponent"

    companion object {
        var lodBias = 0f
    }

}