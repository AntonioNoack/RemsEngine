package com.bulletphysics.collision.broadphase

class DbvtNode {

    @JvmField
    val volume: DbvtAabbMm = DbvtAabbMm()

    @JvmField
    var parent: DbvtNode? = null

    @JvmField
    var child0: DbvtNode? = null

    @JvmField
    var child1: DbvtNode? = null

    @JvmField
    var data: Any? = null

    val isLeaf: Boolean get() = child1 == null
    val isBranch: Boolean get() = child1 != null
    val isInternal: Boolean get() = !this.isLeaf
}