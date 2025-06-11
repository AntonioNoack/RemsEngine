package com.bulletphysics.collision.shapes

/**
 * Callback for operating with [OptimizedBvh].
 *
 * @author jezek2
 */
interface NodeOverlapCallback {
    fun processNode(subPart: Int, triangleIndex: Int)
}
