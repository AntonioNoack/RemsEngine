package com.bulletphysics.collision.shapes

import java.io.Serializable

/**
 * BvhSubtreeInfo provides info to gather a subtree of limited size.
 *
 * @author jezek2
 */
class BvhSubtreeInfo : Serializable {
    /*unsigned*/val quantizedAabbMin: ShortArray = ShortArray(3)
    /*unsigned*/val quantizedAabbMax: ShortArray = ShortArray(3)

    // points to the root of the subtree
	@JvmField
	var rootNodeIndex: Int = 0
    @JvmField
	var subtreeSize: Int = 0

    fun setAabbFromQuantizeNode(nodes: QuantizedBvhNodes, nodeId: Int) {
        quantizedAabbMin[0] = nodes.getQuantizedAabbMin(nodeId, 0).toShort()
        quantizedAabbMin[1] = nodes.getQuantizedAabbMin(nodeId, 1).toShort()
        quantizedAabbMin[2] = nodes.getQuantizedAabbMin(nodeId, 2).toShort()
        quantizedAabbMax[0] = nodes.getQuantizedAabbMax(nodeId, 0).toShort()
        quantizedAabbMax[1] = nodes.getQuantizedAabbMax(nodeId, 1).toShort()
        quantizedAabbMax[2] = nodes.getQuantizedAabbMax(nodeId, 2).toShort()
    }
}
