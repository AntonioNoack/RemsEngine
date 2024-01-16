package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d

/**
 * class for generating procedural meshes
 * todo animated procedural meshes
 * */
abstract class ProceduralMesh : MeshComponentBase() {

    val data = Mesh()

    override fun getMeshOrNull() = data

    @NotSerializedProperty
    var needsUpdate1 = true

    @DebugProperty
    val numberOfPoints
        get() = (data.positions?.size ?: -3) / 3

    @DebugProperty
    val numberOfTriangles: Int
        get() {
            val indices = data.indices
            if (indices != null) return indices.size / 3
            val positions = data.positions
            return if (positions != null) positions.size / 9 else -1
        }

    @DebugAction
    fun invalidateMesh() {
        needsUpdate1 = true
        // todo register for rare update? instead of onUpdate()
    }

    override fun getMesh(): Mesh {
        if (needsUpdate1) {
            needsUpdate1 = false
            generateMesh(data)
            data.invalidateGeometry()
            invalidateAABB()
        }
        return data
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        getMesh()
        return super.fillSpace(globalTransform, aabb)
    }

    abstract fun generateMesh(mesh: Mesh)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ProceduralMesh
        dst.needsUpdate1 = dst.needsUpdate1 || needsUpdate1
        dst.materials = materials
    }

    override fun onUpdate(): Int {
        getMesh()
        return 32
    }

    override fun onDestroy() {
        super.onDestroy()
        data.destroy()
    }
}