package me.anno.ecs.components.collider

import com.bulletphysics.collision.shapes.*
import com.bulletphysics.util.ObjectArrayList
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector3d
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MeshCollider() : Collider() {

    constructor(src: MeshCollider) : this() {
        src.copy(this)
    }

    @SerializedProperty
    var isConvex = true

    @Type("MeshBaseComponent/PrefabSaveable")
    var mesh: MeshBaseComponent? = null

    @HideInInspector
    @NotSerializedProperty
    var hull: ShapeHull? = null

    var isValid = false

    @DebugAction
    fun validate() {
        if (!isValid) {
            createBulletShape(Vector3d())
        }
    }

    @DebugProperty
    val info = "hull: $hull, ${mesh?.getMesh()?.positions?.size} positions"

    override fun createBulletShape(scale: Vector3d): CollisionShape {

        isValid = true

        if (mesh == null) {
            mesh = entity?.getComponentInChildren(MeshBaseComponent::class, false)
        }

        val mesh = mesh?.getMesh() ?: return SphereShape(0.5)

        val positions = mesh.positions
        if (positions == null) {
            isValid = false
            return defaultShape
        }

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
            this.hull = hull

            return ConvexHullShape(hull.vertexPointer)

        } else {

            this.hull = null

            // we don't send the data to the gpu here, so we don't need to allocate directly
            // this has the advantage, that the jvm will free the memory itself

            val indexCount = indices?.size ?: positions.size / 3
            val indices3 = ByteBuffer.allocate(4 * indexCount)
                .order(ByteOrder.nativeOrder())
            if (indices == null) {
                // 0 1 2 3 4 5 6 7 8 ...
                for (i in 0 until indexCount) {
                    indices3.putInt(i * 4, i)
                }
            } else indices3.asIntBuffer().put(indices)
            indices3.flip()

            val vertexBase = ByteBuffer
                .allocate(4 * positions.size)
                .order(ByteOrder.nativeOrder())

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
        validate()
        val hull = hull
        if (isConvex && hull != null) {
            val points = hull.vertexPointer
            val indices = hull.indexPointer
            for (i in 0 until hull.numIndices() step 3) {
                val a = points[indices[i]]
                val b = points[indices[i + 1]]
                val c = points[indices[i + 2]]
                LineShapes.drawLine(entity, a, b)
                LineShapes.drawLine(entity, b, c)
                LineShapes.drawLine(entity, c, a)
            }
        }
    }

    override fun clone(): MeshCollider {
        return MeshCollider(this)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshCollider
        clone.mesh = mesh
        clone.isConvex = isConvex
        clone.hull = hull
    }

    override val className get() = "MeshCollider"

    companion object {
        val defaultShape = BoxShape(javax.vecmath.Vector3d(1.0, 1.0, 1.0))
    }

}