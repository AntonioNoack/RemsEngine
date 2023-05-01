package me.anno.ecs.components.bullet

import com.bulletphysics.collision.shapes.*
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.*
import me.anno.ecs.components.collider.twod.CircleCollider
import me.anno.ecs.components.collider.twod.RectCollider
import me.anno.ecs.components.mesh.sdf.SDFCollider
import me.anno.ecs.components.bullet.BulletPhysics.Companion.mat4x3ToTransform
import me.anno.ecs.components.bullet.sdf.ConcaveSDFShape
import me.anno.ecs.components.bullet.sdf.ConvexSDFShape
import me.anno.utils.LOGGER
import me.anno.utils.types.Matrices.isIdentity
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min


fun MeshCollider.createBulletShape(scale: Vector3d): CollisionShape {

    val mesh = mesh ?: return defaultShape

    val positions = mesh.positions
    if (positions == null) {
        isValid = false
        return defaultShape
    }

    isValid = true

    val indices = mesh.indices

    val meshTransform = meshTransform
    Stack.reset(false)

    if (isConvex) {

        // calculate convex hull
        // simplify it maybe
        val convex =
            if (scale.x in 0.99..1.01 && scale.y in 0.99..1.01 && scale.z in 0.99..1.01 && meshTransform.isIdentity()) {
                ConvexHullShape3(positions)
            } else {
                val tmp = Vector3f()
                val points = ArrayList<javax.vecmath.Vector3d>(positions.size / 3)
                for (i in positions.indices step 3) {
                    tmp.set(positions[i], positions[i + 1], positions[i + 2])
                    meshTransform.transformPosition(tmp)
                    val x = tmp.x * scale.x
                    val y = tmp.y * scale.y
                    val z = tmp.z * scale.z
                    points.add(javax.vecmath.Vector3d(x, y, z))
                }
                ConvexHullShape(points)
            }
        if (positions.size < 30 || !enableSimplifications) return convex

        val hull = ShapeHull(convex)
        hull.buildHull(convex.margin)
        return ConvexHullShape(hull.vertexPointer)

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
        if (scale.x == 1.0 && scale.y == 1.0 && scale.z == 1.0 && meshTransform.isIdentity()) {
            fb.put(positions)
        } else {
            val sx = scale.x.toFloat()
            val sy = scale.y.toFloat()
            val sz = scale.z.toFloat()
            val tmp = Vector3f()
            for (i in positions.indices step 3) {
                tmp.set(positions[i], positions[i + 1], positions[i + 2])
                meshTransform.transformPosition(tmp)
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
        return BvhTriangleMeshShape(smi, true, true)
    }
}

fun CapsuleCollider.createBulletShape(scale: Vector3d): CollisionShape {
    return when (axis) {
        0 -> CapsuleShape(radius * scale.y, halfHeight * scale.x * 2.0, axis) // x
        1 -> CapsuleShape(radius * scale.x, halfHeight * scale.y * 2.0, axis) // y
        2 -> CapsuleShape(radius * scale.x, halfHeight * scale.z * 2.0, axis) // z
        else -> throw RuntimeException()
    }
}

fun ConeCollider.createBulletShape(scale: Vector3d): CollisionShape {
    return when (axis) {
        0 -> ConeShapeX(radius * scale.y, height * scale.x)
        2 -> ConeShapeZ(radius * scale.x, height * scale.z)
        else -> ConeShape(radius * scale.x, height * scale.y)
    }
}

fun CylinderCollider.createBulletShape(scale: Vector3d): CollisionShape {
    return when (axis) {
        0 -> CylinderShapeX(javax.vecmath.Vector3d(halfHeight * scale.x, radius * scale.y, radius * scale.z))
        1 -> CylinderShape(javax.vecmath.Vector3d(radius * scale.x, halfHeight * scale.y, radius * scale.z))
        2 -> CylinderShapeZ(javax.vecmath.Vector3d(radius * scale.x, radius * scale.y, halfHeight * scale.z))
        else -> throw RuntimeException()
    }
}

fun BoxCollider.createBulletShape(scale: Vector3d): CollisionShape {
    val shape = BoxShape(
        javax.vecmath.Vector3d(
            halfExtends.x * scale.x,
            halfExtends.y * scale.y,
            halfExtends.z * scale.z
        )
    )
    shape.margin = margin
    return shape
}

fun SDFCollider.createBulletShape(scale: Vector3d): CollisionShape {
    // todo scale is missing...
    val sdf = sdf ?: return defaultShape
    return if (isConvex) {
        ConvexSDFShape(sdf, this)
    } else {
        ConcaveSDFShape(sdf, this)
    }
}

fun createBulletShape(collider: Collider, scale: Vector3d): CollisionShape {
    return when (collider) {
        is MeshCollider -> collider.createBulletShape(scale)
        is CapsuleCollider -> collider.createBulletShape(scale)
        is ConeCollider -> collider.createBulletShape(scale)
        is ConvexCollider -> ConvexHullShape3(collider.points!!)
        is CylinderCollider -> collider.createBulletShape(scale)
        is CircleCollider -> SphereShape(collider.radius * scale.dot(0.33, 0.34, 0.33))
        is SphereCollider -> SphereShape(collider.radius * scale.dot(0.33, 0.34, 0.33))
        is BoxCollider -> collider.createBulletShape(scale)
        is RectCollider -> {
            val halfExtends = collider.halfExtends
            return BoxShape(
                javax.vecmath.Vector3d(
                    halfExtends.x * scale.x,
                    halfExtends.y * scale.y,
                    scale.z
                )
            )
        }
        is SDFCollider -> collider.createBulletShape(scale)
        else -> defaultShape
    }
}

fun createBulletCollider(collider: Collider, base: Entity, scale: Vector3d): Pair<Transform, CollisionShape> {
    val transform0 = collider.entity!!.fromLocalToOtherLocal(base)
    // there may be extra scale hidden in there
    val extraScale = transform0.getScale(Vector3d())
    val totalScale = Vector3d(scale).mul(extraScale)
    val shape = createBulletShape(collider, totalScale)
    val transform = mat4x3ToTransform(transform0, extraScale)
    return transform to shape
}

@JvmField
val defaultShape = BoxShape(javax.vecmath.Vector3d(1.0, 1.0, 1.0))

fun SDFCollider.getAABB(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {

    val sdf = sdf ?: return
    val bounds = sdf.globalAABB

    // if t is just scaling + translation, we could simplify this

    aabbMin.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    aabbMax.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)

    val tmp = Stack.newVec()
    val basis = t.basis
    for (i in 0 until 8) {
        tmp.set(
            if (i.and(1) != 0) bounds.minX else bounds.maxX,
            if (i.and(2) != 0) bounds.minY else bounds.maxY,
            if (i.and(4) != 0) bounds.minZ else bounds.maxZ
        )
        basis.transform(tmp)
        aabbMin.x = min(aabbMin.x, tmp.x)
        aabbMin.y = min(aabbMin.y, tmp.y)
        aabbMin.z = min(aabbMin.z, tmp.z)
        aabbMax.x = max(aabbMax.x, tmp.x)
        aabbMax.y = max(aabbMax.y, tmp.y)
        aabbMax.z = max(aabbMax.z, tmp.z)
    }

    aabbMin.add(t.origin)
    aabbMax.add(t.origin)
    Stack.subVec(1)
}
