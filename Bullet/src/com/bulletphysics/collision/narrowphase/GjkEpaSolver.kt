package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.ObjectStackList
import cz.advel.stack.Stack
import me.anno.maths.Maths.TAUf
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.max

/**
 * GjkEpaSolver contributed under zlib by Nathanael Presson, Nov. 2006.
 *
 * @author jezek2
 */
class GjkEpaSolver {

    val stackMkv = ObjectStackList<Ray>(Ray::class.java)
    val stackHe = ObjectStackList<Vec3Node>(Vec3Node::class.java)
    val stackFace = ObjectStackList<Face>(Face::class.java)

    fun pushStack() {
        stackMkv.push()
        stackHe.push()
        stackFace.push()
    }

    fun popStack() {
        stackMkv.pop()
        stackHe.pop()
        stackFace.pop()
    }

    enum class ResultsStatus {
        /** Shapes don't penetrate **/
        SEPARATED,

        /** Shapes are penetrating **/
        PENETRATING,

        /** GJK phase fail, no big issue, shapes are probably just 'touching' **/
        GJK_FAILED,

        /** EPA phase fail, bigger problem, need to save parameters, and debug **/
        EPA_FAILED,
    }

    class Results {
        var status: ResultsStatus? = null
        val witness0 = Vector3d()
        val witness1 = Vector3d()
        val normal = Vector3f()
        var depth = 0f
        var epaIterations = 0
        var gjkIterations = 0
    }

    class Ray {
        val origin = Vector3d() // Minkowski vertex
        val direction = Vector3f() // Ray

        fun set(m: Ray) {
            origin.set(m.origin)
            direction.set(m.direction)
        }
    }

    class Vec3Node {
        val v: Vector3d = Vector3d()
        var n: Vec3Node? = null
    }

    inner class GJK {

        //public btStackAlloc sa;
        //public Block sablock;

        val table = arrayOfNulls<Vec3Node>(GJK_HASH_SIZE)

        lateinit var transform0: Transform
        lateinit var transform1: Transform

        lateinit var shape0: ConvexShape
        lateinit var shape1: ConvexShape

        val simplex = Array(5) { Ray() }
        val rayDirection = Vector3f()

        /*unsigned*/var order: Int = 0
        /*unsigned*/var iterations: Int = 0
        var margin: Double = 0.0
        var failed: Boolean = false

        fun init(
            transform0: Transform, shape0: ConvexShape,
            transform1: Transform, shape1: ConvexShape,
            margin: Double
        ) {
            pushStack()
            this.transform0 = transform0
            this.transform1 = transform1
            this.shape0 = shape0
            this.shape1 = shape1
            this.margin = margin
            failed = false
        }

        fun destroy() {
            popStack()
        }

        // vdh: very dummy hash
        fun hash(v: Vector3f): Int {
            val h = (v.x * 15461).toInt() xor (v.y * 83003).toInt() xor (v.z * 15473).toInt()
            return (h * 169639) and GJK_HASH_MASK
        }

        fun localSupport(globalDir: Vector3f, i: Int, out: Vector3d): Vector3d {
            val dir = Stack.newVec3f()
            val transform = if (i == 0) transform0 else transform1
            val rotation = transform.basis
            rotation.transformTranspose(globalDir, dir)

            val shape = if (i == 0) shape0 else shape1
            val out0 = JomlPools.vec3f.create()
            shape.localGetSupportingVertex(dir, out0)
            rotation.transform(out0)
            out.set(out0).add(transform.origin)
            Stack.subVec3f(1)
            return out
        }

        fun support(direction: Vector3f, ray: Ray) {
            ray.direction.set(direction)

            val tmp1 = localSupport(direction, 0, Stack.newVec3d())

            val tmp = Stack.newVec3f()
            tmp.set(direction).negate()
            val tmp2 = localSupport(tmp, 1, Stack.newVec3d())

            tmp1.sub(tmp2, ray.origin)
            ray.origin.fma(margin, direction)
            Stack.subVec3f(1)
            Stack.subVec3d(2)
        }

        fun fetchSupport(): Boolean {
            val h = hash(rayDirection)
            var e = table[h]
            while (e != null) {
                if (e.v == rayDirection) {
                    --order
                    return false
                } else {
                    e = e.n
                }
            }
            e = stackHe.get()
            e.v.set(rayDirection)
            e.n = table[h]
            table[h] = e
            support(rayDirection, simplex[++order])
            return (rayDirection.dot(simplex[order].origin) > 0)
        }

        fun solveSimplex2(ao: Vector3d, ab: Vector3d): Boolean {
            if (ab.dot(ao) >= 0) {
                val area = Stack.borrowVec3d()
                ab.cross(ao, area)
                if (area.lengthSquared() > GJK_SQ_IN_SIMPLEX_EPSILON) {
                    rayDirection.set(area.cross(ab))
                } else {
                    return true
                }
            } else {
                order = 0
                simplex[0].set(simplex[1])
                rayDirection.set(ao)
            }
            return false
        }

        fun solveSimplex3(ao: Vector3d, ab: Vector3d, ac: Vector3d): Boolean {
            val tmp = Stack.newVec3d()
            ab.cross(ac, tmp)
            val result = solveSimplex3a(ao, ab, ac, tmp)
            Stack.subVec3d(1)
            return result
        }

        fun solveSimplex3a(ao: Vector3d, ab: Vector3d, ac: Vector3d, cabc: Vector3d): Boolean {
            // TODO: optimize

            val tmp = Stack.newVec3d()
            cabc.cross(ab, tmp)

            val tmp2 = Stack.newVec3d()
            cabc.cross(ac, tmp2)

            val result: Boolean
            if (tmp.dot(ao) < -GJK_IN_SIMPLEX_EPSILON) {
                order = 1
                simplex[0].set(simplex[1])
                simplex[1].set(simplex[2])
                result = solveSimplex2(ao, ab)
            } else if (tmp2.dot(ao) > +GJK_IN_SIMPLEX_EPSILON) {
                order = 1
                simplex[1].set(simplex[2])
                result = solveSimplex2(ao, ac)
            } else {
                val d = cabc.dot(ao)
                if (abs(d) > GJK_IN_SIMPLEX_EPSILON) {
                    if (d > 0) {
                        rayDirection.set(cabc)
                    } else {
                        rayDirection.set(cabc).negate()

                        val swapTmp = Ray()
                        swapTmp.set(simplex[0])
                        simplex[0].set(simplex[1])
                        simplex[1].set(swapTmp)
                    }
                    result = false
                } else {
                    result = true
                }
            }
            Stack.subVec3d(2)
            return result
        }

        fun solveSimplex4(ao: Vector3d, ab: Vector3d, ac: Vector3d, ad: Vector3d): Boolean {
            // TODO: optimize

            val abc = Stack.newVec3d()
            ab.cross(ac, abc)

            val acd = Stack.newVec3d()
            ac.cross(ad, acd)

            val adb = Stack.newVec3d()
            ad.cross(ab, adb)

            val result: Boolean
            if (abc.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                order = 2
                simplex[0].set(simplex[1])
                simplex[1].set(simplex[2])
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ab, ac, abc)
            } else if (acd.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                order = 2
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ac, ad, acd)
            } else if (adb.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                order = 2
                simplex[1].set(simplex[0])
                simplex[0].set(simplex[2])
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ad, ab, adb)
            } else result = true
            Stack.subVec3d(3)
            return result
        }

        fun searchOrigin(): Boolean {
            val tmp = Stack.newVec3d()
            tmp.set(1.0, 0.0, 0.0)
            val result = searchOrigin(tmp)
            Stack.subVec3d(1)
            return result
        }

        fun searchOrigin(initRay: Vector3d): Boolean {
            val ao = Stack.newVec3d()
            val ab = Stack.newVec3d()
            val ac = Stack.newVec3d()
            val ad = Stack.newVec3d()

            iterations = 0
            order = -1
            failed = false
            rayDirection.set(initRay)
            rayDirection.normalize()

            Arrays.fill(table, null)

            fetchSupport()
            rayDirection.set(simplex[0].origin).negate()
            while (iterations < GJK_MAX_ITERATIONS) {
                val rl = rayDirection.length()
                rayDirection.mul(1f / (if (rl > 0f) rl else 1f))
                if (fetchSupport()) {
                    var found = false
                    when (order) {
                        1 -> {
                            simplex[1].origin.negate(ao)
                            simplex[0].origin.sub(simplex[1].origin, ab)
                            found = solveSimplex2(ao, ab)
                        }
                        2 -> {
                            simplex[2].origin.negate(ao)
                            simplex[1].origin.sub(simplex[2].origin, ab)
                            simplex[0].origin.sub(simplex[2].origin, ac)
                            found = solveSimplex3(ao, ab, ac)
                        }
                        3 -> {
                            simplex[3].origin.negate(ao)
                            simplex[2].origin.sub(simplex[3].origin, ab)
                            simplex[1].origin.sub(simplex[3].origin, ac)
                            simplex[0].origin.sub(simplex[3].origin, ad)
                            found = solveSimplex4(ao, ab, ac, ad)
                        }
                    }
                    if (found) {
                        Stack.subVec3d(4)
                        return true
                    }
                } else {
                    Stack.subVec3d(4)
                    return false
                }
                ++iterations
            }
            failed = true
            Stack.subVec3d(4)
            return false
        }

        fun encloseOrigin(): Boolean {
            when (order) {
                1 -> {
                    val tmp = Stack.newVec3f()
                    val ab = Stack.newVec3f()
                    simplex[1].origin.sub(simplex[0].origin, ab)

                    val b0 = ab.crossX(Stack.newVec3f())
                    val b1 = ab.crossY(Stack.newVec3f())
                    val b2 = ab.crossZ(Stack.newVec3f())

                    val m0 = b0.lengthSquared()
                    val m1 = b1.lengthSquared()
                    val m2 = b2.lengthSquared()

                    val tmpQuat = Stack.newQuat()
                    ab.normalize(tmp)
                    tmpQuat.setAngleAxis(TAUf / 3f, tmp)

                    val r = Stack.newMat()
                    r.set(tmpQuat)

                    val w = Stack.newVec3f()
                    w.set(if (m0 > m1) if (m0 > m2) b0 else b2 else if (m1 > m2) b1 else b2)
                    w.normalize(tmp)
                    support(tmp, simplex[4])
                    r.transform(w)
                    w.normalize(tmp)
                    support(tmp, simplex[2])
                    r.transform(w).normalize(tmp)
                    support(tmp, simplex[3])
                    r.transform(w)
                    order = 4

                    Stack.subVec3f(6)
                    Stack.subMat(1)
                    Stack.subQuat(1)

                    return true
                }
                2 -> {
                    val n0 = Stack.newVec3d()
                    subCross(simplex[0].origin, simplex[1].origin, simplex[2].origin, n0)
                    n0.normalize()

                    val n1 = Stack.newVec3f().set(n0)

                    support(n1, simplex[3])

                    n1.negate()
                    support(n1, simplex[4])
                    order = 4

                    Stack.subVec3f(1)
                    Stack.subVec3d(1)
                    return true
                }
                3, 4 -> return true
                else -> return false
            }
        }
    }

    class Face {
        val vertices: Array<Ray?> = arrayOfNulls(3)
        val children: Array<Face?> = arrayOfNulls<Face>(3)
        val edges: IntArray = IntArray(3)
        val normal = Vector3f()
        var depth = 0f
        var mark: Int = 0
        var prev: Face? = null
        var next: Face? = null
    }

    inner class EPA(pgjk: GJK) {
        var gjk: GJK = pgjk

        //public btStackAlloc* sa;
        var root: Face? = null
        var nfaces: Int = 0
        var iterations: Int = 0
        val features = Array(6) { Vector3f() }

        val nearest0 = Vector3f()
        val nearest1 = Vector3f()

        val normal = Vector3f()
        var depth = 0f
        var failed = false

        fun getCoordinates(face: Face, out: Vector3f): Vector3f {
            val tmp = Stack.newVec3f()
            val tmp1 = Stack.newVec3f()
            val tmp2 = Stack.newVec3f()

            val o = Stack.newVec3d()
            face.normal.mul(-face.depth.toDouble(), o)

            face.vertices[0]!!.origin.sub(o, tmp1)
            face.vertices[1]!!.origin.sub(o, tmp2)
            tmp1.cross(tmp2, tmp)
            val a0 = tmp.length()

            face.vertices[1]!!.origin.sub(o, tmp1)
            face.vertices[2]!!.origin.sub(o, tmp2)
            tmp1.cross(tmp2, tmp)
            val a1 = tmp.length()

            face.vertices[2]!!.origin.sub(o, tmp1)
            face.vertices[0]!!.origin.sub(o, tmp2)
            tmp1.cross(tmp2, tmp)
            val a2 = tmp.length()

            val sm = a0 + a1 + a2

            out.set(a1, a2, a0)
            out.mul(1f / (if (sm > 0f) sm else 1f))

            Stack.subVec3f(4)
            return out
        }

        fun findBestFace(): Face? {
            var bestFace: Face? = null
            var currentFace = root
            var bestDepth = INFINITY
            while (currentFace != null) {
                if (currentFace.depth < bestDepth) {
                    bestDepth = currentFace.depth
                    bestFace = currentFace
                }
                currentFace = currentFace.next
            }
            return bestFace
        }

        fun set(f: Face, a: Ray, b: Ray, c: Ray): Boolean {
            val tmp1 = Stack.newVec3d()
            val tmp2 = Stack.newVec3d()
            val tmp3 = Stack.newVec3d()

            val normal = Stack.newVec3d()
            subCross(a.origin, b.origin, c.origin, normal)

            val len = normal.length().toFloat()

            a.origin.cross(b.origin, tmp1)
            b.origin.cross(c.origin, tmp2)
            c.origin.cross(a.origin, tmp3)

            val valid = (tmp1.dot(normal) >= -EPA_IN_FACE_EPSILON) &&
                    (tmp2.dot(normal) >= -EPA_IN_FACE_EPSILON) &&
                    (tmp3.dot(normal) >= -EPA_IN_FACE_EPSILON)

            f.vertices[0] = a
            f.vertices[1] = b
            f.vertices[2] = c
            f.mark = 0
            f.normal.set(normal).mul(1f / (if (len > 0f) len else INFINITY))
            f.depth = max(0f, -f.normal.dot(a.origin).toFloat())
            Stack.subVec3d(4)
            return valid
        }

        fun newFace(a: Ray, b: Ray, c: Ray): Face {
            //Face pf = new Face();
            val face = stackFace.get()
            if (set(face, a, b, c)) {
                if (root != null) {
                    root!!.prev = face
                }
                face.prev = null
                face.next = root
                root = face
                ++nfaces
            } else {
                face.next = null
                face.prev = face.next
            }
            return face
        }

        fun detach(face: Face) {
            if (face.prev != null || face.next != null) {
                --nfaces
                if (face === root) {
                    root = face.next
                    root!!.prev = null
                } else {
                    if (face.next == null) {
                        face.prev!!.next = null
                    } else {
                        checkNotNull(face.prev)
                        face.prev!!.next = face.next
                        face.next!!.prev = face.prev
                    }
                }
                face.next = null
                face.prev = face.next
            }
        }

        fun link(f0: Face, e0: Int, f1: Face, e1: Int) {
            f0.children[e0] = f1
            f1.edges[e1] = e0
            f1.children[e1] = f0
            f0.edges[e0] = e1
        }

        fun support(direction: Vector3f): Ray {
            val ray = stackMkv.get()
            gjk.support(direction, ray)
            return ray
        }

        fun buildHorizon(markId: Int, w: Ray, f: Face, e: Int, cf: Array<Face?>, ff: Array<Face?>): Int {
            var ne = 0
            if (f.mark != markId) {
                val e1: Int = mod3[e + 1]
                if ((f.normal.dot(w.origin) + f.depth) > 0) {
                    val nf = newFace(f.vertices[e1]!!, f.vertices[e]!!, w)
                    link(nf, 0, f, e)
                    if (cf[0] != null) {
                        link(cf[0]!!, 1, nf, 2)
                    } else {
                        ff[0] = nf
                    }
                    cf[0] = nf
                    ne = 1
                } else {
                    val e2 = mod3[e + 2]
                    detach(f)
                    f.mark = markId
                    ne += buildHorizon(markId, w, f.children[e1]!!, f.edges[e1], cf, ff)
                    ne += buildHorizon(markId, w, f.children[e2]!!, f.edges[e2], cf, ff)
                }
            }
            return (ne)
        }

        @JvmOverloads
        fun evaluatePD(accuracy: Float = EPA_ACCURACY): Float {
            pushStack()
            val tmp = Stack.newVec3f()
            try {
                var bestFace: Face? = null
                var markId = 1
                depth = Float.NEGATIVE_INFINITY
                normal.set(0.0, 0.0, 0.0)
                root = null
                nfaces = 0
                iterations = 0
                failed = false
                /* Prepare hull */
                if (gjk.encloseOrigin()) {
                    lateinit var faceIndices: Array<IntArray>
                    var numFaceIndices = 0

                    lateinit var edgeIndices: Array<IntArray>
                    var edgeIndex = 0
                    var numEdgeIndices = 0

                    when (gjk.order) {
                        3 -> {
                            faceIndices = tetrahedronFaceIndices
                            numFaceIndices = 4
                            edgeIndices = tetrahedronEdgeIndices
                            numEdgeIndices = 6
                        }
                        4 -> {
                            faceIndices = hexahedronFaceIndices
                            numFaceIndices = 6
                            edgeIndices = hexahedronEdgeIndices
                            numEdgeIndices = 9
                        }
                    }
                    val baseMkv = Array(gjk.order + 1) { i ->
                        Ray().apply {
                            set(gjk.simplex[i])
                        }
                    }
                    val baseFaces = Array(numFaceIndices) { faceIndex ->
                        newFace(
                            baseMkv[faceIndices[faceIndex][0]],
                            baseMkv[faceIndices[faceIndex][1]],
                            baseMkv[faceIndices[faceIndex][2]]
                        )
                    }
                    repeat(numEdgeIndices) {
                        link(
                            baseFaces[edgeIndices[edgeIndex][0]],
                            edgeIndices[edgeIndex][1],
                            baseFaces[edgeIndices[edgeIndex][2]],
                            edgeIndices[edgeIndex][3]
                        )
                        edgeIndex++
                    }
                }
                if (0 == nfaces) {
                    return depth
                }
                /* Expand hull */
                while (iterations < EPA_MAX_ITERATIONS) {
                    val bf = findBestFace()
                    if (bf != null) {
                        bf.normal.negate(tmp)
                        val w = support(tmp)
                        val d = bf.normal.dot(w.origin) + bf.depth
                        bestFace = bf
                        if (d < -accuracy) {
                            val cf = arrayOf<Face?>(null)
                            val ff = arrayOf<Face?>(null)
                            var nf = 0
                            detach(bf)
                            bf.mark = ++markId
                            for (i in 0..2) {
                                nf += buildHorizon(markId, w, bf.children[i]!!, bf.edges[i], cf, ff)
                            }
                            if (nf <= 2) {
                                break
                            }
                            link(cf[0]!!, 1, ff[0]!!, 2)
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                    ++iterations
                }
                /* Extract contact	*/
                if (bestFace != null) {
                    val bary = getCoordinates(bestFace, Stack.newVec3f())
                    normal.set(bestFace.normal)
                    depth = max(0f, bestFace.depth)

                    val tmp2 = Stack.newVec3d()
                    for (i in 0..1) {
                        val s = if (i != 0) -1f else 1f
                        for (j in 0..2) {
                            bestFace.vertices[j]!!.direction.mul(s, tmp)
                            gjk.localSupport(tmp, i, tmp2)
                            features[i * 3 + j].set(tmp2)
                        }
                    }

                    barycentricInterpolation(nearest0, features[0], features[1], features[2], bary)
                    barycentricInterpolation(nearest1, features[3], features[4], features[5], bary)
                    Stack.subVec3f(1)
                    Stack.subVec3d(1)
                } else {
                    failed = true
                }
                return depth
            } finally {
                popStack()
                Stack.subVec3f(1)
            }
        }
    }

    private fun barycentricInterpolation(dst: Vector3f, a: Vector3f, b: Vector3f, c: Vector3f, bary: Vector3f) {
        dst.set(
            bary.dot(a.x, b.x, c.x),
            bary.dot(a.y, b.y, c.y),
            bary.dot(a.z, b.z, c.z)
        )
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private val gjk = GJK()

    fun collide(
        shape0: ConvexShape, transform0: Transform,
        shape1: ConvexShape, transform1: Transform,
        radialMargin: Double,
        results: Results
    ): Boolean {

        results.witness0.set(0.0)
        results.witness1.set(0.0)
        results.normal.set(0f)
        results.depth = 0f
        results.status = ResultsStatus.SEPARATED
        results.epaIterations = 0
        results.gjkIterations = 0

        /* Use GJK to locate origin */
        gjk.init(
            transform0, shape0,
            transform1, shape1,
            radialMargin + EPA_ACCURACY
        )

        try {
            val collide = gjk.searchOrigin()
            results.gjkIterations = gjk.iterations + 1
            if (collide) {
                /* Then EPA for penetration depth	*/
                val epa = EPA(gjk)
                val pd = epa.evaluatePD()
                results.epaIterations = epa.iterations + 1
                if (pd > 0) {
                    results.status = ResultsStatus.PENETRATING
                    results.normal.set(epa.normal)
                    results.depth = pd
                    results.witness0.set(epa.nearest0)
                    results.witness1.set(epa.nearest1)
                    return true
                } else {
                    if (epa.failed) {
                        results.status = ResultsStatus.EPA_FAILED
                    }
                }
            } else {
                if (gjk.failed) {
                    results.status = ResultsStatus.GJK_FAILED
                }
            }
            return false
        } finally {
            gjk.destroy()
        }
    }

    companion object {
        private const val INFINITY = Float.POSITIVE_INFINITY
        private const val GJK_MAX_ITERATIONS = 128
        private const val GJK_HASH_SIZE = 1 shl 6
        private const val GJK_HASH_MASK = GJK_HASH_SIZE - 1
        private const val GJK_IN_SIMPLEX_EPSILON = 0.0001f
        private const val GJK_SQ_IN_SIMPLEX_EPSILON = GJK_IN_SIMPLEX_EPSILON * GJK_IN_SIMPLEX_EPSILON
        private const val EPA_MAX_ITERATIONS = 256
        private const val EPA_IN_FACE_EPSILON = 0.01f
        private const val EPA_ACCURACY = 0.001f

        private val mod3 = intArrayOf(0, 1, 2, 0, 1)

        private val tetrahedronFaceIndices = arrayOf(
            intArrayOf(2, 1, 0),
            intArrayOf(3, 0, 1),
            intArrayOf(3, 1, 2),
            intArrayOf(3, 2, 0)
        )

        private val tetrahedronEdgeIndices = arrayOf(
            intArrayOf(0, 0, 2, 1),
            intArrayOf(0, 1, 1, 1),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(1, 0, 3, 2),
            intArrayOf(2, 0, 1, 2),
            intArrayOf(3, 0, 2, 2)
        )

        private val hexahedronFaceIndices = arrayOf(
            intArrayOf(2, 0, 4),
            intArrayOf(4, 1, 2),
            intArrayOf(1, 4, 0),
            intArrayOf(0, 3, 1),
            intArrayOf(0, 2, 3),
            intArrayOf(1, 3, 2)
        )
        private val hexahedronEdgeIndices = arrayOf(
            intArrayOf(0, 0, 4, 0),
            intArrayOf(0, 1, 2, 1),
            intArrayOf(0, 2, 1, 2),
            intArrayOf(1, 1, 5, 2),
            intArrayOf(1, 0, 2, 0),
            intArrayOf(2, 2, 3, 2),
            intArrayOf(3, 1, 5, 0),
            intArrayOf(3, 0, 4, 2),
            intArrayOf(5, 1, 4, 1)
        )
    }
}
