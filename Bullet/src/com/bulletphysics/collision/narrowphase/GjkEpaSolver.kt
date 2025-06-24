package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.MatrixUtil
import com.bulletphysics.linearmath.QuaternionUtil.setRotation
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.ArrayPool
import com.bulletphysics.util.ObjectStackList
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setNormalize
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub
import cz.advel.stack.Stack
import org.joml.Matrix3d
import org.joml.Vector3d
import java.util.*
import kotlin.math.abs
import kotlin.math.max

/**
 * GjkEpaSolver contributed under zlib by Nathanael Presson, Nov. 2006.
 *
 * @author jezek2
 */
class GjkEpaSolver {
    val floatArrays: ArrayPool<DoubleArray?> =
        ArrayPool.Companion.get(Double::class.javaPrimitiveType!!)

    val stackMkv = ObjectStackList<VertexAndRay>(VertexAndRay::class.java)
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
        SEPARATED,  /* Shapes don't penetrate												*/
        PENETRATING,  /* Shapes are penetrating												*/
        GJK_FAILED,  /* GJK phase fail, no big issue, shapes are probably just 'touching'	*/
        EPA_FAILED,  /* EPA phase fail, bigger problem, need to save parameters, and debug	*/
    }

    class Results {
        var status: ResultsStatus? = null
        val witnesses /*[2]*/ = arrayOf(Vector3d(), Vector3d())
        val normal: Vector3d = Vector3d()
        var depth: Double = 0.0
        var epaIterations: Int = 0
        var gjkIterations: Int = 0
    }

    class VertexAndRay {
        val w: Vector3d = Vector3d() // Minkowski vertex
        val r: Vector3d = Vector3d() // Ray

        fun set(m: VertexAndRay) {
            w.set(m.w)
            r.set(m.r)
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
        val wrotations /*[2]*/ = arrayOf(Matrix3d(), Matrix3d())
        val positions /*[2]*/ = arrayOf(Vector3d(), Vector3d())
        val shapes: Array<ConvexShape?> = arrayOfNulls(2)
        val simplex = Array(5) { VertexAndRay() }
        val ray: Vector3d = Vector3d()
        /*unsigned*/var order: Int = 0
        /*unsigned*/var iterations: Int = 0
        var margin: Double = 0.0
        var failed: Boolean = false

        constructor()

        @JvmOverloads
        constructor(
            wrot0: Matrix3d, pos0: Vector3d, shape0: ConvexShape?,
            wrot1: Matrix3d, pos1: Vector3d, shape1: ConvexShape?,
            pmargin: Double = 0.0
        ) {
            init(wrot0, pos0, shape0, wrot1, pos1, shape1, pmargin)
        }

        fun init( /*StackAlloc psa,*/
                  wrot0: Matrix3d, pos0: Vector3d, shape0: ConvexShape?,
                  wrot1: Matrix3d, pos1: Vector3d, shape1: ConvexShape?,
                  pmargin: Double
        ) {
            pushStack()
            wrotations[0].set(wrot0)
            positions[0].set(pos0)
            shapes[0] = shape0
            wrotations[1].set(wrot1)
            positions[1].set(pos1)
            shapes[1] = shape1
            //sa		=psa;
            //sablock	=sa->beginBlock();
            margin = pmargin
            failed = false
        }

        fun destroy() {
            popStack()
        }

        // vdh: very dummy hash
        /*unsigned*/ fun Hash(v: Vector3d): Int {
            val h = (v.x * 15461).toInt() xor (v.y * 83003).toInt() xor (v.z * 15473).toInt()
            return (h * 169639) and GJK_HASH_MASK
        }

        fun localSupport(
            d: Vector3d, /*unsigned*/i: Int, out: Vector3d
        ): Vector3d {
            val dir = Stack.newVec()
            MatrixUtil.transposeTransform(dir, d, wrotations[i])

            shapes[i]!!.localGetSupportingVertex(dir, out)
            wrotations[i].transform(out)
            out.add(positions[i])
            Stack.subVec(1)

            return out
        }

        fun support(d: Vector3d, v: VertexAndRay) {
            v.r.set(d)

            val tmp1 = localSupport(d, 0, Stack.newVec())

            val tmp = Stack.newVec()
            tmp.set(d)
            tmp.negate()
            val tmp2 = localSupport(tmp, 1, Stack.newVec())

            v.w.setSub(tmp1, tmp2)
            v.w.setScaleAdd(margin, d, v.w)
            Stack.subVec(3)
        }

        fun fetchSupport(): Boolean {
            val h = Hash(ray)
            var e = table[h]
            while (e != null) {
                if (e.v == ray) {
                    --order
                    return false
                } else {
                    e = e.n
                }
            }
            e = stackHe.get()
            e.v.set(ray)
            e.n = table[h]
            table[h] = e
            support(ray, simplex[++order])
            return (ray.dot(simplex[order].w) > 0)
        }

        fun solveSimplex2(ao: Vector3d, ab: Vector3d): Boolean {
            if (ab.dot(ao) >= 0) {
                val area = Stack.borrowVec()
                area.setCross(ab, ao)
                if (area.lengthSquared() > GJK_SQ_IN_SIMPLEX_EPSILON) {
                    ray.setCross(area, ab)
                } else {
                    return true
                }
            } else {
                order = 0
                simplex[0].set(simplex[1])
                ray.set(ao)
            }
            return false
        }

        fun solveSimplex3(ao: Vector3d, ab: Vector3d, ac: Vector3d): Boolean {
            val tmp = Stack.newVec()
            tmp.setCross(ab, ac)
            val result = solveSimplex3a(ao, ab, ac, tmp)
            Stack.subVec(1)
            return result
        }

        fun solveSimplex3a(ao: Vector3d, ab: Vector3d, ac: Vector3d, cabc: Vector3d): Boolean {
            // TODO: optimize

            val tmp = Stack.newVec()
            tmp.setCross(cabc, ab)

            val tmp2 = Stack.newVec()
            tmp2.setCross(cabc, ac)

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
                        ray.set(cabc)
                    } else {
                        ray.setNegate(cabc)

                        val swapTmp = VertexAndRay()
                        swapTmp.set(simplex[0])
                        simplex[0].set(simplex[1])
                        simplex[1].set(swapTmp)
                    }
                    result = false
                } else {
                    result = true
                }
            }
            Stack.subVec(2)
            return result
        }

        fun solveSimplex4(ao: Vector3d, ab: Vector3d, ac: Vector3d, ad: Vector3d): Boolean {
            // TODO: optimize

            val crs = Stack.newVec()

            val tmp = Stack.newVec()
            tmp.setCross(ab, ac)

            val tmp2 = Stack.newVec()
            tmp2.setCross(ac, ad)

            val tmp3 = Stack.newVec()
            tmp3.setCross(ad, ab)

            val result: Boolean
            if (tmp.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                crs.set(tmp)
                order = 2
                simplex[0].set(simplex[1])
                simplex[1].set(simplex[2])
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ab, ac, crs)
            } else if (tmp2.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                crs.set(tmp2)
                order = 2
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ac, ad, crs)
            } else if (tmp3.dot(ao) > GJK_IN_SIMPLEX_EPSILON) {
                crs.set(tmp3)
                order = 2
                simplex[1].set(simplex[0])
                simplex[0].set(simplex[2])
                simplex[2].set(simplex[3])
                result = solveSimplex3a(ao, ad, ab, crs)
            } else result = true
            Stack.subVec(4)
            return result
        }

        fun searchOrigin(): Boolean {
            val tmp = Stack.newVec()
            tmp.set(1.0, 0.0, 0.0)
            val result = searchOrigin(tmp)
            Stack.subVec(1)
            return result
        }

        fun searchOrigin(initRay: Vector3d): Boolean {
            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()
            val tmp3 = Stack.newVec()
            val tmp4 = Stack.newVec()

            iterations = 0
            order = -1
            failed = false
            ray.set(initRay)
            ray.normalize()

            Arrays.fill(table, null)

            fetchSupport()
            ray.setNegate(simplex[0].w)
            while (iterations < GJK_MAX_ITERATIONS) {
                val rl = ray.length()
                ray.mul(1.0 / (if (rl > 0.0) rl else 1.0))
                if (fetchSupport()) {
                    var found = false
                    when (order) {
                        1 -> {
                            tmp1.setNegate(simplex[1].w)
                            tmp2.setSub(simplex[0].w, simplex[1].w)
                            found = solveSimplex2(tmp1, tmp2)
                        }
                        2 -> {
                            tmp1.setNegate(simplex[2].w)
                            tmp2.setSub(simplex[1].w, simplex[2].w)
                            tmp3.setSub(simplex[0].w, simplex[2].w)
                            found = solveSimplex3(tmp1, tmp2, tmp3)
                        }
                        3 -> {
                            tmp1.setNegate(simplex[3].w)
                            tmp2.setSub(simplex[2].w, simplex[3].w)
                            tmp3.setSub(simplex[1].w, simplex[3].w)
                            tmp4.setSub(simplex[0].w, simplex[3].w)
                            found = solveSimplex4(tmp1, tmp2, tmp3, tmp4)
                        }
                    }
                    if (found) {
                        Stack.subVec(4)
                        return true
                    }
                } else {
                    Stack.subVec(4)
                    return false
                }
                ++iterations
            }
            failed = true
            Stack.subVec(4)
            return false
        }

        fun encloseOrigin(): Boolean {
            val tmp = Stack.newVec()
            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()

            when (order) {
                0 -> {}
                1 -> {
                    val ab = Stack.newVec()
                    ab.setSub(simplex[1].w, simplex[0].w)

                    val b = arrayOf(Stack.newVec(), Stack.newVec(), Stack.newVec())
                    b[0].set(1.0, 0.0, 0.0)
                    b[1].set(0.0, 1.0, 0.0)
                    b[2].set(0.0, 0.0, 1.0)

                    b[0].setCross(ab, b[0])
                    b[1].setCross(ab, b[1])
                    b[2].setCross(ab, b[2])

                    val m0 = b[0].lengthSquared()
                    val m1 = b[1].lengthSquared()
                    val m2 = b[2].lengthSquared()

                    val tmpQuat = Stack.newQuat()
                    tmp.setNormalize(ab)
                    tmpQuat.setAngleAxis(TAU / 3.0, tmp)

                    val r = Stack.newMat()
                    r.set(tmpQuat)

                    val w = Stack.newVec()
                    w.set(b[if (m0 > m1) if (m0 > m2) 0 else 2 else if (m1 > m2) 1 else 2])

                    tmp.setNormalize(w)
                    support(tmp, simplex[4])
                    r.transform(w)
                    tmp.setNormalize(w)
                    support(tmp, simplex[2])
                    r.transform(w)
                    tmp.setNormalize(w)
                    support(tmp, simplex[3])
                    r.transform(w)
                    order = 4

                    Stack.subVec(8)
                    Stack.subMat(1)
                    Stack.subQuat(1)

                    return true
                }
                2 -> {
                    tmp1.setSub(simplex[1].w, simplex[0].w)
                    tmp2.setSub(simplex[2].w, simplex[0].w)
                    val n = Stack.newVec()
                    n.setCross(tmp1, tmp2)
                    n.normalize()

                    support(n, simplex[3])

                    tmp.setNegate(n)
                    support(tmp, simplex[4])
                    order = 4

                    Stack.subVec(4)
                    return true
                }
                3, 4 -> {
                    Stack.subVec(3)
                    return true
                }
            }
            Stack.subVec(3)
            return false
        }
    }

    class Face {
        val vertices: Array<VertexAndRay?> = arrayOfNulls(3)
        val children: Array<Face?> = arrayOfNulls<Face>(3)
        val e: IntArray = IntArray(3)
        val n: Vector3d = Vector3d()
        var d: Double = 0.0
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
        val features = Array(6) { Vector3d() }
        val nearest /*[2]*/ = arrayOf(Vector3d(), Vector3d())
        val normal: Vector3d = Vector3d()
        var depth: Double = 0.0
        var failed: Boolean = false

        fun getCoordinates(face: Face, out: Vector3d): Vector3d {
            val tmp = Stack.newVec()
            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()

            val o = Stack.newVec()
            o.setScale(-face.d, face.n)

            val a = floatArrays.getFixed(3)

            tmp1.setSub(face.vertices[0]!!.w, o)
            tmp2.setSub(face.vertices[1]!!.w, o)
            tmp.setCross(tmp1, tmp2)
            a!![0] = tmp.length()

            tmp1.setSub(face.vertices[1]!!.w, o)
            tmp2.setSub(face.vertices[2]!!.w, o)
            tmp.setCross(tmp1, tmp2)
            a[1] = tmp.length()

            tmp1.setSub(face.vertices[2]!!.w, o)
            tmp2.setSub(face.vertices[0]!!.w, o)
            tmp.setCross(tmp1, tmp2)
            a[2] = tmp.length()

            val sm = a[0] + a[1] + a[2]

            out.set(a[1], a[2], a[0])
            out.mul(1.0 / (if (sm > 0.0) sm else 1.0))

            floatArrays.release(a)
            Stack.subVec(4)

            return out
        }

        fun findBestFace(): Face? {
            var bf: Face? = null
            if (root != null) {
                var cf = root
                var bd: Double = INFINITY
                do {
                    if (cf!!.d < bd) {
                        bd = cf.d
                        bf = cf
                    }
                } while (null != (cf.next.also { cf = it }))
            }
            return bf
        }

        fun set(f: Face, a: VertexAndRay, b: VertexAndRay, c: VertexAndRay): Boolean {
            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()
            val tmp3 = Stack.newVec()

            val nrm = Stack.newVec()
            tmp1.setSub(b.w, a.w)
            tmp2.setSub(c.w, a.w)
            nrm.setCross(tmp1, tmp2)

            val len = nrm.length()

            tmp1.setCross(a.w, b.w)
            tmp2.setCross(b.w, c.w)
            tmp3.setCross(c.w, a.w)

            val valid = (tmp1.dot(nrm) >= -EPA_IN_FACE_EPSILON) &&
                    (tmp2.dot(nrm) >= -EPA_IN_FACE_EPSILON) &&
                    (tmp3.dot(nrm) >= -EPA_IN_FACE_EPSILON)

            f.vertices[0] = a
            f.vertices[1] = b
            f.vertices[2] = c
            f.mark = 0
            f.n.setScale(1.0 / (if (len > 0.0) len else INFINITY), nrm)
            f.d = max(0.0, -f.n.dot(a.w))
            Stack.subVec(4)
            return valid
        }

        fun newFace(a: VertexAndRay, b: VertexAndRay, c: VertexAndRay): Face {
            //Face pf = new Face();
            val pf = stackFace.get()
            if (set(pf, a, b, c)) {
                if (root != null) {
                    root!!.prev = pf
                }
                pf.prev = null
                pf.next = root
                root = pf
                ++nfaces
            } else {
                pf.next = null
                pf.prev = pf.next
            }
            return (pf)
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
            f1.e[e1] = e0
            f1.children[e1] = f0
            f0.e[e0] = e1
        }

        fun support(w: Vector3d): VertexAndRay {
            //Mkv v = new Mkv();
            val v = stackMkv.get()
            gjk.support(w, v)
            return v
        }

        fun buildHorizon(markId: Int, w: VertexAndRay, f: Face, e: Int, cf: Array<Face?>, ff: Array<Face?>): Int {
            var ne = 0
            if (f.mark != markId) {
                val e1: Int = mod3[e + 1]
                if ((f.n.dot(w.w) + f.d) > 0) {
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
                    ne += buildHorizon(markId, w, f.children[e1]!!, f.e[e1], cf, ff)
                    ne += buildHorizon(markId, w, f.children[e2]!!, f.e[e2], cf, ff)
                }
            }
            return (ne)
        }

        @JvmOverloads
        fun evaluatePD(accuracy: Double = EPA_ACCURACY): Double {
            pushStack()
            val tmp = Stack.newVec()
            try {
                var bestFace: Face? = null
                var markId = 1
                depth = -INFINITY
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
                    val baseMkv = Array(gjk.order+1) { i->
                        VertexAndRay().apply {
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
                        tmp.setNegate(bf.n)
                        val w = support(tmp)
                        val d = bf.n.dot(w.w) + bf.d
                        bestFace = bf
                        if (d < -accuracy) {
                            val cf = arrayOf<Face?>(null)
                            val ff = arrayOf<Face?>(null)
                            var nf = 0
                            detach(bf)
                            bf.mark = ++markId
                            for (i in 0..2) {
                                nf += buildHorizon(markId, w, bf.children[i]!!, bf.e[i], cf, ff)
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
                    val bary = getCoordinates(bestFace, Stack.newVec())
                    normal.set(bestFace.n)
                    depth = max(0.0, bestFace.d)
                    for (i in 0..1) {
                        val s = if (i != 0) -1.0 else 1.0
                        for (j in 0..2) {
                            tmp.setScale(s, bestFace.vertices[j]!!.r)
                            gjk.localSupport(tmp, i, features[i * 3 + j])
                        }
                    }

                    barycentricInterpolation(nearest[0], features[0], features[1], features[2], bary)
                    barycentricInterpolation(nearest[1], features[3], features[4], features[5], bary)
                    Stack.subVec(1)
                } else {
                    failed = true
                }
                return depth
            } finally {
                popStack()
                Stack.subVec(1)
            }
        }
    }

    private fun barycentricInterpolation(dst: Vector3d, a: Vector3d, b: Vector3d, c: Vector3d, bary: Vector3d) {
        dst.x = bary.dot(a.x, b.x, c.x)
        dst.y = bary.dot(a.y, b.y, c.y)
        dst.z = bary.dot(a.z, b.z, c.z)
    }

    /**///////////////////////////////////////////////////////////////////////// */
    private val gjk = GJK()

    fun collide(
        shape0: ConvexShape?, wtrs0: Transform,
        shape1: ConvexShape?, wtrs1: Transform,
        radialMargin: Double,  /*,
			btStackAlloc* stackAlloc*/
        results: Results
    ): Boolean {
        // Initialize

        results.witnesses[0].set(0.0, 0.0, 0.0)
        results.witnesses[1].set(0.0, 0.0, 0.0)
        results.normal.set(0.0, 0.0, 0.0)
        results.depth = 0.0
        results.status = ResultsStatus.SEPARATED
        results.epaIterations = 0
        results.gjkIterations = 0
        /* Use GJK to locate origin		*/
        gjk.init(
            wtrs0.basis, wtrs0.origin, shape0,
            wtrs1.basis, wtrs1.origin, shape1,
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
                    results.witnesses[0].set(epa.nearest[0])
                    results.witnesses[1].set(epa.nearest[1])
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
        private const val INFINITY = BulletGlobals.SIMD_INFINITY
        private const val TAU = BulletGlobals.SIMD_TAU
        private const val GJK_MAX_ITERATIONS = 128
        private const val GJK_HASH_SIZE = 1 shl 6
        private const val GJK_HASH_MASK = GJK_HASH_SIZE - 1
        private const val GJK_IN_SIMPLEX_EPSILON = 0.0001
        private const val GJK_SQ_IN_SIMPLEX_EPSILON = GJK_IN_SIMPLEX_EPSILON * GJK_IN_SIMPLEX_EPSILON
        private const val EPA_MAX_ITERATIONS = 256
        private const val EPA_IN_FACE_EPSILON = 0.01
        private const val EPA_ACCURACY = 0.001

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
