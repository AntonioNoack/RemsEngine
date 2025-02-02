package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.TransformMesh.transformMesh
import me.anno.maths.Maths.TAUf
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.FloatArrayList
import org.joml.Matrix4x3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

object CapsuleModel {
    /**
     * creates a capsule mesh with <us> vertical and <vs> horizontal sections
     * */
    fun createCapsule(us: Int, vs: Int, axis: Axis, r: Float, h: Float, mesh: Mesh = Mesh()): Mesh {
        assertEquals(0, vs.and(1))
        UVSphereModel.createUVSphere(us, vs, mesh)
        val positions = FloatArrayList(16)
        val normals = FloatArrayList(16)
        fun addPoint(a: Vector3f, dy: Float) {
            positions.add(a.x * r, a.y * r + dy, a.z * r)
            normals.add(a)
        }
        mesh.forEachTriangle { a, b, c ->
            val dy = sign(a.y + b.y + c.y) * h
            addPoint(a, dy)
            addPoint(b, dy)
            addPoint(c, dy)
            false
        }
        fun addPoint(xi: Int, y: Float) {
            val angle = xi * TAUf / us
            val x = cos(angle)
            val z = sin(angle)
            positions.add(x * r, y, z * r)
            normals.add(x, 0f, z)
        }
        // add center ring
        for (x in 0 until us) {
            addPoint(x, -h)
            addPoint(x + 1, +h)
            addPoint(x + 1, -h)
            addPoint(x, -h)
            addPoint(x, +h)
            addPoint(x + 1, +h)
        }
        mesh.positions = positions.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.indices = null
        when (axis) {
            Axis.X -> transformMesh(mesh, Matrix4x3d().rotateZ(PI * 0.5))
            Axis.Y -> {}
            Axis.Z -> transformMesh(mesh, Matrix4x3d().rotateX(PI * 0.5))
        }
        mesh.invalidateGeometry()
        return mesh
    }
}