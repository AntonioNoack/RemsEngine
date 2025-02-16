package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.RenderState
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.clamp
import org.joml.AABBd
import org.joml.Matrix4x3
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

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        if (aabbIndex < 0) {
            fillSpaceStart()
            for (index in meshes.indices) {
                fillSpaceAdd(MeshCache[meshes[index]] ?: continue)
            }
            fillSpaceEnd(globalTransform, dstUnion)
        } else {
            val index = clamp(aabbIndex, 0, meshes.lastIndex)
            val mesh = MeshCache[meshes[index]]
            if (mesh != null) fillSpaceSet(mesh, globalTransform, dstUnion)
        }
        return true
    }

    @DebugProperty
    val lodIndex: Int get() = getLODIndex()

    private fun getLODIndex(): Int {
        val pos = RenderState.cameraPosition
        val distSq = if (transform != null) {
            // globalAABB should be filled
            globalAABB.distanceSquared(pos)
        } else {
            val mesh = MeshCache[meshes.firstOrNull()] ?: return 0 // maybe should use aabbIndex
            mesh.getBounds().distanceSquared(pos)
        }
        val relDistSq = distSq / (lod1Dist * lod1Dist)
        val index = (lodBias + lodScale * log2(relDistSq.toFloat())).toInt()
        return clamp(index, 0, meshes.lastIndex)
    }

    private fun getLODMeshRef(): FileReference {
        val index = getLODIndex()
        return meshes.getOrNull(index) ?: InvalidRef
    }

    override fun getMeshOrNull(): Mesh? = MeshCache[getLODMeshRef(), true]
    override fun getMesh(): IMesh? = MeshCache[getLODMeshRef(), false]

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is LODMeshComponent) return
        dst.lod1Dist = lod1Dist
        dst.meshes = meshes // clone the list?
    }

    companion object {
        var lodBias = 0f
    }
}