package com.bulletphysics.collision.narrowphase

import com.bulletphysics.linearmath.VectorUtil.add
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setCross
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setScaleAdd
import com.bulletphysics.util.setSub

/**
 * VoronoiSimplexSolver is an implementation of the closest point distance algorithm
 * from a 1-4 points simplex to the origin. Can be used with GJK, as an alternative
 * to Johnson distance algorithm.
 *
 * @author jezek2
 */
class VoronoiSimplexSolver : SimplexSolverInterface {

    val subsimplexResultsPool = ObjectPool.Companion.get<SubSimplexClosestResult>(SubSimplexClosestResult::class.java)

    var numVertices: Int = 0

    val simplexVectorW = Array(VORONOI_SIMPLEX_MAX_VERTICES) { Vector3d() }
    val simplexPointsP = Array(VORONOI_SIMPLEX_MAX_VERTICES) { Vector3d() }
    val simplexPointsQ = Array(VORONOI_SIMPLEX_MAX_VERTICES) { Vector3d() }

    val cachedP1: Vector3d = Vector3d()
    val cachedP2: Vector3d = Vector3d()
    val cachedV: Vector3d = Vector3d()
    val lastW: Vector3d = Vector3d()
    var cachedValidClosest: Boolean = false

    val cachedBC: SubSimplexClosestResult = SubSimplexClosestResult()

    var needsUpdate: Boolean = false

    fun removeVertex(index: Int) {
        assert(numVertices > 0)
        numVertices--
        simplexVectorW[index].set(simplexVectorW[numVertices])
        simplexPointsP[index].set(simplexPointsP[numVertices])
        simplexPointsQ[index].set(simplexPointsQ[numVertices])
    }

    fun reduceVertices(usedVertices: UsageBitfield) {
        if ((numVertices() >= 4) && (!usedVertices.usedVertexD)) removeVertex(3)
        if ((numVertices() >= 3) && (!usedVertices.usedVertexC)) removeVertex(2)
        if ((numVertices() >= 2) && (!usedVertices.usedVertexB)) removeVertex(1)
        if ((numVertices() >= 1) && (!usedVertices.usedVertexA)) removeVertex(0)
    }

    fun updateClosestVectorAndPoints(): Boolean {
        if (needsUpdate) {
            cachedBC.reset()

            needsUpdate = false

            when (numVertices()) {
                0 -> cachedValidClosest = false
                1 -> {
                    cachedP1.set(simplexPointsP[0])
                    cachedP2.set(simplexPointsQ[0])
                    cachedV.setSub(cachedP1, cachedP2) //== m_simplexVectorW[0]
                    cachedBC.reset()
                    cachedBC.setBarycentricCoordinates(1.0, 0.0, 0.0, 0.0)
                    cachedValidClosest = cachedBC.isValid
                }
                2 -> {
                    val tmp = Stack.newVec()

                    //closest point origin from line segment
                    val from = simplexVectorW[0]
                    val to = simplexVectorW[1]
                    val nearest = Stack.newVec()

                    val p = Stack.newVec()
                    p.set(0.0, 0.0, 0.0)
                    val diff = Stack.newVec()
                    diff.setSub(p, from)

                    val v = Stack.newVec()
                    v.setSub(to, from)

                    var t = v.dot(diff)

                    if (t > 0) {
                        val dotVV = v.dot(v)
                        if (t < dotVV) {
                            t /= dotVV
                            tmp.setScale(t, v)
                            diff.sub(tmp)
                            cachedBC.usedVertices.usedVertexA = true
                        } else {
                            t = 1.0
                            diff.sub(v)
                            // reduce to 1 point
                        }
                        cachedBC.usedVertices.usedVertexB = true
                    } else {
                        t = 0.0
                        //reduce to 1 point
                        cachedBC.usedVertices.usedVertexA = true
                    }
                    cachedBC.setBarycentricCoordinates(1.0 - t, t, 0.0, 0.0)

                    tmp.setScale(t, v)
                    nearest.setAdd(from, tmp)

                    tmp.setSub(simplexPointsP[1], simplexPointsP[0])
                    tmp.mul(t)
                    cachedP1.setAdd(simplexPointsP[0], tmp)

                    tmp.setSub(simplexPointsQ[1], simplexPointsQ[0])
                    tmp.mul(t)
                    cachedP2.setAdd(simplexPointsQ[0], tmp)

                    cachedV.setSub(cachedP1, cachedP2)

                    reduceVertices(cachedBC.usedVertices)

                    cachedValidClosest = cachedBC.isValid
                    Stack.subVec(5)
                }
                3 -> {
                    val tmp1 = Stack.newVec()
                    val tmp2 = Stack.newVec()
                    val tmp3 = Stack.newVec()

                    // closest point origin from triangle
                    val p = Stack.newVec()
                    p.set(0.0, 0.0, 0.0)

                    val a = simplexVectorW[0]
                    val b = simplexVectorW[1]
                    val c = simplexVectorW[2]

                    closestPtPointTriangle(p, a, b, c, cachedBC)

                    tmp1.setScale(cachedBC.barycentricCoords[0], simplexPointsP[0])
                    tmp2.setScale(cachedBC.barycentricCoords[1], simplexPointsP[1])
                    tmp3.setScale(cachedBC.barycentricCoords[2], simplexPointsP[2])
                    add(cachedP1, tmp1, tmp2, tmp3)

                    tmp1.setScale(cachedBC.barycentricCoords[0], simplexPointsQ[0])
                    tmp2.setScale(cachedBC.barycentricCoords[1], simplexPointsQ[1])
                    tmp3.setScale(cachedBC.barycentricCoords[2], simplexPointsQ[2])
                    add(cachedP2, tmp1, tmp2, tmp3)

                    cachedV.setSub(cachedP1, cachedP2)

                    reduceVertices(cachedBC.usedVertices)
                    cachedValidClosest = cachedBC.isValid
                    Stack.subVec(4)
                }
                4 -> {
                    val a = simplexVectorW[0]
                    val b = simplexVectorW[1]
                    val c = simplexVectorW[2]
                    val d = simplexVectorW[3]

                    val p = Stack.newVec()
                    p.set(0.0, 0.0, 0.0)
                    val hasSeparation = closestPtPointTetrahedron(p, a, b, c, d, cachedBC)
                    Stack.subVec(1)

                    if (hasSeparation) {
                        val tmp1 = Stack.newVec()
                        val tmp2 = Stack.newVec()
                        val tmp3 = Stack.newVec()
                        val tmp4 = Stack.newVec()
                        tmp1.setScale(cachedBC.barycentricCoords[0], simplexPointsP[0])
                        tmp2.setScale(cachedBC.barycentricCoords[1], simplexPointsP[1])
                        tmp3.setScale(cachedBC.barycentricCoords[2], simplexPointsP[2])
                        tmp4.setScale(cachedBC.barycentricCoords[3], simplexPointsP[3])
                        add(cachedP1, tmp1, tmp2, tmp3, tmp4)

                        tmp1.setScale(cachedBC.barycentricCoords[0], simplexPointsQ[0])
                        tmp2.setScale(cachedBC.barycentricCoords[1], simplexPointsQ[1])
                        tmp3.setScale(cachedBC.barycentricCoords[2], simplexPointsQ[2])
                        tmp4.setScale(cachedBC.barycentricCoords[3], simplexPointsQ[3])
                        add(cachedP2, tmp1, tmp2, tmp3, tmp4)

                        cachedV.setSub(cachedP1, cachedP2)
                        reduceVertices(cachedBC.usedVertices)
                        Stack.subVec(4)
                        cachedValidClosest = cachedBC.isValid
                    } else {
                        //					printf("sub distance got penetration\n");
                        if (cachedBC.degenerate) {
                            cachedValidClosest = false
                        } else {
                            cachedValidClosest = true
                            //degenerate case == false, penetration = true + zero
                            cachedV.set(0.0, 0.0, 0.0)
                        }
                    }
                }
                else -> {
                    cachedValidClosest = false
                }
            }
        }

        return cachedValidClosest
    }

    fun closestPtPointTriangle(
        p: Vector3d, a: Vector3d, b: Vector3d, c: Vector3d,
        result: SubSimplexClosestResult
    ): Boolean {
        result.usedVertices.reset()

        // Check if P in vertex region outside A
        val ab = Stack.newVec()
        ab.setSub(b, a)

        val ac = Stack.newVec()
        ac.setSub(c, a)

        val ap = Stack.newVec()
        ap.setSub(p, a)

        val d1 = ab.dot(ap)
        val d2 = ac.dot(ap)

        if (d1 <= 0.0 && d2 <= 0.0) {
            result.closestPointOnSimplex.set(a)
            result.usedVertices.usedVertexA = true
            result.setBarycentricCoordinates(1.0, 0.0, 0.0, 0.0)
            Stack.subVec(3)
            return true // a; // barycentric coordinates (1,0,0)
        }

        // Check if P in vertex region outside B
        val bp = Stack.newVec()
        bp.setSub(p, b)

        val d3 = ab.dot(bp)
        val d4 = ac.dot(bp)

        if (d3 >= 0.0 && d4 <= d3) {
            result.closestPointOnSimplex.set(b)
            result.usedVertices.usedVertexB = true
            result.setBarycentricCoordinates(0.0, 1.0, 0.0, 0.0)
            Stack.subVec(4)
            return true // b; // barycentric coordinates (0,1,0)
        }

        // Check if P in edge region of AB, if so return projection of P onto AB
        val vc = d1 * d4 - d3 * d2
        if (vc <= 0.0 && d1 >= 0.0 && d3 <= 0.0) {
            val v = d1 / (d1 - d3)
            result.closestPointOnSimplex.setScaleAdd(v, ab, a)
            result.usedVertices.usedVertexA = true
            result.usedVertices.usedVertexB = true
            result.setBarycentricCoordinates(1.0 - v, v, 0.0, 0.0)
            Stack.subVec(4)
            return true
            //return a + v * ab; // barycentric coordinates (1-v,v,0)
        }

        // Check if P in vertex region outside C
        val cp = Stack.newVec()
        cp.setSub(p, c)

        val d5 = ab.dot(cp)
        val d6 = ac.dot(cp)

        if (d6 >= 0.0 && d5 <= d6) {
            result.closestPointOnSimplex.set(c)
            result.usedVertices.usedVertexC = true
            result.setBarycentricCoordinates(0.0, 0.0, 1.0, 0.0)
            Stack.subVec(5)
            return true //c; // barycentric coordinates (0,0,1)
        }

        // Check if P in edge region of AC, if so return projection of P onto AC
        val vb = d5 * d2 - d1 * d6
        if (vb <= 0.0 && d2 >= 0.0 && d6 <= 0.0) {
            val w = d2 / (d2 - d6)
            result.closestPointOnSimplex.setScaleAdd(w, ac, a)
            result.usedVertices.usedVertexA = true
            result.usedVertices.usedVertexC = true
            result.setBarycentricCoordinates(1.0 - w, 0.0, w, 0.0)
            Stack.subVec(5)
            return true
            //return a + w * ac; // barycentric coordinates (1-w,0,w)
        }

        // Check if P in edge region of BC, if so return projection of P onto BC
        val va = d3 * d6 - d5 * d4
        if (va <= 0.0 && (d4 - d3) >= 0.0 && (d5 - d6) >= 0.0) {
            val w = (d4 - d3) / ((d4 - d3) + (d5 - d6))

            val tmp = Stack.newVec()
            tmp.setSub(c, b)
            result.closestPointOnSimplex.setScaleAdd(w, tmp, b)

            result.usedVertices.usedVertexB = true
            result.usedVertices.usedVertexC = true
            result.setBarycentricCoordinates(0.0, 1.0 - w, w, 0.0)
            Stack.subVec(6)
            return true
            // return b + w * (c - b); // barycentric coordinates (0,1-w,w)
        }

        // P inside face region. Compute Q through its barycentric coordinates (u,v,w)
        val denom = 1.0 / (va + vb + vc)
        val v = vb * denom
        val w = vc * denom

        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()

        tmp1.setScale(v, ab)
        tmp2.setScale(w, ac)
        add(result.closestPointOnSimplex, a, tmp1, tmp2)
        result.usedVertices.usedVertexA = true
        result.usedVertices.usedVertexB = true
        result.usedVertices.usedVertexC = true
        result.setBarycentricCoordinates(1.0 - v - w, v, w, 0.0)
        Stack.subVec(7)

        return true
        //	return a + ab * v + ac * w; // = u*a + v*b + w*c, u = va * denom = btScalar(1.0) - v - w
    }

    fun closestPtPointTetrahedron(
        p: Vector3d,
        a: Vector3d,
        b: Vector3d,
        c: Vector3d,
        d: Vector3d,
        finalResult: SubSimplexClosestResult
    ): Boolean {
        val tempResult = subsimplexResultsPool.get()!!
        tempResult.reset()
        try {
            val tmp = Stack.newVec()
            val q = Stack.newVec()

            // Start out assuming point inside all halfspaces, so closest to itself
            finalResult.closestPointOnSimplex.set(p)
            finalResult.usedVertices.reset()
            finalResult.usedVertices.usedVertexA = true
            finalResult.usedVertices.usedVertexB = true
            finalResult.usedVertices.usedVertexC = true
            finalResult.usedVertices.usedVertexD = true

            val pointOutsideABC: Int = pointOutsideOfPlane(p, a, b, c, d)
            val pointOutsideACD: Int = pointOutsideOfPlane(p, a, c, d, b)
            val pointOutsideADB: Int = pointOutsideOfPlane(p, a, d, b, c)
            val pointOutsideBDC: Int = pointOutsideOfPlane(p, b, d, c, a)

            if (pointOutsideABC < 0 || pointOutsideACD < 0 || pointOutsideADB < 0 || pointOutsideBDC < 0) {
                finalResult.degenerate = true
                return false
            }

            if (pointOutsideABC == 0 && pointOutsideACD == 0 && pointOutsideADB == 0 && pointOutsideBDC == 0) {
                return false
            }


            var bestSqDist = Float.Companion.MAX_VALUE.toDouble()
            // If point outside face abc then compute closest point on abc
            if (pointOutsideABC != 0) {
                closestPtPointTriangle(p, a, b, c, tempResult)
                q.set(tempResult.closestPointOnSimplex)

                tmp.setSub(q, p)
                val sqDist = tmp.dot(tmp)
                // Update best closest point if (squared) distance is less than current best
                if (sqDist < bestSqDist) {
                    bestSqDist = sqDist
                    finalResult.closestPointOnSimplex.set(q)
                    //convert result bitmask!
                    finalResult.usedVertices.reset()
                    finalResult.usedVertices.usedVertexA = tempResult.usedVertices.usedVertexA
                    finalResult.usedVertices.usedVertexB = tempResult.usedVertices.usedVertexB
                    finalResult.usedVertices.usedVertexC = tempResult.usedVertices.usedVertexC
                    finalResult.setBarycentricCoordinates(
                        tempResult.barycentricCoords[VERTEX_A],
                        tempResult.barycentricCoords[VERTEX_B],
                        tempResult.barycentricCoords[VERTEX_C],
                        0.0
                    )
                }
            }


            // Repeat test for face acd
            if (pointOutsideACD != 0) {
                closestPtPointTriangle(p, a, c, d, tempResult)
                q.set(tempResult.closestPointOnSimplex)

                //convert result bitmask!
                tmp.setSub(q, p)
                val sqDist = tmp.dot(tmp)
                if (sqDist < bestSqDist) {
                    bestSqDist = sqDist
                    finalResult.closestPointOnSimplex.set(q)
                    finalResult.usedVertices.reset()
                    finalResult.usedVertices.usedVertexA = tempResult.usedVertices.usedVertexA

                    finalResult.usedVertices.usedVertexC = tempResult.usedVertices.usedVertexB
                    finalResult.usedVertices.usedVertexD = tempResult.usedVertices.usedVertexC
                    finalResult.setBarycentricCoordinates(
                        tempResult.barycentricCoords[VERTEX_A],
                        0.0,
                        tempResult.barycentricCoords[VERTEX_B],
                        tempResult.barycentricCoords[VERTEX_C]
                    )
                }
            }


            // Repeat test for face adb
            if (pointOutsideADB != 0) {
                closestPtPointTriangle(p, a, d, b, tempResult)
                q.set(tempResult.closestPointOnSimplex)

                //convert result bitmask!
                tmp.setSub(q, p)
                val sqDist = tmp.dot(tmp)
                if (sqDist < bestSqDist) {
                    bestSqDist = sqDist
                    finalResult.closestPointOnSimplex.set(q)
                    finalResult.usedVertices.reset()
                    finalResult.usedVertices.usedVertexA = tempResult.usedVertices.usedVertexA
                    finalResult.usedVertices.usedVertexB = tempResult.usedVertices.usedVertexC

                    finalResult.usedVertices.usedVertexD = tempResult.usedVertices.usedVertexB
                    finalResult.setBarycentricCoordinates(
                        tempResult.barycentricCoords[VERTEX_A],
                        tempResult.barycentricCoords[VERTEX_C],
                        0.0,
                        tempResult.barycentricCoords[VERTEX_B]
                    )
                }
            }


            // Repeat test for face bdc
            if (pointOutsideBDC != 0) {
                closestPtPointTriangle(p, b, d, c, tempResult)
                q.set(tempResult.closestPointOnSimplex)
                //convert result bitmask!
                tmp.setSub(q, p)
                val sqDist = tmp.dot(tmp)
                if (sqDist < bestSqDist) {
                    bestSqDist = sqDist
                    finalResult.closestPointOnSimplex.set(q)
                    finalResult.usedVertices.reset()
                    //
                    finalResult.usedVertices.usedVertexB = tempResult.usedVertices.usedVertexA
                    finalResult.usedVertices.usedVertexC = tempResult.usedVertices.usedVertexC
                    finalResult.usedVertices.usedVertexD = tempResult.usedVertices.usedVertexB

                    finalResult.setBarycentricCoordinates(
                        0.0,
                        tempResult.barycentricCoords[VERTEX_A],
                        tempResult.barycentricCoords[VERTEX_C],
                        tempResult.barycentricCoords[VERTEX_B]
                    )
                }
            }

            //help! we ended up full !
            if (finalResult.usedVertices.usedVertexA &&
                finalResult.usedVertices.usedVertexB &&
                finalResult.usedVertices.usedVertexC &&
                finalResult.usedVertices.usedVertexD
            ) {
                return true
            }

            return true
        } finally {
            subsimplexResultsPool.release(tempResult)
        }
    }

    /**
     * Clear the simplex, remove all the vertices.
     */
    override fun reset() {
        cachedValidClosest = false
        numVertices = 0
        needsUpdate = true
        lastW.set(1e308, 1e308, 1e308)
        cachedBC.reset()
    }

    override fun addVertex(w: Vector3d, p: Vector3d, q: Vector3d) {
        lastW.set(w)
        needsUpdate = true

        simplexVectorW[numVertices].set(w)
        simplexPointsP[numVertices]!!.set(p)
        simplexPointsQ[numVertices]!!.set(q)

        numVertices++
    }

    /**
     * Return/calculate the closest vertex.
     */
    override fun closest(v: Vector3d): Boolean {
        val succes = updateClosestVectorAndPoints()
        v.set(cachedV)
        return succes
    }

    override fun maxVertex(): Double {
        var maxV = 0.0
        for (i in 0 until numVertices()) {
            val curLen2 = simplexVectorW[i].lengthSquared()
            if (maxV < curLen2) {
                maxV = curLen2
            }
        }
        return maxV
    }

    override fun fullSimplex(): Boolean {
        return (numVertices == 4)
    }

    override fun getSimplex(pBuf: Array<Vector3d>, qBuf: Array<Vector3d>, yBuf: Array<Vector3d>): Int {
        for (i in 0 until numVertices()) {
            yBuf[i].set(simplexVectorW[i])
            pBuf[i].set(simplexPointsP[i])
            qBuf[i].set(simplexPointsQ[i])
        }
        return numVertices()
    }

    override fun inSimplex(w: Vector3d): Boolean {
        var found = false
        //btScalar maxV = btScalar(0.);

        //w is in the current (reduced) simplex

        for (i in 0 until numVertices()) {
            if (simplexVectorW[i].equals(w)) {
                found = true
                break
            }
        }

        //check in case lastW is already removed
        if (w.equals(lastW)) {
            return true
        }

        return found
    }

    override fun backupClosest(v: Vector3d) {
        v.set(cachedV)
    }

    override fun emptySimplex(): Boolean {
        return (numVertices() == 0)
    }

    override fun computePoints(p1: Vector3d, p2: Vector3d) {
        updateClosestVectorAndPoints()
        p1.set(cachedP1)
        p2.set(cachedP2)
    }

    override fun numVertices(): Int {
        return numVertices
    }

    /**///////////////////////////////////////////////////////////////////////// */
    class UsageBitfield {
        var usedVertexA: Boolean = false
        var usedVertexB: Boolean = false
        var usedVertexC: Boolean = false
        var usedVertexD: Boolean = false

        fun reset() {
            usedVertexA = false
            usedVertexB = false
            usedVertexC = false
            usedVertexD = false
        }
    }

    class SubSimplexClosestResult {
        val closestPointOnSimplex: Vector3d = Vector3d()

        //MASK for m_usedVertices
        //stores the simplex vertex-usage, using the MASK,
        // if m_usedVertices & MASK then the related vertex is used
        val usedVertices: UsageBitfield = UsageBitfield()
        val barycentricCoords: DoubleArray = DoubleArray(4)
        var degenerate: Boolean = false

        fun reset() {
            degenerate = false
            setBarycentricCoordinates(0.0, 0.0, 0.0, 0.0)
            usedVertices.reset()
        }

        val isValid: Boolean
            get() = (barycentricCoords[0] >= 0.0) &&
                    (barycentricCoords[1] >= 0.0) &&
                    (barycentricCoords[2] >= 0.0) &&
                    (barycentricCoords[3] >= 0.0)

        fun setBarycentricCoordinates(a: Double, b: Double, c: Double, d: Double) {
            barycentricCoords[0] = a
            barycentricCoords[1] = b
            barycentricCoords[2] = c
            barycentricCoords[3] = d
        }
    }

    companion object {
        private const val VORONOI_SIMPLEX_MAX_VERTICES = 5

        private const val VERTEX_A = 0
        private const val VERTEX_B = 1
        private const val VERTEX_C = 2

        /**
         * Test if point p and d lie on opposite sides of plane through abc
         */
        fun pointOutsideOfPlane(p: Vector3d, a: Vector3d, b: Vector3d, c: Vector3d, d: Vector3d): Int {
            val tmp = Stack.newVec()
            val normal = Stack.newVec()
            normal.setSub(b, a)
            tmp.setSub(c, a)
            normal.setCross(normal, tmp)

            tmp.setSub(p, a)
            val signp = tmp.dot(normal) // [AP AB AC]

            tmp.setSub(d, a)
            val signd = tmp.dot(normal) // [AD AB AC]

            //#ifdef CATCH_DEGENERATE_TETRAHEDRON
//	#ifdef BT_USE_DOUBLE_PRECISION
//	if (signd * signd < (btScalar(1e-8) * btScalar(1e-8)))
//		{
//			return -1;
//		}
//	#else
            Stack.subVec(2)
            if (signd * signd < ((1e-4f) * (1e-4f))) {
                //		printf("affine dependent/degenerate\n");//
                return -1
            }

            //#endif

            //#endif
            // Points on opposite sides if expression signs are opposite
            return if (signp * signd < 0.0) 1 else 0
        }
    }
}
