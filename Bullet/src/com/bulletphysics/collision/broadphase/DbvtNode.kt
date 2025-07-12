package com.bulletphysics.collision.broadphase

import org.joml.AABBd

class DbvtNode {

    @JvmField
    var bounds = AABBd()

    @JvmField
    var parent: DbvtNode? = null

    @JvmField
    var child0: DbvtNode? = null

    @JvmField
    var child1: DbvtNode? = null

    @JvmField
    var data: DbvtProxy? = null

    val isLeaf: Boolean get() = child1 == null
    val isBranch: Boolean get() = child1 != null
    val isInternal: Boolean get() = !this.isLeaf
}