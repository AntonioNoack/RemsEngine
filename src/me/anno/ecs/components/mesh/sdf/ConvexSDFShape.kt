package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.utils.pooling.JomlPools

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
        vec: javax.vecmath.Vector3d, out: javax.vecmath.Vector3d // = pos + margin * normal
    ): javax.vecmath.Vector3d {
        // to do: do we need an exact solution here?
        // this currently is just a quick guess
        val hit = JomlPools.vec3f.create()
        val normal = JomlPools.vec3f.create()
        val pos = JomlPools.vec4f.create()
        pos.set(vec.x, vec.y, vec.z, 0.0)
        hit.set(vec.x, vec.y, vec.z)
        sdf.calcNormal(hit, normal)
        val distance = sdf.computeSDF(pos) + margin
        out.set(normal.x * distance, normal.y * distance, normal.z * distance)
        JomlPools.vec3f.sub(2)
        JomlPools.vec4f.sub(1)
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(
        vec: javax.vecmath.Vector3d, out: javax.vecmath.Vector3d
    ): javax.vecmath.Vector3d {
        // to do: do we need an exact solution here?
        // this currently is just a quick guess
        val hit = JomlPools.vec3f.create()
        val normal = JomlPools.vec3f.create()
        val pos = JomlPools.vec4f.create()
        pos.set(vec.x, vec.y, vec.z, 0.0)
        hit.set(vec.x, vec.y, vec.z)
        sdf.calcNormal(hit, normal)
        val distance = sdf.computeSDF(pos).toDouble()
        out.set(normal.x * distance, normal.y * distance, normal.z * distance)
        JomlPools.vec3f.sub(2)
        JomlPools.vec4f.sub(1)
        return out
    }

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