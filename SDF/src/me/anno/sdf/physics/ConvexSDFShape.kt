package me.anno.sdf.physics

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.sdf.SDFCollider
import me.anno.sdf.SDFComponent
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import javax.vecmath.Vector3d
import kotlin.math.abs

class ConvexSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConvexShape() {

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        collider.getAABB(t, aabbMin, aabbMax)
    }

    // might be correct...
    override val shapeType get(): BroadphaseNativeType {
        return BroadphaseNativeType.CONVEX_SHAPE_PROXYTYPE
    }

    var maxSteps = 10
    val localScaling = Vector3d(1.0, 1.0, 1.0)

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        out.set(localScaling)
        return out
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        collider.calculateLocalInertia(mass, inertia)
    }

    override var margin: Double
        get() = collider.margin.toDouble()
        set(value) {
            collider.margin = value.toFloat()
        }

    override fun localGetSupportingVertex(
        dir: Vector3d,
        out: Vector3d // = margin * normal
    ) = localGetSupportingVertex(dir, out, margin)

    private val seeds = IntArrayList(8)

    /**
     * return the closest point to that
     * */
    fun localGetSupportingVertex(
        pos: Vector3d,
        out: Vector3d,
        margin: Double
    ): Vector3d {

        val bounds = sdf.localAABB
        val dir2 = JomlPools.vec3f.create().set(pos.x, pos.y, pos.z).normalize()
        val maxDistance =
            (bounds.deltaX * abs(dir2.x) + bounds.deltaY * abs(dir2.y) + bounds.deltaZ * abs(dir2.z)).toFloat()

        val start = JomlPools.vec3f.create().set(dir2).mul(maxDistance)
            .add(bounds.centerX.toFloat(), bounds.centerY.toFloat(), bounds.centerZ.toFloat())
        dir2.mul(-1f)

        val distance = sdf.raycast(
            start, dir2, 0f,
            maxDistance * 2f,
            maxSteps, seeds,
        ) - margin.toFloat()

        val hit = dir2.mulAdd(distance, start, start)
        out.set(hit.x.toDouble(), hit.y.toDouble(), hit.z.toDouble())
        JomlPools.vec3f.sub(2)
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d) =
        localGetSupportingVertex(dir, out, 0.0)

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        vectors: Array<Vector3d>, supportVerticesOut: Array<Vector3d>, numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            localGetSupportingVertex(vectors[i], supportVerticesOut[i], 0.0)
        }
    }

    override fun getAabbSlow(
        t: Transform, aabbMin: Vector3d, aabbMax: Vector3d
    ) {
        // mmh, not really slow...
        collider.getAABB(t, aabbMin, aabbMax)
    }

    override val numPreferredPenetrationDirections get() = 0
    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3d) {
        throw InternalError()
    }

}