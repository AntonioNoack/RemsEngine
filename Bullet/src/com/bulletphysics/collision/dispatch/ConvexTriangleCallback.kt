package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.shapes.ConvexShape
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

    val aabbMin = Vector3d()
    val aabbMax = Vector3d()

    private var resultOut: ManifoldResult? = null

    private var dispatchInfoPtr: DispatcherInfo? = null
    private var collisionMarginTriangle = 0f

    /**
     * create the manifold from the dispatcher 'manifold pool'
     * */
    val manifoldPtr = dispatcher.getNewManifold(convexBody, triBody)

    fun destroy() {
        clearCache()
        dispatcher.releaseManifold(manifoldPtr)
    }

    fun setTimeStepAndCounters(
        collisionMarginTriangle: Float,
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
        convexInTriangleSpace.mul(convexBody.worldTransform)

        val convexShape = convexBody.collisionShape
        convexShape!!.getBounds(convexInTriangleSpace, aabbMin, aabbMax)

        aabbMax.add(collisionMarginTriangle.toDouble())
        aabbMin.sub(collisionMarginTriangle.toDouble())

        Stack.subTrans(1)
    }

    private val ci = CollisionAlgorithmConstructionInfo()
    private val tm = TriangleShape()

    init {

        clearCache()
    }

    override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
        // aabb filter is already applied!

        ci.dispatcher1 = dispatcher

        val ob = triBody

        // debug drawing of the overlapping triangles
        val debugDraw = dispatchInfoPtr?.debugDraw
        if (debugDraw != null && debugDraw.debugMode != 0) {
            val color = Stack.newVec3d().set(255.0, 255.0, 0.0)
            val tr = ob.worldTransform

            val tmp1 = Stack.newVec3d()
            val tmp2 = Stack.newVec3d()
            val tmp3 = Stack.newVec3d()

            tr.transformPosition(a, tmp1)
            tr.transformPosition(b, tmp2)
            tr.transformPosition(c, tmp3)
            debugDraw.drawLine(tmp1, tmp2, color)
            debugDraw.drawLine(tmp2, tmp3, color)
            debugDraw.drawLine(tmp3, tmp1, color)

            Stack.subVec3d(3)
        }

        val convexShape = convexBody.collisionShape
        if (convexShape is ConvexShape) {
            tm.init(a, b, c)
            tm.margin = collisionMarginTriangle

            val tmpShape = ob.collisionShape
            ob.collisionShape = tm

            val colAlgo = ci.dispatcher1.findAlgorithm(convexBody, triBody, manifoldPtr)

            resultOut!!.setShapeIdentifiers(-1, -1, partId, triangleIndex)
            colAlgo!!.processCollision(convexBody, triBody, dispatchInfoPtr!!, resultOut!!)
            ci.dispatcher1.freeCollisionAlgorithm(colAlgo)
            ob.collisionShape = (tmpShape)
        }
    }

    fun clearCache() {
        dispatcher.clearManifold(manifoldPtr)
    }
}
