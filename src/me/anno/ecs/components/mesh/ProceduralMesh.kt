package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.transformUnion
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class ProceduralMesh : MeshComponentBase() {

    val mesh2 = Mesh()

    override fun getMesh() = mesh2

    @NotSerializedProperty
    var needsUpdate = true

    @DebugProperty
    val numberOfPoints
        get() = (mesh2.positions?.size ?: -3) / 3

    @DebugProperty
    val numberOfTriangles: Int
        get() {
            val indices = mesh2.indices
            if (indices != null) return indices.size / 3
            val positions = mesh2.positions
            return if (positions != null) positions.size / 9 else -1
        }

    @DebugProperty
    @NotSerializedProperty
    val localAABB = AABBd()

    @DebugProperty
    @NotSerializedProperty
    val globalAABB = AABBd()

    @DebugAction
    fun invalidateMesh() {
        needsUpdate = true
        // todo register for rare update? instead of onUpdate()
    }

    override fun ensureBuffer() {
        if (needsUpdate) {
            needsUpdate = false
            generateMesh(mesh2)
            mesh2.invalidateGeometry()
            invalidateAABB()
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        // add aabb of that mesh with the transform
        ensureBuffer()
        mesh2.ensureBuffer()
        localAABB.set(mesh2.aabb)
        globalAABB.clear()
        mesh2.aabb.transformUnion(globalTransform, globalAABB)
        aabb.union(globalAABB)
        return true
    }

    abstract fun generateMesh(mesh: Mesh)

    abstract override fun clone(): ProceduralMesh

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ProceduralMesh
        clone.needsUpdate = clone.needsUpdate || needsUpdate
        clone.materials = materials
    }

    override fun onUpdate(): Int {
        ensureBuffer()
        return 32
    }

}