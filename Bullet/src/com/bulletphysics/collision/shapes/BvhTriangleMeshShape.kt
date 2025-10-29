package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * BvhTriangleMeshShape is a static-triangle mesh shape with several optimizations,
 * such as bounding volume hierarchy. It is recommended to enable useQuantizedAabbCompression
 * for better memory usage.
 *
 * It takes a triangle mesh as input, for example a [TriangleMesh] or
 * [TriangleIndexVertexArray]. The BvhTriangleMeshShape class allows for
 * triangle mesh deformations by a refit or partialRefit method.
 *
 * Instead of building the bounding volume hierarchy acceleration structure, it is
 * also possible to serialize (save) and deserialize (load) the structure from disk.
 * See ConcaveDemo for an example.
 *
 * @author jezek2
 */
class BvhTriangleMeshShape : TriangleMeshShape {
    private var bvh: OptimizedBvh?
    private var useQuantizedAabbCompression = false

    @get:Suppress("unused")
    var ownsBvh: Boolean
        private set

    private val myNodeCallbacks = ObjectPool.get<MyNodeOverlapCallback>(MyNodeOverlapCallback::class.java)

    @Suppress("unused")
    constructor() : super(null) {
        this.bvh = null
        this.ownsBvh = false
    }

    @Suppress("unused")
    constructor(meshInterface: StridingMeshInterface, useQuantizedAabbCompression: Boolean) :
            this(meshInterface, useQuantizedAabbCompression, true)

    constructor(meshInterface: StridingMeshInterface, useQuantizedAabbCompression: Boolean, buildBvh: Boolean) : super(
        meshInterface
    ) {

        this.bvh = null
        this.useQuantizedAabbCompression = useQuantizedAabbCompression
        this.ownsBvh = false

        // construct bvh from meshInterface
        //#ifndef DISABLE_BVH
        val bvhAabbMin = Stack.newVec()
        val bvhAabbMax = Stack.newVec()
        meshInterface.calculateAabbBruteForce(bvhAabbMin, bvhAabbMax)

        if (buildBvh) {
            bvh = OptimizedBvh()
            bvh!!.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax)
            ownsBvh = true

            // JAVA NOTE: moved from TriangleMeshShape
            recalculateLocalAabb()
        }
        Stack.subVec(2)

        //#endif //DISABLE_BVH
    }

    /**
     * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
     */
    @Suppress("unused")
    constructor(
        meshInterface: StridingMeshInterface,
        useQuantizedAabbCompression: Boolean,
        bvhAabbMin: Vector3d, bvhAabbMax: Vector3d
    ) : this(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax, true)

    /**
     * Optionally pass in a larger bvh aabb, used for quantization. This allows for deformations within this aabb.
     */
    constructor(
        meshInterface: StridingMeshInterface,
        useQuantizedAabbCompression: Boolean,
        bvhAabbMin: Vector3d, bvhAabbMax: Vector3d, buildBvh: Boolean
    ) : super(meshInterface) {
        this.bvh = null
        this.useQuantizedAabbCompression = useQuantizedAabbCompression
        this.ownsBvh = false

        // construct bvh from meshInterface
        //#ifndef DISABLE_BVH
        if (buildBvh) {
            bvh = OptimizedBvh()

            bvh!!.build(meshInterface, useQuantizedAabbCompression, bvhAabbMin, bvhAabbMax)
            ownsBvh = true
        }

        // JAVA NOTE: moved from TriangleMeshShape
        recalculateLocalAabb()
        //#endif //DISABLE_BVH
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.TRIANGLE_MESH_SHAPE_PROXYTYPE

    fun performRaycast(callback: TriangleCallback, raySource: Vector3d, rayTarget: Vector3d) {
        val myNodeCallback = myNodeCallbacks.get()
        myNodeCallback.init(callback, meshInterface!!)

        bvh!!.reportRayOverlappingNodex(myNodeCallback, raySource, rayTarget)

        myNodeCallbacks.release(myNodeCallback)
    }

    fun performConvexCast(
        callback: TriangleCallback, raySource: Vector3d, rayTarget: Vector3d,
        aabbMin: Vector3d, aabbMax: Vector3d
    ) {
        val myNodeCallback = myNodeCallbacks.get()
        myNodeCallback.init(callback, meshInterface!!)

        bvh!!.reportBoxCastOverlappingNodex(myNodeCallback, raySource, rayTarget, aabbMin, aabbMax)

        myNodeCallbacks.release(myNodeCallback)
    }

    /**
     * Perform bvh tree traversal and report overlapping triangles to 'callback'.
     */
    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        //#ifdef DISABLE_BVH
        // // brute force traverse all triangles
        //btTriangleMeshShape::processAllTriangles(callback,aabbMin,aabbMax);
        //#else

        // first get all the nodes

        val myNodeCallback = myNodeCallbacks.get()
        myNodeCallback.init(callback, meshInterface!!)

        bvh!!.reportAabbOverlappingNodes(myNodeCallback, aabbMin, aabbMax)

        myNodeCallbacks.release(myNodeCallback)
        //#endif//DISABLE_BVH
    }

    @Suppress("unused")
    fun refitTree(aabbMin: Vector3d?, aabbMax: Vector3d?) {
        // JAVA NOTE: update it for 2.70b1
        //bvh.refit(meshInterface, aabbMin, aabbMax);
        bvh!!.refit(meshInterface!!)

        recalculateLocalAabb()
    }

    /**
     * For a fast incremental refit of parts of the tree. Note: the entire AABB of the tree will become more conservative, it never shrinks.
     */
    @Suppress("unused")
    fun partialRefitTree(aabbMin: Vector3d, aabbMax: Vector3d) {
        bvh!!.refitPartial(meshInterface, aabbMin, aabbMax)

        setMin(localAabbMin, aabbMin)
        setMax(localAabbMax, aabbMax)
    }

    override fun setLocalScaling(scaling: Vector3d) {
        val tmp = Stack.newVec()
        getLocalScaling(tmp).sub(scaling)

        if (tmp.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
            super.setLocalScaling(scaling)
            /*
			if (ownsBvh)
			{
			m_bvh->~btOptimizedBvh();
			btAlignedFree(m_bvh);
			}
			*/
            // m_localAabbMin/m_localAabbMax is already re-calculated in btTriangleMeshShape. We could just scale aabb, but this needs some more work
            bvh = OptimizedBvh()
            // rebuild the bvh...
            bvh!!.build(meshInterface!!, useQuantizedAabbCompression, localAabbMin, localAabbMax)
            ownsBvh = true
        }
        Stack.subVec(1)
    }

    @get:Suppress("unused")
    @set:Suppress("unused")
    var optimizedBvh: OptimizedBvh?
        get() = bvh
        set(bvh) {
            val scaling = Stack.newVec()
            scaling.set(1.0, 1.0, 1.0)
            setOptimizedBvh(bvh, scaling)
        }

    fun setOptimizedBvh(bvh: OptimizedBvh?, scaling: Vector3d) {
        assert(this.bvh == null)
        assert(!ownsBvh)

        this.bvh = bvh

        // update the scaling without rebuilding the bvh
        val tmp = Stack.newVec()
        getLocalScaling(tmp).sub(scaling, tmp)

        if (tmp.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
            super.setLocalScaling(scaling)
        }
        Stack.subVec(1)
    }

    @Suppress("unused")
    fun usesQuantizedAabbCompression(): Boolean {
        return useQuantizedAabbCompression
    }

    class MyNodeOverlapCallback : NodeOverlapCallback {
        var meshInterface: StridingMeshInterface? = null
        var callback: TriangleCallback? = null

        private val triangle = Array(3) { Vector3d() }

        fun init(callback: TriangleCallback, meshInterface: StridingMeshInterface) {
            this.meshInterface = meshInterface
            this.callback = callback
        }

        override fun processNode(subPart: Int, triangleIndex: Int) {
            val meshInterface = meshInterface!!
            val data = meshInterface.getLockedReadOnlyVertexIndexBase(subPart)

            val meshScaling = meshInterface.getScaling(Stack.newVec())
            data.getTriangle(triangleIndex * 3, meshScaling, triangle)
            Stack.subVec(1)

            /* Perform ray vs. triangle collision here */
            val (a, b, c) = triangle
            callback!!.processTriangle(a, b, c, subPart, triangleIndex)

            meshInterface.unLockReadOnlyVertexBase(subPart)
        }
    }
}
