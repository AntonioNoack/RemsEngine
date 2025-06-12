package com.bulletphysics.dynamics

import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTree
import org.joml.Vector3d

class CollisionTree : OctTree<RigidBody>(16) {
    override fun createChild(): KdTree<Vector3d, RigidBody> = CollisionTree()
    override fun getMin(data: RigidBody): Vector3d = data.collisionAabbMin
    override fun getMax(data: RigidBody): Vector3d = data.collisionAabbMax
}