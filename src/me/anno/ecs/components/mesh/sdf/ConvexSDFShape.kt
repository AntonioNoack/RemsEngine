package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.utils.pooling.JomlPools
import kotlin.math.abs

class ConvexSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConvexShape() {

    override fun getAabb(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {
        collider.getAABB(t, aabbMin, aabbMax)
    }

    // might be correct...
    override fun getShapeType(): BroadphaseNativeType {
        return BroadphaseNativeType.CONVEX_SHAPE_PROXYTYPE
    }

    val localScaling = javax.vecmath.Vector3d(1.0, 1.0, 1.0)

    override fun setLocalScaling(scaling: javax.vecmath.Vector3d) {
        localScaling.set(scaling)
    }

    override fun getLocalScaling(out: javax.vecmath.Vector3d): javax.vecmath.Vector3d {
        out.set(localScaling)
        return out
    }

    override fun calculateLocalInertia(mass: Double, inertia: javax.vecmath.Vector3d) {
        collider.calculateLocalInertia(mass, inertia)
    }

    override fun getName() = collider.name
    override fun getMargin() = collider.margin

    override fun setMargin(margin: Double) {
        collider.margin = margin
    }

    override fun localGetSupportingVertex(
        dir: javax.vecmath.Vector3d,
        out: javax.vecmath.Vector3d // = margin * normal
    ) = localGetSupportingVertex(dir, out, margin)

    fun localGetSupportingVertex(
        dir: javax.vecmath.Vector3d,
        out: javax.vecmath.Vector3d,
        margin: Double
    ): javax.vecmath.Vector3d {

        dir.normalize()

        val bounds = sdf.localAABB
        val dir2 = JomlPools.vec3f.create().set(dir.x, dir.y, dir.z)
        val maxDistance =
            (bounds.deltaX() * abs(dir.x) + bounds.deltaY() * abs(dir.y) + bounds.deltaZ() * abs(dir.z)).toFloat()
        val start = JomlPools.vec3f.create().set(dir2).mul(maxDistance)
            .add(bounds.avgX().toFloat(), bounds.avgY().toFloat(), bounds.avgZ().toFloat())
        dir2.mul(-1f)
        val distance = sdf.raycast(
            start, dir2, 0f,
            maxDistance * 2f,
        ) - margin.toFloat()

        start.add(dir2.x * distance, dir2.y * distance, dir2.z * distance)
        out.set(start.x.toDouble(), start.y.toDouble(), start.z.toDouble())

        JomlPools.vec3f.sub(2)
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(
        dir: javax.vecmath.Vector3d,
        out: javax.vecmath.Vector3d
    ) = localGetSupportingVertex(dir, out, 0.0)

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        vectors: Array<out javax.vecmath.Vector3d>,
        supportVerticesOut: Array<out javax.vecmath.Vector3d>,
        numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            localGetSupportingVertex(vectors[i], supportVerticesOut[i], 0.0)
        }
    }

    override fun getAabbSlow(
        t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d
    ) {
        // mmh, not really slow...
        collider.getAABB(t, aabbMin, aabbMax)
    }

    override fun getNumPreferredPenetrationDirections() = 0
    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: javax.vecmath.Vector3d) {
        throw InternalError()
    }

}