package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Events.addEvent
import me.anno.engine.serialization.NotSerializedProperty
import org.joml.AABBd
import org.joml.Matrix4x3d

/**
 * class for generating procedural meshes
 * todo animated procedural meshes -> GPUMesh maybe?
 * */
abstract class ProceduralMesh : MeshComponentBase() {

    abstract fun generateMesh(mesh: Mesh)

    val data = Mesh()

    override fun getMeshOrNull(): Mesh = data

    @NotSerializedProperty
    var needsMeshUpdate = true

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
        if(!needsMeshUpdate) {
            needsMeshUpdate = true
            addEvent(0) {
                getMesh()
            }
        }
    }

    override fun getMesh(): Mesh {
        if (needsMeshUpdate) {
            needsMeshUpdate = false
            generateMesh(data)
            data.invalidateGeometry()
            invalidateAABB()
        }
        return data
    }

    override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
        getMesh()
        return super.fillSpace(globalTransform, dstUnion)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ProceduralMesh) return
        dst.needsMeshUpdate = dst.needsMeshUpdate || needsMeshUpdate
        dst.materials = materials
    }

    override fun destroy() {
        super.destroy()
        data.destroy()
        needsMeshUpdate = false
    }
}