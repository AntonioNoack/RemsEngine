package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.*
import com.bulletphysics.util.ObjectArrayList
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import org.lwjgl.system.MemoryUtil

class MeshCollider : Collider() {

    @SerializedProperty
    var isConvex = true

    var mesh: MeshComponent? = null

    override fun getSignedDistance(deltaPosition: Vector3d, movement: Vector3d): Double {
        TODO("Not yet implemented")
    }

    override fun createBulletShape(): CollisionShape {

        val mesh = mesh ?: return SphereShape(0.5)

        val positions = mesh.positions!!
        val indices = mesh.indices

        if (isConvex) {

            // calculate convex hull
            // simplify it maybe

            val points = ObjectArrayList<javax.vecmath.Vector3d>(positions.size / 3)
            for (i in positions.indices step 3) {
                points.add(
                    javax.vecmath.Vector3d(
                        positions[i + 0].toDouble(),
                        positions[i + 1].toDouble(),
                        positions[i + 2].toDouble()
                    )
                )
            }

            val convex = ConvexHullShape(points)
            if (points.size < 10) return convex

            val hull = ShapeHull(convex)
            hull.buildHull(convex.margin)
            return ConvexHullShape(hull.vertexPointer)

        } else {

            val indexCount = indices?.size ?: positions.size / 3
            val indices3 = MemoryUtil.memAlloc(4 * indexCount)
            if (indices == null) {
                // 0 1 2 3 4 5 6 7 8 ...
                for (i in 0 until indexCount) {
                    indices3.putInt(i * 4, i)
                }
            } else indices3.asIntBuffer().put(indices)
            indices3.flip()

            val vertexBase = MemoryUtil.memAlloc(4 * positions.size)
            vertexBase.asFloatBuffer().put(positions)
            vertexBase.flip()

            // int numTriangles, ByteBuffer triangleIndexBase, int triangleIndexStride, int numVertices, ByteBuffer vertexBase, int vertexStride
            // we can use floats, because we have extended the underlying class
            val smi = TriangleIndexVertexArray(indexCount, indices3, 4, positions.size, vertexBase, 12)
            return BvhTriangleMeshShape(smi, true, true)

        }
    }

    override fun drawShape() {
        // todo draw underlying collider shape
    }

    override val className get() = "MeshCollider"

}