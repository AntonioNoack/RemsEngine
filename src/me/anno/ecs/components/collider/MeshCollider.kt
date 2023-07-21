package me.anno.ecs.components.collider

import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles
import me.anno.utils.types.Triangles.thirdF
import org.joml.*
import kotlin.math.sqrt

open class MeshCollider() : Collider() {

    constructor(src: FileReference) : this() {
        meshFile = src
    }

    @Suppress("unused")
    constructor(src: Mesh) : this(src.ref)

    @SerializedProperty
    override var isConvex = true

    @SerializedProperty
    var enableSimplifications = true

    @Type("Mesh/PrefabSaveable")
    var mesh: Mesh? = null
        get() {
            if (field == null) field = MeshCache[meshFile]
            if (field == null) field = entity?.getComponentInChildren(MeshComponentBase::class, false)?.getMesh()
            return field
        }

    val meshTransform = Matrix4x3f()

    @DebugProperty
    val meshTriangles get() = mesh?.numPrimitives

    @DebugProperty
    val meshAABB: AABBf?
        get() {
            val mesh = mesh
            mesh?.getBounds()
            return mesh?.aabb
        }

    @Type("MeshComponent/Reference")
    var meshFile: FileReference = InvalidRef
        set(value) {
            field = value
            mesh = MeshCache[value] ?: mesh
        }

    var isValid = false

    /**
     * returns +Inf, if not
     * returns the distance, if it hit
     *
     * also sets the surface normal
     * */
    override fun raycast(
        start: Vector3f, direction: Vector3f, radiusAtOrigin: Float, radiusPerUnit: Float,
        surfaceNormal: Vector3f?, maxDistance: Float
    ): Float {

        val mesh = mesh ?: return Float.POSITIVE_INFINITY
        if (!mesh.getBounds().testLine(start, direction))
            return Float.POSITIVE_INFINITY

        // test whether we intersect any triangle of this mesh
        var bestDistance = maxDistance
        val tmpPos = JomlPools.vec3f.create()
        val tmpNor = JomlPools.vec3f.create()

        val ai = JomlPools.vec3f.create()
        val bi = JomlPools.vec3f.create()
        val ci = JomlPools.vec3f.create()
        val mid = JomlPools.vec3f.create()
        val scaleUp = -0.001f // against small inaccuracies
        var neg = false
        val meshTransform = meshTransform
        mesh.forEachTriangle(ai, bi, ci) { a, b, c ->
            // make the triangle slightly larger than it is
            meshTransform.transformPosition(a)
            meshTransform.transformPosition(b)
            meshTransform.transformPosition(c)
            mid.set(a).add(b).add(c).mul(thirdF)
            a.lerp(mid, scaleUp)
            b.lerp(mid, scaleUp)
            c.lerp(mid, scaleUp)
            // check collision of localStart-localEnd with triangle a,b,c
            val localDistance = Triangles.rayTriangleIntersection(
                start, direction, a, b, c,
                radiusAtOrigin, radiusPerUnit,
                bestDistance, tmpPos, tmpNor
            )
            if (localDistance < bestDistance) {
                bestDistance = localDistance
                neg = tmpNor.dot(direction) > 0f
                surfaceNormal?.set(tmpNor)
            }
        }

        JomlPools.vec3f.sub(6)

        return if (neg) -bestDistance else bestDistance

    }

    /**
     * calculates ((b-a) x c) * (p-a)
     * without any allocations
     * */
    private fun complexMaths1(a: Vector3f, b: Vector3f, c: Vector3f, p: Vector3f): Int {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = c.x
        val y1 = c.y
        val z1 = c.z
        val rx = y0 * z1 - y1 * z0
        val ry = z0 * x1 - x0 * z1
        val rz = x0 * y1 - y0 * x1
        val v = rx * (p.x - a.x) + ry * (p.y - a.y) + rz * (p.z - a.z)
        return if (v >= 0f) +1 else -1
    }

    /**
     * calculates r=clamp(dot(ba,pa)/dot2(ba),0.0,1.0), dot2(ba*r-pa)
     * without any allocations
     * */
    private fun complexMaths2(a: Vector3f, b: Vector3f, p: Vector3f): Float {
        val x0 = b.x - a.x
        val y0 = b.y - a.y
        val z0 = b.z - a.z
        val x1 = p.x - a.x
        val y1 = p.y - a.y
        val z1 = p.z - a.z
        val bpa = x0 * x1 + y0 * y1 + z0 * z1
        val ba2 = x0 * x0 + y0 * y0 + z0 * z0
        val r = clamp(bpa / max(ba2, 1e-38f))
        val dx = x0 * r - x1
        val dy = y0 * r - y1
        val dz = z0 * r - z1
        return dx * dx + dy * dy + dz * dz
    }

    /*override fun getSignedDistance(deltaPos: Vector3f, outNormal: Vector3f): Float {
        val tmp = JomlPools.vec3f.create()
        val distance = getSignedDistance(deltaPos, tmp)
        JomlPools.vec3f.sub(1)
        return distance
    }*/

    override fun getSignedDistance(deltaPos: Vector3f): Float {
        // https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm
        // combine the sdfs of all faces :3 (will be slow, but might be beautiful :3)
        val mesh = mesh ?: return Float.POSITIVE_INFINITY
        val nor = JomlPools.vec3f.create()
        val ba = JomlPools.vec3f.create()
        val ac = JomlPools.vec3f.create()
        val pa = JomlPools.vec3f.create()
        var best = Float.POSITIVE_INFINITY
        var neg = false // only works for convex shapes with center at zero
        val meshTransform = meshTransform
        mesh.forEachTriangle { a, b, c ->
            meshTransform.transformPosition(a)
            meshTransform.transformPosition(b)
            meshTransform.transformPosition(c)
            ac.set(a).sub(c)
            ba.set(b).sub(a)
            nor.set(ba).cross(ac)
            val sgn = complexMaths1(a, b, nor, deltaPos) +
                    complexMaths1(b, c, nor, deltaPos) +
                    complexMaths1(c, a, nor, deltaPos)
            val dsq = if (sgn < 2) {
                val ta = complexMaths2(a, b, deltaPos)
                val tb = complexMaths2(b, c, deltaPos)
                val tc = complexMaths2(c, a, deltaPos)
                min(ta, min(tb, tc))
            } else {
                pa.set(deltaPos).sub(a)
                sq(nor.dot(pa)) / nor.lengthSquared()
            }
            if (dsq < best) {
                best = dsq
                // make inner sides negative
                neg = deltaPos.dot(nor) - a.dot(nor) > 0f
            }
        }
        JomlPools.vec3f.sub(4)
        val distance = sqrt(best)
        return if (neg) -distance else +distance
    }

    override fun drawShape() {
        val mesh = mesh ?: return
        val tr = meshTransform
        if (tr.isIdentity()) {
            mesh.forEachTriangle { a, b, c ->
                val color = -1
                drawLine(entity, a, b, color)
                drawLine(entity, b, c, color)
                drawLine(entity, c, a, color)
            }
        } else {
            mesh.forEachTriangle { a, b, c ->
                tr.transformPosition(a)
                tr.transformPosition(b)
                tr.transformPosition(c)
                val color = -1
                drawLine(entity, a, b, color)
                drawLine(entity, b, c, color)
                drawLine(entity, c, a, color)
            }
        }
    }

    override fun union(globalTransform: Matrix4x3d, aabb: AABBd, tmp: Vector3d, preferExact: Boolean) {
        val mesh = mesh
        if (mesh != null) {
            val mat = JomlPools.mat4x3d.borrow()
            mat.set(globalTransform)
            mat.mul(meshTransform)
            mesh.forEachPoint(preferExact) { x, y, z ->
                tmp.set(x.toDouble(), y.toDouble(), z.toDouble())
                mat.transformPosition(tmp)
                aabb.union(tmp)
            }
        } else super.union(globalTransform, aabb, tmp, preferExact)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as MeshCollider
        // todo getInClone returns an error for thumbnail test on
        // getReference("Downloads/up/PolygonSciFiCity_Unity_Project_2017_4.unitypackage/f9a80be48a6254344b5f885cfff4bbb0/64472554668277586.json")
        dst.mesh = mesh // getInClone(mesh, clone)
        dst.meshFile = meshFile
        dst.isConvex = isConvex
        dst.meshTransform.set(meshTransform)
    }

    override val className: String get() = "MeshCollider"

}