package com.bulletphysics.dynamics

import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTree
import me.anno.utils.pooling.ObjectPool
import org.joml.Vector3d

class CollisionTree : OctTree<RigidBody>(MAX_NUM_CHILDREN) {

    override fun createChild(): KdTree<Vector3d, RigidBody> = treePool.create()
    override fun destroyChild(child: KdTree<Vector3d, RigidBody>) {
        treePool.destroy(child as CollisionTree)
    }

    override fun createList(): ArrayList<RigidBody> {
        return arrayPool.create()
    }

    override fun destroyList(list: ArrayList<RigidBody>) {
        list.clear()
        arrayPool.destroy(list)
    }

    override fun getMin(data: RigidBody): Vector3d = data.collisionAabbMin
    override fun getMax(data: RigidBody): Vector3d = data.collisionAabbMax

    companion object {

        private const val MAX_NUM_CHILDREN = 16

        // what is a good size? we need ~#entities/8
        private const val LIMIT = 32 shl 10

        private val treePool = ObjectPool(LIMIT) { CollisionTree() }
        private val arrayPool = ObjectPool(LIMIT) { ArrayList<RigidBody>(MAX_NUM_CHILDREN) }
    }
}