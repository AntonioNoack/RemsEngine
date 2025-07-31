package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.dispatch.CollisionAlgorithmCreateFunc
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.ManifoldResult
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.collision.shapes.ConcaveShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.extras.gimpact.PlaneShape.getPlaneEquationTransformed
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.dot3
import com.bulletphysics.util.IntArrayList
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector4d

/**
 * Collision Algorithm for GImpact Shapes.
 *
 *
 *
 *
 * For register this algorithm in Bullet, proceed as following:
 * <pre>
 * CollisionDispatcher dispatcher = (CollisionDispatcher)dynamicsWorld.getDispatcher();
 * GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
</pre> *
 *
 * @author jezek2
 */
class GImpactCollisionAlgorithm : CollisionAlgorithm() {

    var convexAlgorithm: CollisionAlgorithm? = null
    var lastManifold: PersistentManifold? = null
    var resultOut: ManifoldResult? = null
    var dispatchInfo: DispatcherInfo? = null

    var face0: Int = 0
    var face1: Int = 0

    var part0: Int = 0
    var part1: Int = 0

    private val tmpPairList = IntPairList()

    override fun init(ci: CollisionAlgorithmConstructionInfo) {
        super.init(ci)
        lastManifold = null
        convexAlgorithm = null
    }

    override fun destroy() {
        clearCache()
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        clearCache()

        this.resultOut = resultOut
        this.dispatchInfo = dispatchInfo
        val gimpactShape0: GImpactShapeInterface?
        val gimpactShape1: GImpactShapeInterface?

        if (body0.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
            gimpactShape0 = body0.collisionShape as GImpactShapeInterface?

            if (body1.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
                gimpactShape1 = body1.collisionShape as GImpactShapeInterface?
                gimpactVsGimpact(body0, body1, gimpactShape0!!, gimpactShape1!!)
            } else {
                gimpactVsShape(body0, body1, gimpactShape0!!, body1.collisionShape!!, false)
            }
        } else if (body1.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
            gimpactShape1 = body1.collisionShape as GImpactShapeInterface?
            gimpactVsShape(body1, body0, gimpactShape1!!, body0.collisionShape!!, true)
        }
    }

    fun gimpactVsGimpact(
        body0: CollisionObject, body1: CollisionObject,
        shape0: GImpactShapeInterface, shape1: GImpactShapeInterface
    ) {
        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE) {
            val meshShape0 = shape0 as GImpactMeshShape
            part0 = meshShape0.meshPartCount
            while ((part0--) != 0) {
                gimpactVsGimpact(body0, body1, meshShape0.getMeshPart(part0), shape1)
            }
            return
        }

        if (shape1.gImpactShapeType == ShapeType.TRIMESH_SHAPE) {
            val meshShape1 = shape1 as GImpactMeshShape
            part1 = meshShape1.meshPartCount
            while ((part1--) != 0) {
                gimpactVsGimpact(body0, body1, shape0, meshShape1.getMeshPart(part1))
            }
            return
        }

        val orgTrans0 = body0.worldTransform
        val orgTrans1 = body1.worldTransform

        val pairList = tmpPairList
        pairList.clear()

        gimpactVsGimpactFindPairs(orgTrans0, orgTrans1, shape0, shape1, pairList)

        if (pairList.size == 0) {
            return
        }

        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE_PART &&
            shape1.gImpactShapeType == ShapeType.TRIMESH_SHAPE_PART
        ) {
            val shapePart0 = shape0 as GImpactMeshShapePart
            val shapePart1 = shape1 as GImpactMeshShapePart
            collideSatTriangles(body0, body1, shapePart0, shapePart1, pairList)
            return
        }

        // general function
        shape0.lockChildShapes()
        shape1.lockChildShapes()

        val retriever0 = GIMShapeRetriever(shape0)
        val retriever1 = GIMShapeRetriever(shape1)

        val childHasTransform0 = shape0.childrenHasTransform()
        val childHasTransform1 = shape1.childrenHasTransform()

        val tmpTrans = Stack.newTrans()

        var i = pairList.size()
        while ((i--) != 0) {
            this.face0 = pairList.getFirst(i)
            this.face1 = pairList.getSecond(i)
            val colShape0 = retriever0.getChildShape(this.face0)
            val colShape1 = retriever1.getChildShape(this.face1)

            if (childHasTransform0) {
                tmpTrans.setMul(orgTrans0, shape0.getChildTransform(this.face0))
                body0.setWorldTransform(tmpTrans)
            }

            if (childHasTransform1) {
                tmpTrans.setMul(orgTrans1, shape1.getChildTransform(this.face1))
                body1.setWorldTransform(tmpTrans)
            }

            // collide two convex shapes
            convexVsConvexCollision(body0, body1, colShape0, colShape1)

            if (childHasTransform0) {
                body0.setWorldTransform(orgTrans0)
            }

            if (childHasTransform1) {
                body1.setWorldTransform(orgTrans1)
            }
        }

        shape0.unlockChildShapes()
        shape1.unlockChildShapes()
    }

    fun gimpactVsShape(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: GImpactShapeInterface,
        shape1: CollisionShape,
        swapped: Boolean
    ) {
        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE) {
            val meshshape0 = shape0 as GImpactMeshShape
            part0 = meshshape0.meshPartCount

            while ((part0--) != 0) {
                gimpactVsShape(
                    body0,
                    body1,
                    meshshape0.getMeshPart(part0),
                    shape1, swapped
                )
            }

            return
        }

        //#ifdef GIMPACT_VS_PLANE_COLLISION
        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE_PART &&
            shape1 is StaticPlaneShape
        ) {
            val shapePart = shape0 as GImpactMeshShapePart
            triMeshPartVsPlaneCollision(body0, body1, shapePart, shape1, swapped)
            return
        }

        //#endif
        if (shape1 is CompoundShape) {
            gimpactVsCompoundShape(body0, body1, shape0, shape1, swapped)
            return
        } else if (shape1 is ConcaveShape) {
            gimpactVsConcave(body0, body1, shape0, shape1, swapped)
            return
        }

        val orgTrans0 = body0.worldTransform
        val orgTrans1 = body1.worldTransform

        val collidedResults = IntArrayList()

        gimpactVsShapeFindPairs(orgTrans0, orgTrans1, shape0, shape1, collidedResults)

        if (collidedResults.size() == 0) {
            return
        }
        shape0.lockChildShapes()

        val retriever0 = GIMShapeRetriever(shape0)

        val childHasTransform0 = shape0.childrenHasTransform()

        val tmpTrans = Stack.newTrans()
        var i = collidedResults.size()
        while ((i--) != 0) {
            val childIndex = collidedResults.get(i)
            if (swapped) {
                this.face1 = childIndex
            } else {
                this.face0 = childIndex
            }
            val colshape0 = retriever0.getChildShape(childIndex)

            if (childHasTransform0) {
                tmpTrans.setMul(orgTrans0, shape0.getChildTransform(childIndex))
                body0.setWorldTransform(tmpTrans)
            }

            // collide two shapes
            if (swapped) {
                shapeVsShapeCollision(body1, body0, shape1, colshape0)
            } else {
                shapeVsShapeCollision(body0, body1, colshape0, shape1)
            }

            // restore transforms
            if (childHasTransform0) {
                body0.setWorldTransform(orgTrans0)
            }
        }

        shape0.unlockChildShapes()
        Stack.subTrans(1)
    }

    fun gimpactVsCompoundShape(
        body0: CollisionObject,
        body1: CollisionObject,
        gimpact: GImpactShapeInterface,
        compound: CompoundShape,
        swapped: Boolean
    ) {
        val compoundTransform = body1.worldTransform
        val childTransform = Stack.newTrans()

        val children = compound.children
        for (i in children.indices) {

            val child = children[i]
            childTransform.setMul(compoundTransform, child.transform)

            body1.setWorldTransform(childTransform)

            // collide child shape
            gimpactVsShape(
                body0, body1,
                gimpact, child.shape, swapped
            )

            // restore transforms
            body1.setWorldTransform(compoundTransform)
        }
        Stack.subTrans(1)
    }

    fun gimpactVsConcave(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: GImpactShapeInterface,
        shape1: ConcaveShape,
        swapped: Boolean
    ) {
        // create the callback
        val callback = GImpactTriangleCallback()
        callback.algorithm = this
        callback.body0 = body0
        callback.body1 = body1
        callback.shape = shape0
        callback.swapped = swapped
        callback.margin = shape1.margin

        // getting the trimesh AABB
        val gimpactInConcaveSpace = Stack.newTrans()

        body1.getWorldTransform(gimpactInConcaveSpace)
        gimpactInConcaveSpace.inverse()
        gimpactInConcaveSpace.mul(body0.worldTransform)

        val minAABB = Stack.newVec()
        val maxAABB = Stack.newVec()
        shape0.getBounds(gimpactInConcaveSpace, minAABB, maxAABB)
        shape1.processAllTriangles(callback, minAABB, maxAABB)
        Stack.subVec(2)
    }

    /**
     * Creates a new contact point.
     */
    fun newContactManifold(body0: CollisionObject, body1: CollisionObject): PersistentManifold {
        this.lastManifold = dispatcher.getNewManifold(body0, body1)
        return this.lastManifold!!
    }

    fun destroyConvexAlgorithm() {
        if (convexAlgorithm != null) {
            //convex_algorithm.destroy();
            dispatcher.freeCollisionAlgorithm(convexAlgorithm!!)
            convexAlgorithm = null
        }
    }

    fun destroyContactManifolds() {
        if (this.lastManifold == null) return
        dispatcher.releaseManifold(this.lastManifold!!)
        this.lastManifold = null
    }

    fun clearCache() {
        destroyContactManifolds()
        destroyConvexAlgorithm()

        face0 = -1
        part0 = -1
        face1 = -1
        part1 = -1
    }

    /**
     * Call before process collision.
     */
    fun checkManifold(body0: CollisionObject, body1: CollisionObject) {
        if (this.lastManifold == null) {
            newContactManifold(body0, body1)
        }

        resultOut!!.persistentManifold = this.lastManifold!!
    }

    /**
     * Call before process collision.
     */
    fun newAlgorithm(body0: CollisionObject, body1: CollisionObject): CollisionAlgorithm? {
        checkManifold(body0, body1)
        return dispatcher.findAlgorithm(body0, body1, this.lastManifold)
    }

    /**
     * Call before process collision.
     */
    fun checkConvexAlgorithm(body0: CollisionObject, body1: CollisionObject) {
        if (convexAlgorithm != null) return
        convexAlgorithm = newAlgorithm(body0, body1)
    }

    fun addContactPoint(
        body0: CollisionObject,
        body1: CollisionObject,
        point: Vector3d,
        normal: Vector3d,
        distance: Double
    ) {
        resultOut!!.setShapeIdentifiers(part0, this.face0, part1, this.face1)
        checkManifold(body0, body1)
        resultOut!!.addContactPoint(normal, point, distance)
    }

    fun collideSatTriangles(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: GImpactMeshShapePart,
        shape1: GImpactMeshShapePart,
        pairs: IntPairList
    ) {
        val normal = Stack.newVec()

        val orgTrans0 = body0.worldTransform
        val orgTrans1 = body1.worldTransform

        val tri0 = PrimitiveTriangle()
        val tri1 = PrimitiveTriangle()
        val contactData = TriangleContact()

        shape0.lockChildShapes()
        shape1.lockChildShapes()

        for (i in 0 until pairs.size) {
            this.face0 = pairs.getFirst(i)
            this.face1 = pairs.getSecond(i)

            shape0.getPrimitiveTriangle(this.face0, tri0)
            shape1.getPrimitiveTriangle(this.face1, tri1)

            tri0.applyTransform(orgTrans0)
            tri1.applyTransform(orgTrans1)

            // build planes
            tri0.buildTriPlane()
            tri1.buildTriPlane()

            // test conservative
            if (tri0.overlapTestConservative(tri1)) {
                if (tri0.findTriangleCollisionClipMethod(tri1, contactData)) {
                    for (j in 0 until contactData.pointCount) {
                        normal.x = contactData.separatingNormal.x
                        normal.y = contactData.separatingNormal.y
                        normal.z = contactData.separatingNormal.z

                        addContactPoint(
                            body0, body1,
                            contactData.points[j], normal,
                            -contactData.penetrationDepth
                        )
                    }
                }
            }
        }

        shape0.unlockChildShapes()
        shape1.unlockChildShapes()

        Stack.subVec(1)
    }

    fun shapeVsShapeCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: CollisionShape?,
        shape1: CollisionShape?
    ) {
        val tmpShape0 = body0.collisionShape
        val tmpShape1 = body1.collisionShape

        body0.collisionShape = shape0
        body1.collisionShape = shape1

        val algor = newAlgorithm(body0, body1)

        // post :	checkManifold is called
        resultOut!!.setShapeIdentifiers(part0, this.face0, part1, this.face1)

        algor!!.processCollision(body0, body1, dispatchInfo!!, resultOut!!)

        //algor.destroy();
        dispatcher.freeCollisionAlgorithm(algor)

        body0.collisionShape = (tmpShape0)
        body1.collisionShape = (tmpShape1)
    }

    fun convexVsConvexCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: CollisionShape?,
        shape1: CollisionShape?
    ) {
        val tmpShape0 = body0.collisionShape
        val tmpShape1 = body1.collisionShape

        body0.collisionShape = (shape0)
        body1.collisionShape = (shape1)

        resultOut!!.setShapeIdentifiers(part0, this.face0, part1, this.face1)

        checkConvexAlgorithm(body0, body1)
        convexAlgorithm!!.processCollision(body0, body1, dispatchInfo!!, resultOut!!)

        body0.collisionShape = (tmpShape0)
        body1.collisionShape = (tmpShape1)
    }

    fun gimpactVsGimpactFindPairs(
        trans0: Transform, trans1: Transform,
        shape0: GImpactShapeInterface, shape1: GImpactShapeInterface,
        pairSet: IntPairList
    ) {
        if (shape0.hasBoxSet() && shape1.hasBoxSet()) {
            GImpactBvh.Companion.findCollision(shape0.boxSet, trans0, shape1.boxSet, trans1, pairSet)
        } else {
            val boxShape0 = AABB()
            val boxShape1 = AABB()

            for (i in 0 until shape0.numChildShapes) {
                shape0.getChildAabb(i, trans0, boxShape0.min, boxShape0.max)

                for (j in 0 until shape1.numChildShapes) {
                    shape1.getChildAabb(i, trans1, boxShape1.min, boxShape1.max)

                    if (boxShape1.hasCollision(boxShape0)) {
                        pairSet.pushPair(i, j)
                    }
                }
            }
        }
    }

    fun gimpactVsShapeFindPairs(
        trans0: Transform,
        trans1: Transform,
        shape0: GImpactShapeInterface,
        shape1: CollisionShape,
        collidedPrimitives: IntArrayList
    ) {
        val boxShape = AABB()

        if (shape0.hasBoxSet()) {
            val trans1to0 = Stack.newTrans()
            trans1to0.setInverse(trans0)
            trans1to0.mul(trans1)

            shape1.getBounds(trans1to0, boxShape.min, boxShape.max)

            shape0.boxSet.boxQuery(boxShape, collidedPrimitives)
        } else {
            shape1.getBounds(trans1, boxShape.min, boxShape.max)

            val boxShape0 = AABB()
            for (i in 0 until shape0.numChildShapes) {
                shape0.getChildAabb(i, trans0, boxShape0.min, boxShape0.max)
                if (boxShape.hasCollision(boxShape0)) {
                    collidedPrimitives.add(i)
                }
            }
        }
    }

    private fun triMeshPartVsPlaneCollision(
        body0: CollisionObject, body1: CollisionObject,
        shape0: GImpactMeshShapePart, shape1: StaticPlaneShape, swapped: Boolean
    ) {
        val orgTrans0 = body0.worldTransform
        val orgTrans1 = body1.worldTransform

        val plane = Vector4d()
        getPlaneEquationTransformed(shape1, orgTrans1, plane)

        // test box against plane
        val triangleBounds = AABB()
        shape0.getBounds(orgTrans0, triangleBounds.min, triangleBounds.max)
        triangleBounds.incrementMargin(shape1.margin)

        if (triangleBounds.planeClassify(plane) != PlaneIntersectionType.COLLIDE_PLANE) {
            return
        }

        shape0.lockChildShapes()

        val margin = shape0.margin + shape1.margin

        val vertex = Stack.newVec()
        val tmp = Stack.newVec()

        var vi = shape0.vertexCount
        while ((vi--) != 0) {
            shape0.getVertex(vi, vertex)
            orgTrans0.transformPosition(vertex)

            val distance = dot3(vertex, plane) - plane.w - margin
            if (distance < 0.0) { //add contact
                if (swapped) {
                    tmp.set(-plane.x, -plane.y, -plane.z)
                    addContactPoint(body1, body0, vertex, tmp, distance)
                } else {
                    tmp.set(plane.x, plane.y, plane.z)
                    addContactPoint(body0, body1, vertex, tmp, distance)
                }
            }
        }

        shape0.unlockChildShapes()
        Stack.subVec(2)
    }


    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        return 1.0
    }

    override fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>) {
        val lastManifold = lastManifold
        if (lastManifold != null) {
            manifoldArray.add(lastManifold)
        }
    }

    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(GImpactCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(ci)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as GImpactCollisionAlgorithm)
        }
    }

    companion object {
        /**
         * Use this function to register the algorithm externally.
         */
        @JvmStatic
        fun registerAlgorithm(dispatcher: CollisionDispatcher) {
            val createFunc = CreateFunc()
            val numTypes = BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal
            val i = BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE.ordinal
            for (j in 0 until numTypes) {
                dispatcher.registerCollisionCreateFunc(i, j, createFunc)
                dispatcher.registerCollisionCreateFunc(j, i, createFunc)
            }
        }
    }
}
