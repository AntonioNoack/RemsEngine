package me.anno.sdf.physics

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.sdf.SDFCollider
import me.anno.sdf.SDFComponent
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.abs

class ConvexSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConvexShape() {

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        collider.getAABB(t, aabbMin, aabbMax)
    }

    // might be correct...
    override val shapeType get(): BroadphaseNativeType {
        return BroadphaseNativeType.CONVEX
    }

    var maxSteps = 10

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        return collider.calculateLocalInertia(mass, inertia)
    }

    override var margin: Float
        get() = collider.margin
        set(value) {
            collider.margin = value
        }

    override fun localGetSupportingVertex(
        dir: Vector3f,
        out: Vector3f // = margin * normal
    ) = localGetSupportingVertex(dir, out, margin)

    private val seeds = IntArrayList(8)

    /**
     * return the closest point to that
     * */
    fun localGetSupportingVertex(
        pos: Vector3f,
        out: Vector3f,
        margin: Float
    ): Vector3f {

        val bounds = sdf.localAABB
        val dir2 = JomlPools.vec3f.create().set(pos.x, pos.y, pos.z).normalize()
        val maxDistance =
            (bounds.deltaX * abs(dir2.x) + bounds.deltaY * abs(dir2.y) + bounds.deltaZ * abs(dir2.z)).toFloat()

        val start = JomlPools.vec3f.create().set(dir2).mul(maxDistance)
            .add(bounds.centerX.toFloat(), bounds.centerY.toFloat(), bounds.centerZ.toFloat())
        dir2.negate()

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

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f =
        localGetSupportingVertex(dir, out, 0f)

    override fun getAabbSlow(
        t: Transform, aabbMin: Vector3d, aabbMax: Vector3d
    ) {
        // mmh, not really slow...
        collider.getAABB(t, aabbMin, aabbMax)
    }

    override val numPreferredPenetrationDirections get() = 0
    override fun getPreferredPenetrationDirection(index: Int, penetrationVector: Vector3f) {
        throw InternalError()
    }

}