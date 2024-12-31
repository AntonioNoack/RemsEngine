package me.anno.engine.raycast

import org.joml.AABBi
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Traces a ray through a cubic voxel grid without triangle/mesh-overhead.
 * */
object BlockTracing {

    const val CUSTOM_SKIP_DISTANCE = 1.0
    const val AIR_BLOCK = 0.5
    const val AIR_SKIP_NORMAL = 0.0
    const val SOLID_BLOCK = -1.0

    /**
     * Result:
     *  1. &gt;= CUSTOM_SKIP_DISTANCE -&gt; skip that many blocks
     *  2. AIR_BLOCK: skip one block, and set normal
     *  3. AIR_SKIP_NORMAL: skip one block, don't set normal
     *  4. SOLID_BLOCK: hit something solid, stop tracing
     * */
    fun interface BlockChecker {
        fun getSkipDistance(x: Int, y: Int, z: Int): Double
    }

    private fun max1(a: Double, b: Double): Double {
        return if (a < 0.0) b else if (b < 0.0) a
        else max(a, b)
    }

    fun blockTrace(
        query: RayQuery,
        maxSteps: Int,
        bounds: AABBi,
        hitBlock: BlockChecker,
    ): Boolean {

        if (bounds.isEmpty()) {
            return false
        }

        val dir = query.direction
        val localStart = query.start
        // prevent divisions by zero
        if (abs(dir.x) < 1e-15) dir.x = 1e-15
        if (abs(dir.y) < 1e-15) dir.y = 1e-15
        if (abs(dir.z) < 1e-15) dir.z = 1e-15
        val invDirX = 1.0 / dir.x
        val invDirY = 1.0 / dir.y
        val invDirZ = 1.0 / dir.z
        // start from camera, and project onto front sides
        // for proper rendering, we need to use the backsides, and therefore we project the ray from the back onto the front
        val dirSignX = sign(dir.x).toInt()
        val dirSignY = sign(dir.y).toInt()
        val dirSignZ = sign(dir.z).toInt()
        val ds0X = dirSignX * .5 + .5
        val ds0Y = dirSignY * .5 + .5
        val ds0Z = dirSignZ * .5 + .5

        val e = 0.0001
        val dtf3X = ((if (dir.x > 0.0) bounds.minX + e else bounds.maxX - e) - localStart.x) * invDirX
        val dtf3Y = ((if (dir.y > 0.0) bounds.minY + e else bounds.maxY - e) - localStart.y) * invDirY
        val dtf3Z = ((if (dir.z > 0.0) bounds.minZ + e else bounds.maxZ - e) - localStart.z) * invDirZ

        val dtf1 = max1(dtf3X, max1(dtf3Y, dtf3Z))
        var dist = max(dtf1, 0.0)
        val startX = localStart.x + dist * dir.x
        val startY = localStart.y + dist * dir.y
        val startZ = localStart.z + dist * dir.z
        var blockPosX = floor(startX).toInt()
        var blockPosY = floor(startY).toInt()
        var blockPosZ = floor(startZ).toInt()
        if (!(blockPosX in bounds.minX..bounds.maxX &&
                    blockPosY in bounds.minY..bounds.maxY &&
                    blockPosZ in bounds.minZ..bounds.maxZ)
        ) return false
        var dist3X = (ds0X + blockPosX - localStart.x) * invDirX
        var dist3Y = (ds0Y + blockPosY - localStart.y) * invDirY
        var dist3Z = (ds0Z + blockPosZ - localStart.z) * invDirZ
        val invUStepX = dirSignX * invDirX
        val invUStepY = dirSignY * invDirY
        val invUStepZ = dirSignZ * invDirZ
        var lastNormal = if (dtf3Z == dtf1) 2 else if (dtf3Y == dtf1) 1 else 0
        var hitSomething = false
        for (i in 0 until maxSteps) {
            val nextDist = min(dist3X, min(dist3Y, dist3Z))
            val skippingDist = hitBlock.getSkipDistance(blockPosX, blockPosY, blockPosZ)
            if (skippingDist >= CUSTOM_SKIP_DISTANCE) {
                val dist1 = dist + skippingDist
                blockPosX = floor(localStart.x + dir.x * dist1).toInt()
                blockPosY = floor(localStart.y + dir.y * dist1).toInt()
                blockPosZ = floor(localStart.z + dir.z * dist1).toInt()
                // break if out of bounds
                if (blockPosX in bounds.minX..bounds.maxX &&
                    blockPosY in bounds.minY..bounds.maxY &&
                    blockPosZ in bounds.minZ..bounds.maxZ
                ) {
                    // continue
                    dist3X = (ds0X + blockPosX - localStart.x) * invDirX
                    dist3Y = (ds0Y + blockPosY - localStart.y) * invDirY
                    dist3Z = (ds0Z + blockPosZ - localStart.z) * invDirZ
                } else break
            } else if (skippingDist >= AIR_SKIP_NORMAL) {
                val setNormal = skippingDist >= AIR_BLOCK
                if (nextDist == dist3X) {
                    blockPosX += dirSignX
                    dist3X += invUStepX
                    if (setNormal) lastNormal = 0
                    if (blockPosX !in bounds.minX..bounds.maxX) break
                } else if (nextDist == dist3Y) {
                    blockPosY += dirSignY
                    dist3Y += invUStepY
                    if (setNormal) lastNormal = 1
                    if (blockPosY !in bounds.minY..bounds.maxY) break
                } else {
                    blockPosZ += dirSignZ
                    dist3Z += invUStepZ
                    if (setNormal) lastNormal = 2
                    if (blockPosZ !in bounds.minZ..bounds.maxZ) break
                }
                dist = nextDist
                if (dist > query.result.distance) {
                    break // we're farther than any previous hit
                }
            } else {
                hitSomething = true
                break
            }
        }
        return if (hitSomething && dist < query.result.distance) {
            query.result.distance = dist
            dir.mulAdd(dist, localStart, query.result.positionWS)
            val normal = query.result.geometryNormalWS
            when (lastNormal) {
                0 -> normal.set(-dirSignX.toDouble(), 0.0, 0.0)
                1 -> normal.set(0.0, -dirSignY.toDouble(), 0.0)
                else -> normal.set(0.0, 0.0, -dirSignZ.toDouble())
            }
            true
        } else false
    }
}