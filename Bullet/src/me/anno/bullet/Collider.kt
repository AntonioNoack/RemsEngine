package me.anno.bullet

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape
import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.ShapeHull.Companion.NUM_UNIT_SPHERE_POINTS
import com.bulletphysics.collision.shapes.ShapeHull.Companion.constUnitSpherePoints
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.bullet.BulletPhysics.Companion.mat4x3ToTransform
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.physics.CustomBulletCollider
import me.anno.maths.geometry.convexhull.ConvexHulls
import me.anno.maths.geometry.convexhull.HullDesc
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val LOGGER = LogManager.getLogger("Collider")

fun verticesToFloatArray(vertices: List<Vector3d>): FloatArray {
    val dst = FloatArray(vertices.size * 3)
    for (i in vertices.indices) {
        vertices[i].get(dst, i * 3)
    }
    return dst
}

fun MeshCollider.createBulletMeshShape(scale: Vector3d): CollisionShape {

    val mesh = mesh as? Mesh ?: return defaultShape

    val positions = mesh.positions
    if (positions == null) {
        isValid = false
        return defaultShape
    }

    isValid = true

    val indices = mesh.indices

    Stack.reset(false)

    if (isConvex) {
        // calculate convex hull
        // simplify it maybe

        val convex = ConvexHullShape(positions, null)

        convex.setLocalScaling(scale)
        convex.margin = 0.0

        val min = Vector3d()
        val max = Vector3d()
        convex.getBounds(Transform(), min, max)

        if (positions.size < 30 || !enableSimplifications) {
            convex.margin = margin.toDouble()
            return convex
        }

        val directions = ArrayList<Vector3d>(NUM_UNIT_SPHERE_POINTS)
        for (i in constUnitSpherePoints.indices) {
            directions.add(Vector3d(constUnitSpherePoints[i]))
        }

        val tmp = Vector3d()
        for (i in directions.indices) {
            val v = directions[i]
            convex.localGetSupportingVertex(tmp.set(v), v)
        }

        val hull = ConvexHulls.calculateConvexHull(HullDesc(directions))
        if (hull == null) {
            LOGGER.warn("Failed to create convex hull for $directions")
            convex.margin = margin.toDouble()
            return convex
        }

        val shape = ConvexHullShape(verticesToFloatArray(hull.vertices), hull.triangles)
        shape.margin = margin.toDouble()
        bulletInstance = shape
        return shape

    } else {

        // we don't send the data to the gpu here, so we don't need to allocate directly
        // this has the advantage, that the jvm will free the memory itself
        val vertexCount = positions.size / 3
        val indexCount = indices?.size ?: vertexCount
        val indices3 = ByteBuffer.allocate(4 * indexCount)
            .order(ByteOrder.nativeOrder())

        val indices3i = indices3.asIntBuffer()
        if (indices == null) {
            // 0 1 2 3 4 5 6 7 8 ...
            for (i in 0 until indexCount) {
                indices3i.put(i, i)
            }
        } else {
            val illegalIndex = indices.indexOfFirst { it !in 0 until vertexCount }
            if (illegalIndex >= 0) {
                LOGGER.warn(
                    "Out of bounds index: ${indices[illegalIndex]} " +
                            "at position $illegalIndex !in 0 until $vertexCount"
                )
            }
            indices3i.put(indices)
        }
        indices3i.flip()

        val vertexBase = ByteBuffer
            .allocate(4 * positions.size)
            .order(ByteOrder.nativeOrder())

        val fb = vertexBase.asFloatBuffer()
        if (scale.x == 1.0 && scale.y == 1.0 && scale.z == 1.0) {
            fb.put(positions)
        } else {
            val sx = scale.x.toFloat()
            val sy = scale.y.toFloat()
            val sz = scale.z.toFloat()
            val tmp = Vector3f()
            forLoop(0, positions.size - 2, 3) { i ->
                tmp.set(positions[i], positions[i + 1], positions[i + 2])
                fb.put(tmp.x * sx)
                fb.put(tmp.y * sy)
                fb.put(tmp.z * sz)
            }
        }
        fb.flip()


        val triangleCount = indexCount / 3
        // int numTriangles, ByteBuffer triangleIndexBase, int triangleIndexStride, int numVertices, ByteBuffer vertexBase, int vertexStride
        // we can use floats, because we have extended the underlying class
        val smi = TriangleIndexVertexArray(
            triangleCount, indices3, 12,
            vertexCount, vertexBase, 12
        )
        val shape = BvhTriangleMeshShape(smi, true, true)
        shape.margin = margin.toDouble()
        return shape
    }
}

fun CapsuleCollider.createBulletCapsuleShape(scale: Vector3d): CapsuleShape {
    return when (axis) {
        Axis.X -> CapsuleShape(radius * scale.y, halfHeight * scale.x * 2.0, axis) // x
        Axis.Y -> CapsuleShape(radius * scale.x, halfHeight * scale.y * 2.0, axis) // y
        Axis.Z -> CapsuleShape(radius * scale.x, halfHeight * scale.z * 2.0, axis) // z
    }.apply { margin = roundness.toDouble() }
}

fun ConeCollider.createBulletConeShape(scale: Vector3d): ConeShape {
    val axis = axis
    return when (axis) {
        Axis.X -> ConeShape(radius * scale.y, height * scale.x, axis)
        Axis.Y -> ConeShape(radius * scale.x, height * scale.y, axis)
        Axis.Z -> ConeShape(radius * scale.x, height * scale.z, axis)
    }.apply { margin = roundness.toDouble() }
}

fun CylinderCollider.createBulletCylinderShape(scale: Vector3d): CylinderShape {
    val axis = axis
    return when (axis) {
        Axis.X -> CylinderShape(Vector3d(halfHeight * scale.x, radius * scale.y, radius * scale.z), axis)
        Axis.Y -> CylinderShape(Vector3d(radius * scale.x, halfHeight * scale.y, radius * scale.z), axis)
        Axis.Z -> CylinderShape(Vector3d(radius * scale.x, radius * scale.y, halfHeight * scale.z), axis)
    }.apply { margin = roundness.toDouble() }
}

fun BoxCollider.createBulletBoxShape(scale: Vector3d): BoxShape {
    return BoxShape(
        Vector3d(
            halfExtents.x * scale.x,
            halfExtents.y * scale.y,
            halfExtents.z * scale.z
        )
    ).apply { margin = roundness * scale.absMax() }
}

fun SphereCollider.createBulletSphereShape(scale: Vector3d): SphereShape {
    return SphereShape(radius.toDouble()).apply {
        setLocalScaling(Vector3d(scale.x, scale.y, scale.z))
    }
}

fun createBulletShape(collider: Collider, scale: Vector3d): CollisionShape {
    return when (collider) {
        is MeshCollider -> collider.createBulletMeshShape(scale)
        is CapsuleCollider -> collider.createBulletCapsuleShape(scale)
        is ConeCollider -> collider.createBulletConeShape(scale)
        is ConvexCollider -> {
            val shape = ConvexHullShape(collider.points!!, null)
            shape.setLocalScaling(scale)
            shape.margin = collider.roundness * scale.absMax()
            shape
        }
        is CylinderCollider -> collider.createBulletCylinderShape(scale)
        // is CircleCollider -> SphereShape(collider.radius * scale.dot(0.33, 0.34, 0.33))
        is SphereCollider -> collider.createBulletSphereShape(scale)
        is BoxCollider -> collider.createBulletBoxShape(scale)
        is InfinitePlaneCollider -> StaticPlaneShape(Vector3d(0.0, 1.0, 0.0), 0.0)
        /*is RectCollider -> {
            val halfExtents = collider.halfExtents
            return BoxShape(
                Vector3d(
                    halfExtents.x * scale.x,
                    halfExtents.y * scale.y,
                    scale.z
                )
            )
        }*/
        is CustomBulletCollider -> collider.createBulletCollider(scale) as CollisionShape
        // is SDFCollider -> collider.createBulletShape(scale)
        else -> defaultShape
    }
}

fun createBulletCollider(
    collider: Collider, base: Entity,
    bodyScale: Vector3d, centerOfMass: Vector3d
): Pair<Transform, CollisionShape> {
    val transform0 = collider.entity!!.fromLocalToOtherLocal(base)

    val localScale = JomlPools.vec3d.create()
    val totalScale = JomlPools.vec3d.create()
    val centerOfMass1 = JomlPools.vec3d.create()

    transform0.getScale(localScale)
    bodyScale.mul(localScale, totalScale)

    centerOfMass.negate(centerOfMass1)

    val transform = mat4x3ToTransform(
        transform0, localScale,
        centerOfMass1, Transform()
    )
    val shape = createBulletShape(collider, totalScale)
    JomlPools.vec3d.sub(3)

    // there may be extra scale hidden in there
    return transform to shape
}

@JvmField
val defaultShape = BoxShape(Vector3d(1.0, 1.0, 1.0))
