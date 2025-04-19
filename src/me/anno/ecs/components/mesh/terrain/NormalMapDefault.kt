package me.anno.ecs.components.mesh.terrain

import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

/**
 * Use this, when you don't know how to generate better normals.
 * Set width and height to 0, if heightMap isn't limited to your coordinates, e.g., if it is procedural.
 * */
class NormalMapDefault(
    val heightMap: HeightMap,
    cellSize: Float, flip: Boolean,
    val width: Int, val height: Int,
) : NormalMap {

    val normalizeY = (if (flip) -1f else +1f) * cellSize

    override fun get(xi: Int, zi: Int, dst: Vector3f) {

        val x0 = if (width > 0) max(xi - 1, 0) else xi - 1
        val z0 = if (height > 0) max(zi - 1, 0) else zi - 1

        val x1 = if (width > 0) min(xi + 1, width) else xi + 1
        val z1 = if (height > 0) min(zi + 1, height) else zi + 1

        val dx = (heightMap[x0, zi] - heightMap[x1, zi]) / (x1 - x0)
        val dz = (heightMap[xi, z0] - heightMap[xi, z1]) / (z1 - z0)

        dst.set(dx, normalizeY, dz).safeNormalize()
    }
}