package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.*
import com.bulletphysics.util.ObjectArrayList
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import org.lwjgl.system.MemoryUtil

class MeshCollider() : Collider() {

    constructor(src: MeshCollider) : this() {
        src.copy(this)
    }

    @SerializedProperty
    var isConvex = true

    @Type("Mesh")
    var mesh: Mesh? = null

    override fun createBulletShape(scale: Vector3d): CollisionShape {

        if (mesh == null) {
            mesh = entity?.getComponent(Mesh::class, false)
            me.anno.utils.LOGGER.warn("searched for mesh, found $mesh")
        }

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
                        positions[i + 0] * scale.x,
                        positions[i + 1] * scale.y,
                        positions[i + 2] * scale.z
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
            vertexBase.asFloatBuffer().apply {
                if (scale.x == 1.0 && scale.y == 1.0 && scale.z == 1.0) {
                    put(positions)
                } else {
                    val sx = scale.x.toFloat()
                    val sy = scale.y.toFloat()
                    val sz = scale.z.toFloat()
                    for (i in positions.indices step 3) {
                        put(positions[i + 0] * sx)
                        put(positions[i + 1] * sy)
                        put(positions[i + 2] * sz)
                    }
                }
            }
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

    override fun clone(): MeshCollider {
        return MeshCollider(this)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshCollider
        clone.mesh = mesh
        clone.isConvex = isConvex
    }

    override val className get() = "MeshCollider"

}