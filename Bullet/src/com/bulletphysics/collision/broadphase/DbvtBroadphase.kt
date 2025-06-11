// Dbvt implementation by Nathanael Presson
package com.bulletphysics.collision.broadphase

import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
class DbvtBroadphase(paircache: OverlappingPairCache?) : BroadphaseInterface() {

    val sets = Array(2) { Dbvt() } // Dbvt sets
    var stageRoots: Array<DbvtProxy?> = arrayOfNulls(STAGE_COUNT + 1) // Stages list

    override var overlappingPairCache: OverlappingPairCache =
        (paircache ?: HashedOverlappingPairCache()) // Pair cache

    var predictedFrames: Double = 2.0 // Frames predicted
    var stageCurrent: Int = 0// Current stage
    var fixedUpdatesPerFrame: Int = 1 // % of fixed updates per frame
    var dynamicUpdatesPerFrame: Int = 1 // % of dynamic updates per frame
    var parseId: Int = 0 // Parse id
    var nextUId: Int = 0 // Gen id

    fun collide(dispatcher: Dispatcher) {
        //SPC(m_profiling.m_total);

        // optimize:

        sets[0].optimizeIncremental(1 + (sets[0].leaves * dynamicUpdatesPerFrame) / 100)
        sets[1].optimizeIncremental(1 + (sets[1].leaves * fixedUpdatesPerFrame) / 100)

        // dynamic -> fixed set:
        stageCurrent = (stageCurrent + 1) % STAGE_COUNT
        var current = stageRoots[stageCurrent]
        if (current != null) {
            val collider = DbvtTreeCollider(this)
            do {
                val next = current!!.link1
                stageRoots[current.stage] = listRemove(current, stageRoots[current.stage])
                stageRoots[STAGE_COUNT] = listAppend(current, stageRoots[STAGE_COUNT])
                Dbvt.Companion.collideTT(sets[1].root, current.leaf, collider)
                sets[0].remove(current.leaf!!)
                current.leaf = sets[1].insert(current.aabb, current)
                current.stage = STAGE_COUNT
                current = next
            } while (current != null)
        }

        // collide dynamics:
        run {
            val collider = DbvtTreeCollider(this)
            Dbvt.Companion.collideTT(sets[0].root, sets[1].root, collider)
            Dbvt.Companion.collideTT(sets[0].root, sets[0].root, collider)
        }

        collideCleanup(dispatcher)
        parseId++
    }

    private fun collideCleanup(dispatcher: Dispatcher) {
        val pairs = overlappingPairCache.overlappingPairArray
        var i = 0
        var ni = pairs.size
        while (i < ni) {
            val p = pairs[i]!!
            var pa = p.proxy0 as DbvtProxy
            var pb = p.proxy1 as DbvtProxy
            if (!DbvtAabbMm.Companion.intersect(pa.aabb, pb.aabb)) {
                //if(pa>pb) btSwap(pa,pb);
                if (pa.hashCode() > pb.hashCode()) {
                    val tmp = pa
                    pa = pb
                    pb = tmp
                }
                overlappingPairCache.removeOverlappingPair(pa, pb, dispatcher)
                ni--
                i--
            }
            i++
        }
    }

    override fun createProxy(
        aabbMin: Vector3d, aabbMax: Vector3d, shapeType: BroadphaseNativeType, userPtr: Any?,
        collisionFilterGroup: Short, collisionFilterMask: Short, dispatcher: Dispatcher, multiSapProxy: Any?
    ): BroadphaseProxy {
        val proxy = DbvtProxy(userPtr, collisionFilterGroup, collisionFilterMask)
        DbvtAabbMm.Companion.fromMinMax(aabbMin, aabbMax, proxy.aabb)
        proxy.leaf = sets[0].insert(proxy.aabb, proxy)
        proxy.stage = stageCurrent
        proxy.uid = ++nextUId
        stageRoots[stageCurrent] = listAppend(proxy, stageRoots[stageCurrent])
        return (proxy)
    }

    override fun destroyProxy(proxy: BroadphaseProxy, dispatcher: Dispatcher) {
        val proxy = proxy as DbvtProxy
        if (proxy.stage == STAGE_COUNT) {
            sets[1].remove(proxy.leaf!!)
        } else {
            sets[0].remove(proxy.leaf!!)
        }
        stageRoots[proxy.stage] = listRemove(proxy, stageRoots[proxy.stage])
        overlappingPairCache.removeOverlappingPairsContainingProxy(proxy, dispatcher)
        //btAlignedFree(proxy);
    }

    override fun setAabb(
        proxy: BroadphaseProxy,
        aabbMin: Vector3d,
        aabbMax: Vector3d,
        dispatcher: Dispatcher
    ) {
        val proxy = proxy as DbvtProxy
        val aabb: DbvtAabbMm = DbvtAabbMm.Companion.fromMinMax(aabbMin, aabbMax, Stack.newDbvtAabbMm())

        if (proxy.stage == STAGE_COUNT) {
            // fixed -> dynamic set
            sets[1].remove(proxy.leaf!!)
            proxy.leaf = sets[0].insert(aabb, proxy)
        } else {
            // dynamic set:
            if (DbvtAabbMm.Companion.intersect(proxy.leaf!!.volume, aabb)) { /* Moving				*/
                val delta = Stack.newVec()
                aabbMin.add(aabbMax, delta).mul(0.5)
                delta.sub(proxy.aabb.getCenter(Stack.newVec()))
                //#ifdef DBVT_BP_MARGIN
                delta.mul(predictedFrames)
                sets[0].update(proxy.leaf!!, aabb, delta, DBVT_BP_MARGIN)
                Stack.subVec(2)
                //#else
                //m_sets[0].update(proxy->leaf,aabb,delta*m_predictedframes);
                //#endif
            } else {
                // teleporting:
                sets[0].update(proxy.leaf!!, aabb)
            }
        }

        stageRoots[proxy.stage] = listRemove(proxy, stageRoots[proxy.stage])
        proxy.aabb.set(aabb)
        proxy.stage = stageCurrent
        stageRoots[stageCurrent] = listAppend(proxy, stageRoots[stageCurrent])
        Stack.subDbvtAabbMm(1)
    }

    override fun calculateOverlappingPairs(dispatcher: Dispatcher) {
        collide(dispatcher)
    }

    override fun getBroadphaseAabb(aabbMin: Vector3d, aabbMax: Vector3d) {
        val bounds = Stack.newDbvtAabbMm()
        if (!sets[0].isEmpty) {
            if (!sets[1].isEmpty) {
                DbvtAabbMm.Companion.union(sets[0].root!!.volume, sets[1].root!!.volume, bounds)
            } else {
                bounds.set(sets[0].root!!.volume)
            }
        } else if (!sets[1].isEmpty) {
            bounds.set(sets[1].root!!.volume)
        } else {
            DbvtAabbMm.Companion.fromCenterRadius(Stack.newVec(), 0.0, bounds)
            Stack.subVec(1)
        }
        aabbMin.set(bounds.min)
        aabbMax.set(bounds.max)
        Stack.subDbvtAabbMm(1)
    }

    companion object {
        const val DBVT_BP_MARGIN: Double = 0.05

        const val DYNAMIC_SET: Int = 0 // Dynamic set index
        const val FIXED_SET: Int = 1 // Fixed set index
        const val STAGE_COUNT: Int = 2 // Number of stages

        private fun listAppend(item: DbvtProxy, list: DbvtProxy?): DbvtProxy {
            var list = list
            item.link0 = null
            item.link1 = list
            if (list != null) list.link0 = item
            list = item
            return list
        }

        private fun listRemove(item: DbvtProxy, list: DbvtProxy?): DbvtProxy? {
            var list = list
            if (item.link0 != null) {
                item.link0!!.link1 = item.link1
            } else {
                list = item.link1
            }

            item.link1?.link0 = item.link0
            return list
        }
    }
}
