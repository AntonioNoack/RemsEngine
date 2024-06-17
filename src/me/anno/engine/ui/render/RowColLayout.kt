package me.anno.engine.ui.render

import me.anno.maths.Maths.ceilDiv
import org.joml.Vector2i
import kotlin.math.abs
import kotlin.math.min

object RowColLayout {

    @JvmStatic
    private fun loss(numTiles: Int, numTilesY: Int, w: Int, h: Int): Float {
        val numTilesX = ceilDiv(numTiles, numTilesY)
        val total = numTilesY * numTilesX
        val wastedTiles = total - numTiles
        if (wastedTiles >= numTilesX || wastedTiles >= numTilesY) {
            // we're wasting whole rows / columns -> unacceptable
            return Float.POSITIVE_INFINITY
        }
        val wastedSpace = wastedTiles.toFloat() / numTiles
        val r0 = w * numTilesY
        val r1 = h * numTilesX
        val notSquareness = abs(r0 - r1).toFloat() / min(r0, r1)
        return notSquareness + wastedSpace
    }

    /**
     * O(n) algorithms to find a good tile layout to fill a w-x-h-sized space ->
     * should only be used for n < 100
     * */
    @JvmStatic
    fun findGoodTileLayout(numTiles: Int, w: Int, h: Int): Vector2i {
        var bestNumTilesY = 1
        var bestLoss = Float.POSITIVE_INFINITY
        for (numTilesY in 1..numTiles) {
            val lossI = loss(numTiles, numTilesY, w, h)
            if (lossI < bestLoss) {
                bestLoss = lossI
                bestNumTilesY = numTilesY
            }
        }
        val bestNumTilesX = ceilDiv(numTiles, bestNumTilesY)
        return Vector2i(bestNumTilesX, bestNumTilesY)
    }
}