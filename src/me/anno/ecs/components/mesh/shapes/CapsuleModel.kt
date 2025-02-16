package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.TransformMesh.transform
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.addUnsafe
import org.joml.Matrix4x3m
import org.joml.Vector3f
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
        val numSrcTriangles = mesh.numPrimitives.toInt()
        val numDstTriangles = numSrcTriangles + 6 * us
        val positions = FloatArrayList(numDstTriangles * 9)
        val normals = FloatArrayList(numDstTriangles * 9)
        fun addPoint(a: Vector3f, dy: Float) {
            positions.addUnsafe(a.x * r, a.y * r + dy, a.z * r)
            normals.addUnsafe(a)
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
            positions.addUnsafe(x * r, y, z * r)
            normals.addUnsafe(x, 0f, z)
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
        assertEquals(numDstTriangles * 3, positions.size)
        assertEquals(numDstTriangles * 3, normals.size)
        mesh.positions = positions.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.indices = null
        when (axis) {
            Axis.X -> mesh.transform(Matrix4x3m().rotateZ(PIf * 0.5f))
            Axis.Y -> {}
            Axis.Z -> mesh.transform(Matrix4x3m().rotateX(PIf * 0.5f))
        }
        mesh.invalidateGeometry()
        return mesh
    }
}