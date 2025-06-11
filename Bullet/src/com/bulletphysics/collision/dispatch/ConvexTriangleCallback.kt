package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.collision.shapes.TriangleShape
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * For each triangle in the concave mesh that overlaps with the AABB of a convex
 * (see [.convexBody] field), processTriangle is called.
 *
 * @author jezek2
 */
internal class ConvexTriangleCallback(
    private val dispatcher: Dispatcher,
    body0: CollisionObject,
    body1: CollisionObject,
    isSwapped: Boolean
) : TriangleCallback {
    private val convexBody: CollisionObject = if (isSwapped) body1 else body0
    private val triBody: CollisionObject = if (isSwapped) body0 else body1

    private val aabbMin = Vector3d()
    private val aabbMax = Vector3d()

    private var resultOut: ManifoldResult? = null

    private var dispatchInfoPtr: DispatcherInfo? = null
    private var collisionMarginTriangle = 0.0

    /**
     * create the manifold from the dispatcher 'manifold pool'
     * */
    val manifoldPtr = dispatcher.getNewManifold(convexBody, triBody)

    fun destroy() {
        clearCache()
        dispatcher.releaseManifold(manifoldPtr)
    }

    fun setTimeStepAndCounters(
        collisionMarginTriangle: Double,
        dispatchInfo: DispatcherInfo?,
        resultOut: ManifoldResult
    ) {
        this.dispatchInfoPtr = dispatchInfo
        this.collisionMarginTriangle = collisionMarginTriangle
        this.resultOut = resultOut

        // recalc aabbs
        val convexInTriangleSpace = Stack.newTrans()

        triBody.getWorldTransform(convexInTriangleSpace)
        convexInTriangleSpace.inverse()
        convexInTriangleSpace.mul(convexBody.getWorldTransform(Stack.newTrans()))

        val convexShape = convexBody.collisionShape
        convexShape!!.getAabb(convexInTriangleSpace, aabbMin, aabbMax)
        val extra = Stack.newVec()
        extra.set(collisionMarginTriangle, collisionMarginTriangle, collisionMarginTriangle)

        aabbMax.add(extra)
        aabbMin.sub(extra)
    }

    private val ci = CollisionAlgorithmConstructionInfo()
    private val tm = TriangleShape()

    init {

        clearCache()
    }

    override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
        // aabb filter is already applied!

        ci.dispatcher1 = dispatcher

        val ob = triBody

        // debug drawing of the overlapping triangles
        if (dispatchInfoPtr != null && dispatchInfoPtr!!.debugDraw != null && dispatchInfoPtr!!.debugDraw!!.debugMode > 0) {
            val color = Stack.newVec()
            color.set(255.0, 255.0, 0.0)
            val tr = ob.getWorldTransform(Stack.newTrans())

            val tmp1 = Stack.newVec()
            val tmp2 = Stack.newVec()

            tmp1.set(triangle[0])
            tr.transform(tmp1)
            tmp2.set(triangle[1])
            tr.transform(tmp2)
            dispatchInfoPtr!!.debugDraw!!.drawLine(tmp1, tmp2, color)

            tmp1.set(triangle[1])
            tr.transform(tmp1)
            tmp2.set(triangle[2])
            tr.transform(tmp2)
            dispatchInfoPtr!!.debugDraw!!.drawLine(tmp1, tmp2, color)

            tmp1.set(triangle[2])
            tr.transform(tmp1)
            tmp2.set(triangle[0])
            tr.transform(tmp2)
            dispatchInfoPtr!!.debugDraw!!.drawLine(tmp1, tmp2, color)
        }

        if (convexBody.collisionShape!!.isConvex) {
            tm.init(triangle[0]!!, triangle[1]!!, triangle[2]!!)
            tm.margin = collisionMarginTriangle

            val tmpShape = ob.collisionShape
            ob.internalSetTemporaryCollisionShape(tm)

            val colAlgo = ci.dispatcher1!!.findAlgorithm(convexBody, triBody, manifoldPtr)

            resultOut!!.setShapeIdentifiers(-1, -1, partId, triangleIndex)
            colAlgo!!.processCollision(convexBody, triBody, dispatchInfoPtr!!, resultOut!!)
            ci.dispatcher1!!.freeCollisionAlgorithm(colAlgo)
            ob.internalSetTemporaryCollisionShape(tmpShape)
        }
    }

    fun clearCache() {
        dispatcher.clearManifold(manifoldPtr)
    }

    fun getAabbMin(out: Vector3d): Vector3d {
        out.set(aabbMin)
        return out
    }

    fun getAabbMax(out: Vector3d): Vector3d {
        out.set(aabbMax)
        return out
    }
}
