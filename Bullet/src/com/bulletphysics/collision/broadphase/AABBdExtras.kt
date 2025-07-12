package com.bulletphysics.collision.broadphase

import org.joml.AABBd
import org.joml.Vector3d
import kotlin.math.abs

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */

fun AABBd.addSignedMargin(e: Vector3d) {
    if (e.x > 0) {
        maxX += e.x
    } else {
        minX += e.x
    }

    if (e.y > 0) {
        maxY += e.y
    } else {
        minY += e.y
    }

    if (e.z > 0) {
        maxZ += e.z
    } else {
        minZ += e.z
    }
}

fun AABBd.proximity(other: AABBd): Double {
    return abs((minX + maxX) - (other.minX + other.maxX)) +
            abs((minY + maxY) - (other.minY + other.maxY)) +
            abs((minZ + maxZ) - (other.minZ + other.maxZ))
}
