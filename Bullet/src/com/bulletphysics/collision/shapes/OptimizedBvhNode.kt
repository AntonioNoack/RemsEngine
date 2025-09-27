package com.bulletphysics.collision.shapes

import java.io.Serializable
import org.joml.Vector3d

/**
 * OptimizedBvhNode contains both internal and leaf node information.
 *
 * @author jezek2
 */
class OptimizedBvhNode : Serializable {

    @JvmField
	val aabbMinOrg = Vector3d()
    @JvmField
	val aabbMaxOrg = Vector3d()

    @JvmField
	var escapeIndex: Int = 0

    // for child nodes
	@JvmField
	var subPart: Int = 0

    @JvmField
	var triangleIndex: Int = 0

    fun set(n: OptimizedBvhNode) {
        aabbMinOrg.set(n.aabbMinOrg)
        aabbMaxOrg.set(n.aabbMaxOrg)
        escapeIndex = n.escapeIndex
        subPart = n.subPart
        triangleIndex = n.triangleIndex
    }
}
