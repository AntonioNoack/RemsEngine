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

    fun init(ci: CollisionAlgorithmConstructionInfo, body0: CollisionObject?, body1: CollisionObject?) {
        super.init(ci)
        this.lastManifold = null
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
        val gimpactshape0: GImpactShapeInterface?
        val gimpactshape1: GImpactShapeInterface?

        if (body0.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
            gimpactshape0 = body0.collisionShape as GImpactShapeInterface?

            if (body1.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
                gimpactshape1 = body1.collisionShape as GImpactShapeInterface?

                gimpact_vs_gimpact(body0, body1, gimpactshape0!!, gimpactshape1!!)
            } else {
                gimpactVsShape(body0, body1, gimpactshape0!!, body1.collisionShape!!, false)
            }
        } else if (body1.collisionShape!!.shapeType == BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE) {
            gimpactshape1 = body1.collisionShape as GImpactShapeInterface?

            gimpactVsShape(body1, body0, gimpactshape1!!, body0.collisionShape!!, true)
        }
    }

    fun gimpact_vs_gimpact(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: GImpactShapeInterface,
        shape1: GImpactShapeInterface
    ) {
        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE) {
            val meshshape0 = shape0 as GImpactMeshShape
            part0 = meshshape0.meshPartCount

            while ((part0--) != 0) {
                gimpact_vs_gimpact(body0, body1, meshshape0.getMeshPart(part0), shape1)
            }

            return
        }

        if (shape1.gImpactShapeType == ShapeType.TRIMESH_SHAPE) {
            val meshshape1 = shape1 as GImpactMeshShape
            part1 = meshshape1.meshPartCount

            while ((part1--) != 0) {
                gimpact_vs_gimpact(body0, body1, shape0, meshshape1.getMeshPart(part1))
            }

            return
        }

        val orgtrans0 = body0.getWorldTransform(Stack.newTrans())
        val orgtrans1 = body1.getWorldTransform(Stack.newTrans())

        val pairList = tmpPairList
        pairList.clear()

        gimpactVsGimpactFindPairs(orgtrans0, orgtrans1, shape0, shape1, pairList)

        if (pairList.size() == 0) {
            return
        }
        if (shape0.gImpactShapeType == ShapeType.TRIMESH_SHAPE_PART &&
            shape1.gImpactShapeType == ShapeType.TRIMESH_SHAPE_PART
        ) {
            val shapepart0 = shape0 as GImpactMeshShapePart
            val shapepart1 = shape1 as GImpactMeshShapePart

            //specialized function
            //#ifdef BULLET_TRIANGLE_COLLISION
            //collide_gjk_triangles(body0,body1,shapepart0,shapepart1,&pairset[0].m_index1,pairset.size());
            //#else
            collideSatTriangles(body0, body1, shapepart0, shapepart1, pairList, pairList.size())

            //#endif
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
                tmpTrans.mul(orgtrans0, shape0.getChildTransform(this.face0))
                body0.setWorldTransform(tmpTrans)
            }

            if (childHasTransform1) {
                tmpTrans.mul(orgtrans1, shape1.getChildTransform(this.face1))
                body1.setWorldTransform(tmpTrans)
            }

            // collide two convex shapes
            convexVsConvexCollision(body0, body1, colShape0, colShape1)

            if (childHasTransform0) {
                body0.setWorldTransform(orgtrans0)
            }

            if (childHasTransform1) {
                body1.setWorldTransform(orgtrans1)
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
            shape1.shapeType == BroadphaseNativeType.STATIC_PLANE_PROXYTYPE
        ) {
            val shapepart = shape0 as GImpactMeshShapePart
            val planeshape = shape1 as StaticPlaneShape
            triMeshPartVsPlaneCollision(body0, body1, shapepart, planeshape, swapped)
            return
        }

        //#endif
        if (shape1.isCompound) {
            val compoundshape = shape1 as CompoundShape
            gimpact_vs_compoundshape(body0, body1, shape0, compoundshape, swapped)
            return
        } else if (shape1.isConcave) {
            val concaveshape = shape1 as ConcaveShape
            gimpact_vs_concave(body0, body1, shape0, concaveshape, swapped)
            return
        }

        val orgtrans0 = body0.getWorldTransform(Stack.newTrans())
        val orgtrans1 = body1.getWorldTransform(Stack.newTrans())

        val collidedResults = IntArrayList()

        gimpactVsShapeFindPairs(orgtrans0, orgtrans1, shape0, shape1, collidedResults)

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
                tmpTrans.mul(orgtrans0, shape0.getChildTransform(childIndex))
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
                body0.setWorldTransform(orgtrans0)
            }
        }

        shape0.unlockChildShapes()
    }

    fun gimpact_vs_compoundshape(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: GImpactShapeInterface,
        shape1: CompoundShape,
        swapped: Boolean
    ) {
        val orgtrans1 = body1.getWorldTransform(Stack.newTrans())
        val childtrans1 = Stack.newTrans()
        val tmpTrans = Stack.newTrans()

        var i = shape1.numChildShapes
        while ((i--) != 0) {
            val colshape1 = shape1.getChildShape(i)
            childtrans1.mul(orgtrans1, shape1.getChildTransform(i, tmpTrans))

            body1.setWorldTransform(childtrans1)

            // collide child shape
            gimpactVsShape(
                body0, body1,
                shape0, colshape1!!, swapped
            )

            // restore transforms
            body1.setWorldTransform(orgtrans1)
        }
    }

    fun gimpact_vs_concave(
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
        gimpactInConcaveSpace.mul(body0.getWorldTransform(Stack.newTrans()))

        val minAABB = Stack.newVec()
        val maxAABB = Stack.newVec()
        shape0.getAabb(gimpactInConcaveSpace, minAABB, maxAABB)

        shape1.processAllTriangles(callback, minAABB, maxAABB)
    }

    /**
     * Creates a new contact point.
     */
    fun newContactManifold(body0: CollisionObject, body1: CollisionObject): PersistentManifold {
        this.lastManifold = dispatcher!!.getNewManifold(body0, body1)
        return this.lastManifold!!
    }

    fun destroyConvexAlgorithm() {
        if (convexAlgorithm != null) {
            //convex_algorithm.destroy();
            dispatcher!!.freeCollisionAlgorithm(convexAlgorithm!!)
            convexAlgorithm = null
        }
    }

    fun destroyContactManifolds() {
        if (this.lastManifold == null) return
        dispatcher!!.releaseManifold(this.lastManifold!!)
        this.lastManifold = null
    }

    fun clearCache() {
        destroyContactManifolds()
        destroyConvexAlgorithm()

        this.face0 = -1
        part0 = -1
        this.face1 = -1
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

        return dispatcher!!.findAlgorithm(body0, body1, this.lastManifold)
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
        pairs: IntPairList,
        pair_count: Int
    ) {
        var pair_count = pair_count
        val tmp = Stack.newVec()

        val orgtrans0 = body0.getWorldTransform(Stack.newTrans())
        val orgtrans1 = body1.getWorldTransform(Stack.newTrans())

        val ptri0 = PrimitiveTriangle()
        val ptri1 = PrimitiveTriangle()
        val contact_data = TriangleContact()

        shape0.lockChildShapes()
        shape1.lockChildShapes()

        var pairPointer = 0

        while ((pair_count--) != 0) {
            this.face0 = pairs.getFirst(pairPointer)
            this.face1 = pairs.getSecond(pairPointer)
            pairPointer++

            shape0.getPrimitiveTriangle(this.face0, ptri0)
            shape1.getPrimitiveTriangle(this.face1, ptri1)

            //#ifdef TRI_COLLISION_PROFILING
            //bt_begin_gim02_tri_time();
            //#endif
            ptri0.applyTransform(orgtrans0)
            ptri1.applyTransform(orgtrans1)

            // build planes
            ptri0.buildTriPlane()
            ptri1.buildTriPlane()

            // test conservative
            if (ptri0.overlapTestConservative(ptri1)) {
                if (ptri0.findTriangleCollisionClipMethod(ptri1, contact_data)) {
                    var j = contact_data.pointCount
                    while ((j--) != 0) {
                        tmp.x = contact_data.separatingNormal.x
                        tmp.y = contact_data.separatingNormal.y
                        tmp.z = contact_data.separatingNormal.z

                        addContactPoint(
                            body0, body1,
                            contact_data.points[j],
                            tmp,
                            -contact_data.penetrationDepth
                        )
                    }
                }
            }

            //#ifdef TRI_COLLISION_PROFILING
            //bt_end_gim02_tri_time();
            //#endif
        }

        shape0.unlockChildShapes()
        shape1.unlockChildShapes()
    }

    fun shapeVsShapeCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: CollisionShape?,
        shape1: CollisionShape?
    ) {
        val tmpShape0 = body0.collisionShape
        val tmpShape1 = body1.collisionShape

        body0.internalSetTemporaryCollisionShape(shape0)
        body1.internalSetTemporaryCollisionShape(shape1)

        val algor = newAlgorithm(body0, body1)

        // post :	checkManifold is called
        resultOut!!.setShapeIdentifiers(part0, this.face0, part1, this.face1)

        algor!!.processCollision(body0, body1, dispatchInfo!!, resultOut!!)

        //algor.destroy();
        dispatcher!!.freeCollisionAlgorithm(algor)

        body0.internalSetTemporaryCollisionShape(tmpShape0)
        body1.internalSetTemporaryCollisionShape(tmpShape1)
    }

    fun convexVsConvexCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        shape0: CollisionShape?,
        shape1: CollisionShape?
    ) {
        val tmpShape0 = body0.collisionShape
        val tmpShape1 = body1.collisionShape

        body0.internalSetTemporaryCollisionShape(shape0)
        body1.internalSetTemporaryCollisionShape(shape1)

        resultOut!!.setShapeIdentifiers(part0, this.face0, part1, this.face1)

        checkConvexAlgorithm(body0, body1)
        convexAlgorithm!!.processCollision(body0, body1, dispatchInfo!!, resultOut!!)

        body0.internalSetTemporaryCollisionShape(tmpShape0)
        body1.internalSetTemporaryCollisionShape(tmpShape1)
    }

    fun gimpactVsGimpactFindPairs(
        trans0: Transform, trans1: Transform,
        shape0: GImpactShapeInterface, shape1: GImpactShapeInterface,
        pairset: IntPairList
    ) {
        if (shape0.hasBoxSet() && shape1.hasBoxSet()) {
            GImpactBvh.Companion.findCollision(shape0.boxSet, trans0, shape1.boxSet, trans1, pairset)
        } else {
            val boxshape0 = AABB()
            val boxshape1 = AABB()
            var i = shape0.numChildShapes

            while ((i--) != 0) {
                shape0.getChildAabb(i, trans0, boxshape0.min, boxshape0.max)

                var j = shape1.numChildShapes
                while ((j--) != 0) {
                    shape1.getChildAabb(i, trans1, boxshape1.min, boxshape1.max)

                    if (boxshape1.hasCollision(boxshape0)) {
                        pairset.pushPair(i, j)
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
        collided_primitives: IntArrayList
    ) {
        val boxshape = AABB()

        if (shape0.hasBoxSet()) {
            val trans1to0 = Stack.newTrans()
            trans1to0.inverse(trans0)
            trans1to0.mul(trans1)

            shape1.getAabb(trans1to0, boxshape.min, boxshape.max)

            shape0.boxSet.boxQuery(boxshape, collided_primitives)
        } else {
            shape1.getAabb(trans1, boxshape.min, boxshape.max)

            val boxShape0 = AABB()
            var i = shape0.numChildShapes

            while ((i--) != 0) {
                shape0.getChildAabb(i, trans0, boxShape0.min, boxShape0.max)

                if (boxshape.hasCollision(boxShape0)) {
                    collided_primitives.add(i)
                }
            }
        }
    }

    private fun triMeshPartVsPlaneCollision(
        body0: CollisionObject, body1: CollisionObject,
        shape0: GImpactMeshShapePart, shape1: StaticPlaneShape, swapped: Boolean
    ) {
        val orgTrans0 = body0.getWorldTransform(Stack.newTrans())
        val orgTrans1 = body1.getWorldTransform(Stack.newTrans())

        val plane = Vector4d()
        getPlaneEquationTransformed(shape1, orgTrans1, plane)

        // test box against plane
        val triangleBounds = AABB()
        shape0.getAabb(orgTrans0, triangleBounds.min, triangleBounds.max)
        triangleBounds.incrementMargin(shape1.margin)

        if (triangleBounds.planeClassify(plane) != PlaneIntersectionType.COLLIDE_PLANE) {
            Stack.subTrans(2)
            return
        }

        shape0.lockChildShapes()

        val margin = shape0.margin + shape1.margin

        val vertex = Stack.newVec()
        val tmp = Stack.newVec()

        var vi = shape0.vertexCount
        while ((vi--) != 0) {
            shape0.getVertex(vi, vertex)
            orgTrans0.transform(vertex)

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

        Stack.subTrans(2)
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
            algo.init(ci, body0, body1)
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
            for (i in 0 until numTypes) {
                dispatcher.registerCollisionCreateFunc(
                    BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE.ordinal,
                    i,
                    createFunc
                )
            }
            for (i in 0 until numTypes) {
                dispatcher.registerCollisionCreateFunc(
                    i,
                    BroadphaseNativeType.GIMPACT_SHAPE_PROXYTYPE.ordinal,
                    createFunc
                )
            }
        }
    }
}
