// Dbvt implementation by Nathanael Presson
package com.bulletphysics.collision.broadphase

import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
class DbvtBroadphase() : BroadphaseInterface() {

    val dynamicSet = Dbvt()
    val staticSet = Dbvt()
    val stageRoots = arrayOfNulls<DbvtProxy>(STAGE_COUNT + 1) // Stages list

    override var overlappingPairCache: OverlappingPairCache = HashedOverlappingPairCache() // Pair cache

    var predictedFrames: Double = 2.0 // Frames predicted
    var stageCurrent: Int = 0// Current stage
    var fixedUpdatesPerFrame: Int = 1 // % of fixed updates per frame
    var dynamicUpdatesPerFrame: Int = 1 // % of dynamic updates per frame
    var parseId: Int = 0 // Parse id
    var nextUId: Int = 0 // Gen id

    fun collide(dispatcher: Dispatcher) {
        //SPC(m_profiling.m_total);

        // optimize:

        dynamicSet.optimizeIncremental(1 + (dynamicSet.leaves * dynamicUpdatesPerFrame) / 100)
        staticSet.optimizeIncremental(1 + (staticSet.leaves * fixedUpdatesPerFrame) / 100)

        // dynamic -> fixed set:
        stageCurrent = (stageCurrent + 1) % STAGE_COUNT
        var current = stageRoots[stageCurrent]
        if (current != null) {
            val collider = DbvtTreeCollider(this)
            do {
                val next = current!!.link1
                stageRoots[current.stage] = listRemove(current, stageRoots[current.stage])
                stageRoots[STAGE_COUNT] = listAppend(current, stageRoots[STAGE_COUNT])
                Dbvt.collideTT(staticSet.root, current.leaf, collider)
                dynamicSet.remove(current.leaf!!)
                current.leaf = staticSet.insert(current.aabb, current)
                current.stage = STAGE_COUNT
                current = next
            } while (current != null)
        }

        // collide dynamics:
        val collider = DbvtTreeCollider(this)
        Dbvt.collideTT(dynamicSet.root, staticSet.root, collider) // static-dynamic
        Dbvt.collideTT(dynamicSet.root, dynamicSet.root, collider) // dynamic-dynamic

        collideCleanup(dispatcher)
        parseId++
    }

    private fun collideCleanup(dispatcher: Dispatcher) {
        val pairs = overlappingPairCache.overlappingPairs
        var i = 0
        var ni = pairs.size
        while (i < ni) {
            val p = pairs[i]!!
            var pa = p.proxy0 as DbvtProxy
            var pb = p.proxy1 as DbvtProxy
            if (!pa.aabb.testAABB(pb.aabb)) {
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
        collisionFilter: Int, dispatcher: Dispatcher, multiSapProxy: Any?
    ): BroadphaseProxy {
        val proxy = DbvtProxy(userPtr, collisionFilter)
        proxy.aabb.setMin(aabbMin)
        proxy.aabb.setMax(aabbMax)
        proxy.leaf = dynamicSet.insert(proxy.aabb, proxy)
        proxy.stage = stageCurrent
        proxy.uid = ++nextUId
        stageRoots[stageCurrent] = listAppend(proxy, stageRoots[stageCurrent])
        return (proxy)
    }

    override fun destroyProxy(proxy: BroadphaseProxy, dispatcher: Dispatcher) {
        val proxy = proxy as DbvtProxy
        if (proxy.stage == STAGE_COUNT) {
            staticSet.remove(proxy.leaf!!)
        } else {
            dynamicSet.remove(proxy.leaf!!)
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
        val aabb = Stack.newAabb()
        aabb.setMin(aabbMin)
        aabb.setMax(aabbMax)

        if (proxy.stage == STAGE_COUNT) {
            // fixed -> dynamic set
            staticSet.remove(proxy.leaf!!)
            proxy.leaf = dynamicSet.insert(aabb, proxy)
        } else {
            // dynamic set:
            if (proxy.leaf!!.bounds.testAABB(aabb)) { /* Moving				*/
                val delta = Stack.newVec() // this.center - proxy.center
                aabbMin.add(aabbMax, delta).mul(0.5)
                delta.sub(proxy.aabb.getCenter(Stack.newVec()))
                //#ifdef DBVT_BP_MARGIN
                delta.mul(predictedFrames)
                dynamicSet.update(proxy.leaf!!, aabb, delta, DBVT_BP_MARGIN)
                Stack.subVec(2)
                //#else
                //m_sets[0].update(proxy->leaf,aabb,delta*m_predictedframes);
                //#endif
            } else {
                // teleporting:
                dynamicSet.update(proxy.leaf!!, aabb)
            }
        }

        stageRoots[proxy.stage] = listRemove(proxy, stageRoots[proxy.stage])
        proxy.aabb.set(aabb)
        proxy.stage = stageCurrent
        stageRoots[stageCurrent] = listAppend(proxy, stageRoots[stageCurrent])
        Stack.subAabb(1)
    }

    override fun calculateOverlappingPairs(dispatcher: Dispatcher) {
        collide(dispatcher)
    }

    override fun getBroadphaseAabb(aabbMin: Vector3d, aabbMax: Vector3d) {
        val bounds = Stack.newAabb()
        if (!dynamicSet.isEmpty) {
            if (!staticSet.isEmpty) {
                dynamicSet.root!!.bounds.union(staticSet.root!!.bounds, bounds)
            } else {
                bounds.set(dynamicSet.root!!.bounds)
            }
        } else if (!staticSet.isEmpty) {
            bounds.set(staticSet.root!!.bounds)
        } else {
            bounds.set(0.0, 0.0, 0.0)
        }
        bounds.getMin(aabbMin)
        bounds.getMax(aabbMax)
        Stack.subAabb(1)
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
