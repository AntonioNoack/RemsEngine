package me.anno.ecs.components.mesh.sdf

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import me.anno.maths.Maths.mix
import me.anno.maths.geometry.MarchingCubes
import me.anno.utils.pooling.JomlPools
import kotlin.math.max
import kotlin.math.min

class ConcaveSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConcaveShape() {

    private var margin = BulletGlobals.CONVEX_DISTANCE_MARGIN

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
    override fun getMargin() = this.margin

    override fun setMargin(margin: Double) {
        this.margin = margin
    }

    override fun processAllTriangles(
        callback: TriangleCallback, aabbMin: javax.vecmath.Vector3d, // bounds that need to be included
        aabbMax: javax.vecmath.Vector3d
    ) {

        // it looks like we need to meshify our sdf before we can use bullet physics
        // to do dual contouring for a better mesh
        // small walls just are ignored by this method

        // shrink bounds by aabb, so we use a minimal space
        val min2 = javax.vecmath.Vector3d(aabbMin)
        val max2 = javax.vecmath.Vector3d(aabbMax)
        val bounds = sdf.globalAABB
        min2.x = max(min2.x, bounds.minX)
        min2.y = max(min2.y, bounds.minY)
        min2.z = max(min2.z, bounds.minZ)
        max2.x = min(max2.x, bounds.maxX)
        max2.y = min(max2.y, bounds.maxY)
        max2.z = min(max2.z, bounds.maxZ)

        // else not really defined
        if (min2.x < max2.x && min2.y < max2.y && min2.z < max2.z) {
            // sizes could be adjusted to match the "aspect ratio" of the aabb better
            val fx = 6
            val fy = 6
            val fz = 6
            val field = FloatArray(fx * fy * fz)
            var i = 0
            val pos = JomlPools.vec4f.create()
            val ffx = 1.0 / (fx - 1.0)
            val ffy = 1.0 / (fy - 1.0)
            val ffz = 1.0 / (fz - 1.0)
            for (z in 0 until fz) {
                val zf = z * ffz
                for (y in 0 until fy) {
                    val yf = y * ffy
                    for (x in 0 until fx) {
                        val xf = x * ffx
                        pos.set(
                            mix(min2.x, max2.x, xf), mix(min2.y, max2.y, yf), mix(min2.z, max2.z, zf), 0.0
                        )
                        field[i++] = sdf.computeSDF(pos)
                    }
                }
            }

            val triangle = Array(3) { javax.vecmath.Vector3d() }
            MarchingCubes.march(fx, fy, fz, field, 0f, false) { a, b, c ->
                // is the order correct? (front/back sides)
                // or is this ignored by Bullet?
                triangle[0].set(a.x.toDouble(), a.y.toDouble(), a.z.toDouble())
                triangle[1].set(b.x.toDouble(), b.y.toDouble(), b.z.toDouble())
                triangle[2].set(c.x.toDouble(), c.y.toDouble(), c.z.toDouble())
                callback.processTriangle(triangle, 0, 0)
            }
        }
    }
}