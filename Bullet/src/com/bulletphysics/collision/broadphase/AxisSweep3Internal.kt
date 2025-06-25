package com.bulletphysics.collision.broadphase

import com.bulletphysics.BulletStats
import com.bulletphysics.linearmath.MiscUtil
import com.bulletphysics.linearmath.VectorUtil.mul
import com.bulletphysics.linearmath.VectorUtil.setMax
import com.bulletphysics.linearmath.VectorUtil.setMin
import com.bulletphysics.util.ObjectArrayList
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setSub

/**
 * AxisSweep3Internal is an internal base class that implements sweep and prune.
 * Use concrete implementation [AxisSweep3] or [AxisSweep3_32].
 *
 * @author jezek2
 */
abstract class AxisSweep3Internal internal constructor(
    worldAabbMin: Vector3d,
    worldAabbMax: Vector3d,
    var bpHandleMask: Int,
    var handleSentinel: Int,
    userMaxHandles: Int,  /* = 16384*/
    var pairCache: OverlappingPairCache? /*=0*/
) : BroadphaseInterface() {

    val worldAabbMin: Vector3d = Vector3d() // overall system bounds
    val worldAabbMax: Vector3d = Vector3d() // overall system bounds

    val quantize: Vector3d = Vector3d() // scaling factor for quantization

    var numHandles: Int // number of active handles
    val maxHandles: Int = userMaxHandles + 1 // max number of handles, need to add one sentinel handle
    val handles = Array<Handle>(maxHandles) { createHandle() } // handles pool
    var firstFreeHandle: Int // free handles list

    // edge arrays for the 3 axes (each array has m_maxHandles * 2 + 2 sentinel entries)
    var edges = Array(3) { createEdgeArray(maxHandles * 2) }

    // OverlappingPairCallback is an additional optional user callback for adding/removing overlapping pairs, similar interface to OverlappingPairCache.
    var overlappingPairUserCallback: OverlappingPairCallback? = null

    var ownsPairCache: Boolean = false

    // JAVA NOTE: added
    var mask: Int

    init {

        if (this.pairCache == null) {
            this.pairCache = HashedOverlappingPairCache()
            ownsPairCache = true
        }

        //assert(bounds.HasVolume());

        // init bounds
        this.worldAabbMin.set(worldAabbMin)
        this.worldAabbMax.set(worldAabbMax)

        val aabbSize = Stack.newVec()
        aabbSize.setSub(this.worldAabbMax, this.worldAabbMin)

        val maxInt = this.handleSentinel

        quantize.set(maxInt / aabbSize.x, maxInt / aabbSize.y, maxInt / aabbSize.z)

        // allocate handles buffer and put all handles on free list
        this.numHandles = 0

        // handle 0 is reserved as the null index, and is also used as the sentinel
        firstFreeHandle = 1
        run {
            for (i in firstFreeHandle until maxHandles) {
                handles[i].nextFree = i + 1
            }
            handles[maxHandles - 1].nextFree = 0
        }

        //removed overlap management

        // make boundary sentinels
        handles[0].clientObject = null

        for (axis in 0..2) {
            handles[0].setMinEdges(axis, 0)
            handles[0].setMaxEdges(axis, 1)

            edges[axis].setPos(0, 0)
            edges[axis].setHandle(0, 0)
            edges[axis].setPos(1, handleSentinel)
            edges[axis].setHandle(1, 0)
            //#ifdef DEBUG_BROADPHASE
            //debugPrintAxis(axis);
            //#endif //DEBUG_BROADPHASE
        }

        // JAVA NOTE: added
        mask = getMaskI()
    }

    // allocation/deallocation
    fun allocHandle(): Int {
        assert(firstFreeHandle != 0)

        val handle = firstFreeHandle
        firstFreeHandle = getHandle(handle).nextFree
        numHandles++

        return handle
    }

    fun freeHandle(handle: Int) {
        assert(handle > 0 && handle < maxHandles)

        getHandle(handle).nextFree = firstFreeHandle
        firstFreeHandle = handle

        numHandles--
    }

    fun testOverlap(ignoreAxis: Int, pHandleA: Handle, pHandleB: Handle): Boolean {
        // optimization 1: check the array index (memory address), instead of the m_pos

        for (axis in 0..2) {
            if (axis == ignoreAxis) continue
            if (pHandleA.getMaxEdges(axis) < pHandleB.getMinEdges(axis) ||
                pHandleB.getMaxEdges(axis) < pHandleA.getMinEdges(axis)
            ) {
                return false
            }
        }

        //optimization 2: only 2 axis need to be tested (conflicts with 'delayed removal' optimization)

        /*for (int axis = 0; axis < 3; axis++)
		{
		if (m_pEdges[axis][pHandleA->m_maxEdges[axis]].m_pos < m_pEdges[axis][pHandleB->m_minEdges[axis]].m_pos ||
		m_pEdges[axis][pHandleB->m_maxEdges[axis]].m_pos < m_pEdges[axis][pHandleA->m_minEdges[axis]].m_pos)
		{
		return false;
		}
		}
		*/
        return true
    }

    //#ifdef DEBUG_BROADPHASE
    //void debugPrintAxis(int axis,bool checkCardinality=true);
    //#endif //DEBUG_BROADPHASE
    fun quantize(out: IntArray, point: Vector3d, isMax: Int) {
        val clampedPoint = Stack.newVec(point)

        setMax(clampedPoint, worldAabbMin)
        setMin(clampedPoint, worldAabbMax)

        clampedPoint.setSub(clampedPoint, worldAabbMin)
        mul(clampedPoint, clampedPoint, quantize)

        out[0] = ((clampedPoint.x.toInt() and bpHandleMask) or isMax) and mask
        out[1] = ((clampedPoint.y.toInt() and bpHandleMask) or isMax) and mask
        out[2] = ((clampedPoint.z.toInt() and bpHandleMask) or isMax) and mask
        Stack.subVec(1)
    }

    // sorting a min edge downwards can only ever *add* overlaps
    fun sortMinDown(axis: Int, edge: Int, updateOverlaps: Boolean) {
        val edgeArray = edges[axis]
        var edgeIdx = edge
        var prevIdx = edgeIdx - 1

        val edgeHandle = getHandle(edgeArray.getHandle(edgeIdx))

        while (edgeArray.getPos(edgeIdx) < edgeArray.getPos(prevIdx)) {
            val prevHandle = getHandle(edgeArray.getHandle(prevIdx))

            if (edgeArray.isMax(prevIdx) != 0) {
                // if previous edge is a maximum check the bounds and add an overlap if necessary
                if (updateOverlaps && testOverlap(axis, edgeHandle, prevHandle)) {
                    pairCache!!.addOverlappingPair(edgeHandle, prevHandle)
                    overlappingPairUserCallback?.addOverlappingPair(edgeHandle, prevHandle)
                }

                // update edge reference in other handle
                prevHandle.incMaxEdges(axis)
            } else {
                prevHandle.incMinEdges(axis)
            }
            edgeHandle.decMinEdges(axis)

            // swap the edges
            edgeArray.swap(edgeIdx, prevIdx)

            // decrement
            edgeIdx--
            prevIdx--
        }

        //#ifdef DEBUG_BROADPHASE
        //debugPrintAxis(axis);
        //#endif //DEBUG_BROADPHASE
    }

    // sorting a min edge upwards can only ever *remove* overlaps
    fun sortMinUp(axis: Int, edge: Int, dispatcher: Dispatcher, updateOverlaps: Boolean) {
        val edgeArray = edges[axis]
        var edgeIdx = edge
        var nextIdx = edgeIdx + 1
        val edgeHandle = getHandle(edgeArray.getHandle(edgeIdx))

        while (edgeArray.getHandle(nextIdx) != 0 && (edgeArray.getPos(edgeIdx) >= edgeArray.getPos(nextIdx))) {
            val nextHandle = getHandle(edgeArray.getHandle(nextIdx))

            if (edgeArray.isMax(nextIdx) != 0) {
                // if next edge is maximum remove any overlap between the two handles
                if (updateOverlaps) {
                    val handle0 = getHandle(edgeArray.getHandle(edgeIdx))
                    val handle1 = getHandle(edgeArray.getHandle(nextIdx))

                    pairCache!!.removeOverlappingPair(handle0, handle1, dispatcher)
                    overlappingPairUserCallback?.removeOverlappingPair(handle0, handle1, dispatcher)
                }

                // update edge reference in other handle
                nextHandle.decMaxEdges(axis)
            } else {
                nextHandle.decMinEdges(axis)
            }
            edgeHandle.incMinEdges(axis)

            // swap the edges
            edgeArray.swap(edgeIdx, nextIdx)

            // increment
            edgeIdx++
            nextIdx++
        }
    }

    // sorting a max edge downwards can only ever *remove* overlaps
    fun sortMaxDown(axis: Int, edge: Int, dispatcher: Dispatcher, updateOverlaps: Boolean) {
        val edgeArray = edges[axis]
        var edgeIdx = edge
        var prevIdx = edgeIdx - 1
        val edgeHandle = getHandle(edgeArray.getHandle(edgeIdx))

        while (edgeArray.getPos(edgeIdx) < edgeArray.getPos(prevIdx)) {
            val prevHandle = getHandle(edgeArray.getHandle(prevIdx))

            if (edgeArray.isMax(prevIdx) == 0) {
                // if previous edge was a minimum remove any overlap between the two handles
                if (updateOverlaps) {
                    // this is done during the overlappingpairarray iteration/narrowphase collision
                    val handle0 = getHandle(edgeArray.getHandle(edgeIdx))
                    val handle1 = getHandle(edgeArray.getHandle(prevIdx))
                    pairCache!!.removeOverlappingPair(handle0, handle1, dispatcher)
                    overlappingPairUserCallback?.removeOverlappingPair(handle0, handle1, dispatcher)
                }

                // update edge reference in other handle
                prevHandle.incMinEdges(axis)
            } else {
                prevHandle.incMaxEdges(axis)
            }
            edgeHandle.decMaxEdges(axis)

            // swap the edges
            edgeArray.swap(edgeIdx, prevIdx)

            // decrement
            edgeIdx--
            prevIdx--
        }

        //#ifdef DEBUG_BROADPHASE
        //debugPrintAxis(axis);
        //#endif //DEBUG_BROADPHASE
    }

    /**
     * sorting a max edge upwards can only ever *add* overlaps
     */
    private fun sortMaxUp(axis: Int, edge: Int, updateOverlaps: Boolean) {
        val edgeArray = edges[axis]
        var edgeIdx = edge
        var prevIdx = edgeIdx + 1
        val edgeHandle = getHandle(edgeArray.getHandle(edgeIdx))

        while (edgeArray.getHandle(prevIdx) != 0 && (edgeArray.getPos(edgeIdx) >= edgeArray.getPos(prevIdx))) {
            val nextHandle = getHandle(edgeArray.getHandle(prevIdx))

            if (edgeArray.isMax(prevIdx) == 0) {
                // if next edge is a minimum check the bounds and add an overlap if necessary
                if (updateOverlaps && testOverlap(axis, edgeHandle, nextHandle)) {
                    val handle0 = getHandle(edgeArray.getHandle(edgeIdx))
                    val handle1 = getHandle(edgeArray.getHandle(prevIdx))
                    pairCache!!.addOverlappingPair(handle0, handle1)
                    overlappingPairUserCallback?.addOverlappingPair(handle0, handle1)
                }

                // update edge reference in other handle
                nextHandle.decMinEdges(axis)
            } else {
                nextHandle.decMaxEdges(axis)
            }
            edgeHandle.incMaxEdges(axis)

            // swap the edges
            edgeArray.swap(edgeIdx, prevIdx)

            // increment
            edgeIdx++
            prevIdx++
        }
    }

    override fun calculateOverlappingPairs(dispatcher: Dispatcher) {
        if (pairCache!!.hasDeferredRemoval()) {
            val overlappingPairArray = pairCache!!.overlappingPairArray

            // perform a sort, to find duplicates and to sort 'invalid' pairs to the end
            @Suppress("UNCHECKED_CAST")
            MiscUtil.quickSort(overlappingPairArray as ObjectArrayList<BroadphasePair>, BroadphasePair.broadphasePairSortPredicate)

            val previousPair = BroadphasePair()
            previousPair.proxy0 = null
            previousPair.proxy1 = null
            previousPair.algorithm = null

            for (i in 0 until overlappingPairArray.size) {
                val pair = overlappingPairArray.getQuick(i)

                val isDuplicate = pair.equals(previousPair)
                previousPair.set(pair)

                if (isDuplicate || !testAabbOverlap(pair.proxy0!!, pair.proxy1!!)) {
                    pairCache!!.cleanOverlappingPair(pair, dispatcher)
                    pair.proxy0 = null
                    pair.proxy1 = null
                    BulletStats.overlappingPairs--
                }
            }

            overlappingPairArray.removeIf { pair: BroadphasePair? -> pair!!.proxy0 == null }

            //printf("overlappingPairArray.size=%d\n",overlappingPairArray.size);
        }
    }

    fun addHandle(
        aabbMin: Vector3d,
        aabbMax: Vector3d,
        pOwner: Any?,
        collisionFilter: Int,
        dispatcher: Dispatcher,
        multiSapProxy: Any?
    ): Int {
        // quantize the bounds
        val min = IntArray(3)
        val max = IntArray(3)
        quantize(min, aabbMin, 0)
        quantize(max, aabbMax, 1)

        // allocate a handle
        val handle = allocHandle()

        val pHandle = getHandle(handle)

        pHandle.uid = handle
        //pHandle->m_pOverlaps = 0;
        pHandle.clientObject = pOwner
        pHandle.collisionFilter = collisionFilter
        pHandle.multiSapParentProxy = multiSapProxy

        // compute current limit of edge arrays
        val limit = numHandles * 2

        // insert new edges just inside the max boundary edge
        for (axis in 0..2) {
            handles[0].setMaxEdges(axis, handles[0].getMaxEdges(axis) + 2)

            edges[axis].set(limit + 1, limit - 1)

            edges[axis].setPos(limit - 1, min[axis])
            edges[axis].setHandle(limit - 1, handle)

            edges[axis].setPos(limit, max[axis])
            edges[axis].setHandle(limit, handle)

            pHandle.setMinEdges(axis, limit - 1)
            pHandle.setMaxEdges(axis, limit)
        }

        // now sort the new edges to their correct position
        sortMinDown(0, pHandle.getMinEdges(0), false)
        sortMaxDown(0, pHandle.getMaxEdges(0), dispatcher, false)
        sortMinDown(1, pHandle.getMinEdges(1), false)
        sortMaxDown(1, pHandle.getMaxEdges(1), dispatcher, false)
        sortMinDown(2, pHandle.getMinEdges(2), true)
        sortMaxDown(2, pHandle.getMaxEdges(2), dispatcher, true)

        return handle
    }

    fun removeHandle(handleIdx: Int, dispatcher: Dispatcher) {
        val handle = getHandle(handleIdx)

        // explicitly remove the pairs containing the proxy
        // we could do it also in the sortMinUp (passing true)
        // todo: compare performance
        if (!pairCache!!.hasDeferredRemoval()) {
            pairCache!!.removeOverlappingPairsContainingProxy(handle, dispatcher)
        }

        // compute current limit of edge arrays
        val limit = numHandles * 2

        var axis: Int

        axis = 0
        while (axis < 3) {
            handles[0].setMaxEdges(axis, handles[0].getMaxEdges(axis) - 2)
            axis++
        }

        // remove the edges by sorting them up to the end of the list
        axis = 0
        while (axis < 3) {
            val pEdges = this.edges[axis]
            val max = handle.getMaxEdges(axis)
            pEdges.setPos(max, handleSentinel)

            sortMaxUp(axis, max, false)

            val i = handle.getMinEdges(axis)
            pEdges.setPos(i, handleSentinel)

            sortMinUp(axis, i, dispatcher, false)

            pEdges.setHandle(limit - 1, 0)
            pEdges.setPos(limit - 1, handleSentinel)

            axis++
        }

        // free the handle
        freeHandle(handleIdx)
    }

    fun updateHandle(handleIndex: Int, aabbMin: Vector3d, aabbMax: Vector3d, dispatcher: Dispatcher) {
        val handle = getHandle(handleIndex)

        // quantize the new bounds
        val min = IntArray(3)
        val max = IntArray(3)
        quantize(min, aabbMin, 0)
        quantize(max, aabbMax, 1)

        // update changed edges
        for (axis in 0..2) {
            val emin = handle.getMinEdges(axis)
            val emax = handle.getMaxEdges(axis)

            val dmin = min[axis] - edges[axis].getPos(emin)
            val dmax = max[axis] - edges[axis].getPos(emax)

            edges[axis].setPos(emin, min[axis])
            edges[axis].setPos(emax, max[axis])

            // expand (only adds overlaps)
            if (dmin < 0) {
                sortMinDown(axis, emin, true)
            }
            if (dmax > 0) {
                sortMaxUp(axis, emax, true) // shrink (only removes overlaps)
            }
            if (dmin > 0) {
                sortMinUp(axis, emin, dispatcher, true)
            }
            if (dmax < 0) {
                sortMaxDown(axis, emax, dispatcher, true)
            }

            //#ifdef DEBUG_BROADPHASE
            //debugPrintAxis(axis);
            //#endif //DEBUG_BROADPHASE
        }
    }

    private fun getHandle(index: Int): Handle {
        return handles[index]
    }

    override fun createProxy(
        aabbMin: Vector3d, aabbMax: Vector3d, shapeType: BroadphaseNativeType, userPtr: Any?,
        collisionFilter: Int, dispatcher: Dispatcher, multiSapProxy: Any?
    ): BroadphaseProxy {
        val handleId = addHandle(
            aabbMin, aabbMax, userPtr,
            collisionFilter, dispatcher, multiSapProxy
        )
        return getHandle(handleId)
    }

    override fun destroyProxy(proxy: BroadphaseProxy, dispatcher: Dispatcher) {
        val handle = proxy as Handle
        removeHandle(handle.uid, dispatcher)
    }

    override fun setAabb(proxy: BroadphaseProxy, aabbMin: Vector3d, aabbMax: Vector3d, dispatcher: Dispatcher) {
        val handle = proxy as Handle
        updateHandle(handle.uid, aabbMin, aabbMax, dispatcher)
    }

    fun testAabbOverlap(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): Boolean {
        val pHandleA = proxy0 as Handle
        val pHandleB = proxy1 as Handle

        // optimization 1: check the array index (memory address), instead of the m_pos
        for (axis in 0..2) {
            if (pHandleA.getMaxEdges(axis) < pHandleB.getMinEdges(axis) ||
                pHandleB.getMaxEdges(axis) < pHandleA.getMinEdges(axis)
            ) {
                return false
            }
        }
        return true
    }

    override val overlappingPairCache: OverlappingPairCache get() = pairCache!!

    // getAabb returns the axis aligned bounding box in the 'global' coordinate frame
    // will add some transform later
    override fun getBroadphaseAabb(aabbMin: Vector3d, aabbMax: Vector3d) {
        aabbMin.set(worldAabbMin)
        aabbMax.set(worldAabbMax)
    }

    abstract fun createEdgeArray(size: Int): EdgeArray

    abstract fun createHandle(): Handle

    abstract fun getMaskI(): Int

    interface EdgeArray {
        fun swap(idx1: Int, idx2: Int)

        fun set(dst: Int, src: Int)

        fun getPos(index: Int): Int

        fun setPos(index: Int, value: Int)

        fun getHandle(index: Int): Int

        fun setHandle(index: Int, value: Int)

        fun isMax(offset: Int): Int {
            return (getPos(offset) and 1)
        }
    }

    abstract class Handle : BroadphaseProxy() {
        abstract fun getMinEdges(edgeIndex: Int): Int

        abstract fun setMinEdges(edgeIndex: Int, value: Int)

        abstract fun getMaxEdges(edgeIndex: Int): Int

        abstract fun setMaxEdges(edgeIndex: Int, value: Int)

        fun incMinEdges(edgeIndex: Int) {
            setMinEdges(edgeIndex, getMinEdges(edgeIndex) + 1)
        }

        fun incMaxEdges(edgeIndex: Int) {
            setMaxEdges(edgeIndex, getMaxEdges(edgeIndex) + 1)
        }

        fun decMinEdges(edgeIndex: Int) {
            setMinEdges(edgeIndex, getMinEdges(edgeIndex) - 1)
        }

        fun decMaxEdges(edgeIndex: Int) {
            setMaxEdges(edgeIndex, getMaxEdges(edgeIndex) - 1)
        }

        var nextFree: Int
            get() = getMinEdges(0)
            public set(next) {
                setMinEdges(0, next)
            }
    }
}
