package me.anno.sdf.physics

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.linearmath.Transform
import me.anno.maths.Maths
import me.anno.maths.geometry.MarchingCubes
import me.anno.sdf.SDFCollider
import me.anno.sdf.SDFComponent
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

class ConcaveSDFShape(val sdf: SDFComponent, val collider: SDFCollider) : ConcaveShape() {

    companion object {
        private val LOGGER = LogManager.getLogger(ConcaveSDFShape::class)
    }

    override fun getAabb(t: Transform, aabbMin: javax.vecmath.Vector3d, aabbMax: javax.vecmath.Vector3d) {
        collider.getAABB(t, aabbMin, aabbMax)
    }

    // might be correct...
    override fun getShapeType(): BroadphaseNativeType {
        return BroadphaseNativeType.FAST_CONCAVE_MESH_PROXYTYPE
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

    val fx = 6
    val fy = 6
    val fz = 6
    val field = FloatArray(fx * fy * fz)

    val min2 = javax.vecmath.Vector3d()
    val max2 = javax.vecmath.Vector3d()

    private val seeds = IntArrayList(8)

    override fun processAllTriangles(
        callback: TriangleCallback,
        aabbMin: javax.vecmath.Vector3d, // bounds that need to be included
        aabbMax: javax.vecmath.Vector3d
    ) {

        LOGGER.debug("Requesting ConcaveSDFShape.processAllTriangles({}, {})", aabbMin, aabbMax)

        // it looks like we need to meshify our sdf before we can use bullet physics
        // to do dual contouring for a better mesh
        // small walls just are ignored by this method

        // shrink bounds by aabb, so we use a minimal space
        val min2 = min2; min2.set(aabbMin)
        val max2 = max2; max2.set(aabbMax)
        val bounds = sdf.globalAABB
        min2.x = max(min2.x, bounds.minX)
        min2.y = max(min2.y, bounds.minY)
        min2.z = max(min2.z, bounds.minZ)
        max2.x = min(max2.x, bounds.maxX)
        max2.y = min(max2.y, bounds.maxY)
        max2.z = min(max2.z, bounds.maxZ)

        // else not really defined
        if (min2.x <= max2.x && min2.y <= max2.y && min2.z <= max2.z) {

            // sizes could be adjusted to match the "aspect ratio" of the aabb better
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
                            Maths.mix(min2.x, max2.x, xf),
                            Maths.mix(min2.y, max2.y, yf),
                            Maths.mix(min2.z, max2.z, zf), 0.0
                        )
                        field[i++] = sdf.computeSDF(pos, seeds)
                        seeds.clear()
                    }
                }
            }

            JomlPools.vec4f.sub(1)

            val bounds1 = JomlPools.aabbf.create()
            bounds1.setMin(min2.x.toFloat(), min2.y.toFloat(), min2.z.toFloat())
            bounds1.setMax(max2.x.toFloat(), max2.y.toFloat(), max2.z.toFloat())

            val triangle = Array(3) { javax.vecmath.Vector3d() }
            var ctr = 0
            MarchingCubes.march(fx, fy, fz, field, 0f, bounds1, false) { a, b, c ->
                // is the order correct? (front/back sides)
                // or is this ignored by Bullet?
                triangle[0].set(a.x.toDouble(), a.y.toDouble(), a.z.toDouble())
                triangle[1].set(b.x.toDouble(), b.y.toDouble(), b.z.toDouble())
                triangle[2].set(c.x.toDouble(), c.y.toDouble(), c.z.toDouble())
                callback.processTriangle(triangle, 0, ctr++)
            }

            LOGGER.debug("Generated {} triangles", ctr)
        } else LOGGER.debug("Bounds were empty")
    }
}