package me.anno.ecs.components.collider

import me.anno.maths.Maths
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

object SDFUtils {
    fun and2SDFs(deltaPos: Vector3f, roundness: Float): Float {
        val dx = deltaPos.x + roundness
        val dy = deltaPos.y + roundness
        val outside = Maths.length(max(dx, 0f), max(dy, 0f))
        val inside = min(max(dx, dy), 0f)
        return outside + inside - roundness
    }

    fun and3SDFs(deltaPos: Vector3f, roundness: Float): Float {
        val dx = deltaPos.x + roundness
        val dy = deltaPos.y + roundness
        val dz = deltaPos.z + roundness
        val outside = Maths.length(max(dx, 0f), max(dy, 0f), max(dz, 0f))
        val inside = min(max(dx, max(dy, dz)), 0f)
        return outside + inside - roundness
    }
}