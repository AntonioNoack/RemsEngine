package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.utils.LOGGER
import me.anno.utils.pooling.JomlPools
import kotlin.math.max
import kotlin.math.min

class ConvexSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConvexShape() {

    private var margin = BulletGlobals.CONVEX_DISTANCE_MARGIN

    override fun getAabb(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {
        collider.getAABB(t, aabbMin, aabbMax)
    }

    // might be correct...
    override fun getShapeType(): BroadphaseNativeType {
        return BroadphaseNativeType.TERRAIN_SHAPE_PROXYTYPE
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
    override fun getMargin() = this.margin

    override fun setMargin(margin: Double) {
        this.margin = margin
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

        // this shape is convex, and the center is supposed to be central, so we need to query the first intersection from outside

        LOGGER.debug("localGetSupportingVertex($dir)")

        val bounds = sdf.localAABB
        val maxDistance =
            max(
                dir.x / (if (dir.x < 0f) bounds.minX else bounds.maxX),
                max(
                    dir.y / (if (dir.y < 0f) bounds.minY else bounds.maxY),
                    dir.z / (if (dir.z < 0f) bounds.minZ else bounds.maxZ)
                )
            ).toFloat()
        val start = JomlPools.vec3f.create()
            .set(dir.x, dir.y, dir.z).mul(maxDistance)
        val dir2 = JomlPools.vec3f.create()
            .set(dir.x, dir.y, dir.z)

        // todo how accurate do we need to be?
        val distance = maxDistance - min(sdf.raycast(start, dir2, 0f, maxDistance), maxDistance) + margin

        out.set(dir)
        out.scale(distance)

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
        val hit = JomlPools.vec3f.create()
        val normal = JomlPools.vec3f.create()
        val pos = JomlPools.vec4f.create()
        for (i in 0 until numVectors) {
            val vec = vectors[i]
            val out = supportVerticesOut[i]
            pos.set(vec.x, vec.y, vec.z, 0.0)
            hit.set(vec.x, vec.y, vec.z)
            sdf.calcNormal(hit, normal)
            val distance = sdf.computeSDF(pos).toDouble()
            out.set(normal.x * distance, normal.y * distance, normal.z * distance)
        }
        JomlPools.vec3f.sub(2)
        JomlPools.vec4f.sub(1)
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