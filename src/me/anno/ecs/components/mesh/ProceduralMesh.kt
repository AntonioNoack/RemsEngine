package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.io.serialization.NotSerializedProperty
import me.anno.utils.types.AABBs.transformUnion
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class ProceduralMesh : MeshBaseComponent() {

    val mesh2 = Mesh()

    override fun getMesh() = mesh2

    @NotSerializedProperty
    var needsUpdate = true

    @DebugProperty
    val numberOfPoints get() = (mesh2.positions?.size ?: -3) / 3

    @DebugAction
    fun invalidate() {
        needsUpdate = true
        // todo register for rare update? instead of onUpdate()
    }

    override fun ensureBuffer() {
        if (needsUpdate) {
            needsUpdate = false
            generateMesh()
            mesh2.invalidateGeometry()
            entity?.invalidateAABBsCompletely()
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        // add aabb of that mesh with the transform
        ensureBuffer()
        mesh2.ensureBuffer()
        mesh2.aabb.transformUnion(globalTransform, aabb)
        return true
    }

    abstract fun generateMesh()

    abstract override fun clone(): ProceduralMesh

    override fun onUpdate(): Int {
        ensureBuffer()
        return 32
    }

}