package me.anno.bullet

import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape
import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.ConvexHullShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
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
import me.anno.ecs.components.physics.Physics.Companion.convertEntityToPhysicsI
import me.anno.maths.geometry.convexhull.ConvexHulls
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

fun MeshCollider.createBulletMeshShape(scale: Vector3f): CollisionShape {

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

        convex.localScaling = scale
        convex.margin = 0f

        val min = Vector3d()
        val max = Vector3d()
        convex.getBounds(Transform(), min, max)

        val maxNumVertices = maxNumVertices
        val numVertices = positions.size / 3
        if (maxNumVertices !in 4..numVertices) {
            convex.margin = margin
            return convex
        }

        /* val directions = ArrayList<Vector3d>(NUM_UNIT_SPHERE_POINTS)
         for (i in constUnitSpherePoints.indices) {
             directions.add(Vector3d(constUnitSpherePoints[i]))
         }

         val tmp = Vector3d()
         for (i in directions.indices) {
             val v = directions[i]
             convex.localGetSupportingVertex(tmp.set(v), v)
         }*/

        val hull = ConvexHulls.calculateConvexHull(positions, maxNumVertices)
        if (hull == null) {
            LOGGER.warn("Failed to create convex hull for ${mesh.ref}")
            convex.margin = margin
            return convex
        }

        val shape = ConvexHullShape(verticesToFloatArray(hull.vertices), hull.triangles)
        shape.margin = margin
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
        if (scale.x == 1f && scale.y == 1f && scale.z == 1f) {
            fb.put(positions)
        } else {
            val tmp = Vector3f()
            forLoop(0, positions.size - 2, 3) { i ->
                tmp.set(positions[i], positions[i + 1], positions[i + 2])
                fb.put(tmp.x * scale.x)
                fb.put(tmp.y * scale.y)
                fb.put(tmp.z * scale.z)
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
        shape.margin = margin
        return shape
    }
}

fun CapsuleCollider.createBulletCapsuleShape(scale: Vector3f): CapsuleShape {
    return when (axis) {
        Axis.X -> CapsuleShape(radius * scale.y, halfHeight * scale.x * 2f, axis) // x
        Axis.Y -> CapsuleShape(radius * scale.x, halfHeight * scale.y * 2f, axis) // y
        Axis.Z -> CapsuleShape(radius * scale.x, halfHeight * scale.z * 2f, axis) // z
    }.apply { margin = roundness }
}

fun ConeCollider.createBulletConeShape(scale: Vector3f): ConeShape {
    val axis = axis
    return when (axis) {
        Axis.X -> ConeShape(radius * scale.y, height * scale.x, axis)
        Axis.Y -> ConeShape(radius * scale.x, height * scale.y, axis)
        Axis.Z -> ConeShape(radius * scale.x, height * scale.z, axis)
    }.apply { margin = roundness }
}

fun CylinderCollider.createBulletCylinderShape(scale: Vector3f): CylinderShape {
    val axis = axis
    return when (axis) {
        Axis.X -> CylinderShape(Vector3f(halfHeight * scale.x, radius * scale.y, radius * scale.z), axis)
        Axis.Y -> CylinderShape(Vector3f(radius * scale.x, halfHeight * scale.y, radius * scale.z), axis)
        Axis.Z -> CylinderShape(Vector3f(radius * scale.x, radius * scale.y, halfHeight * scale.z), axis)
    }.apply { margin = roundness }
}

fun BoxCollider.createBulletBoxShape(scale: Vector3f): BoxShape {
    return BoxShape(
        Vector3f(
            halfExtents.x * scale.x,
            halfExtents.y * scale.y,
            halfExtents.z * scale.z
        )
    ).apply { margin = roundness * scale.absMax() }
}

fun SphereCollider.createBulletSphereShape(scale: Vector3f): SphereShape {
    val maxScale = scale.absMax()
    return SphereShape(radius * maxScale).apply {
        if (maxScale > 0f) {
            localScaling = Vector3f(scale).div(maxScale)
        } // else everything is 0 anyway
    }
}

fun createBulletShape(collider: Collider, scale: Vector3f): CollisionShape {
    return when (collider) {
        is MeshCollider -> collider.createBulletMeshShape(scale)
        is CapsuleCollider -> collider.createBulletCapsuleShape(scale)
        is ConeCollider -> collider.createBulletConeShape(scale)
        is ConvexCollider -> {
            val shape = ConvexHullShape(collider.points!!, null)
            shape.localScaling = scale
            shape.margin = collider.roundness * scale.absMax()
            shape
        }
        is CylinderCollider -> collider.createBulletCylinderShape(scale)
        // is CircleCollider -> SphereShape(collider.radius * scale.dot(0.33, 0.34, 0.33))
        is SphereCollider -> collider.createBulletSphereShape(scale)
        is BoxCollider -> collider.createBulletBoxShape(scale)
        is InfinitePlaneCollider -> StaticPlaneShape(Vector3f(0.0, 1.0, 0.0), 0.0)
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
    bodyScale: Vector3f, centerOfMass: Vector3d
): Pair<Transform, CollisionShape> {
    val transform0 = collider.entity!!.fromLocalToOtherLocal(base)

    val localScale = JomlPools.vec3f.create()
    val totalScale = JomlPools.vec3f.create()
    val localCenterOfMass = JomlPools.vec3d.create()

    transform0.getScale(localScale)
    bodyScale.mul(localScale, totalScale)

    centerOfMass.negate(localCenterOfMass)

    val transform = Transform()
    val rot = JomlPools.quat4f.create()
    convertEntityToPhysicsI(
        transform0, transform.origin, rot,
        localScale, localCenterOfMass,
    )
    transform.basis.set(rot)

    val shape = createBulletShape(collider, totalScale)
    JomlPools.vec3f.sub(2)
    JomlPools.vec3d.sub(1)
    JomlPools.quat4f.sub(1)

    // there may be extra scale hidden in there
    return transform to shape
}

@JvmField
val defaultShape = BoxShape(Vector3f(1f))
