package me.anno.maths.noise

import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The Simple Phacelle Noise function produces a stripe pattern aligned with the input vector.
 * The name Phacelle is a portmanteau of phase and cell, since the function produces a phase by
 * interpolating cosine and sine waves from multiple cells.
 *  - p is the input point being evaluated.
 *  - normDir is the direction of the stripes at this point. It must be a normalized vector.
 *  - freq is the freqency of the stripes within each cell. It's best to keep it close to 1.0, as
 *    high values will produce distortions and other artifacts.
 *  - offset is the phase offset of the stripes, where 1.0 is a full cycle.
 *  - normalization is the degree of normalization applied, between 0 and 1. With e.g. a value of
 *    0.4, raw output with a magnitude below 0.6 won't get fully normalized to a magnitude of 1.0.
 * Phacelle Noise function copyright (c) 2025 Rune Skovbo Johansen
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * */
class PhacelleNoise(
    val hash: PhacelleHash
) {

    operator fun get(
        px: Float, py: Float,
        normDirX: Float, normDirY: Float,
        freq: Float, offset: Float, normalization: Float,
        dst: Vector4f
    ): Vector4f {

        // Get a vector orthogonal to the input direction, with a
        // magnitude proportional to the frequency of the stripes.
        val sideDirX = -TAUf * freq * normDirY
        val sideDirY = +TAUf * freq * normDirX
        val offset = offset * TAUf

        // Iterate over 4x4 cells, calculating a stripe pattern for each and blending between them.
        // pInt is the integer part of the current coordinate p, pFrac is the remainder.
        //
        // o   o   o   o
        //
        // o   o   o   o
        //       p
        // o   i   o   o
        //
        // o   o   o   o
        //
        // p: current coordinate    i: integer part of p    o: grid points for 4x4 cells
        val pIntX = floor(px)
        val pIntY = floor(py)
        val pFracX = px - pIntX
        val pFracY = py - pIntY

        var phaseDirX = 0f
        var phaseDirY = 0f
        var weightSum = 0f

        val randomOffset = JomlPools.vec2f.create()
        for (i in -1..2) {
            for (j in -1..2) {
                val gridOffsetX = i.toFloat()
                val gridOffsetY = j.toFloat()

                // Calculate a cell point by starting off with a point in the integer grid.
                val gridPointX = pIntX + gridOffsetX
                val gridPointY = pIntY + gridOffsetY

                // Calculate a random offset for the cell point between -0.5 and 0.5 on each axis.
                val randomOffset = hash[gridPointX, gridPointY, randomOffset]

                // The final cell point (we don't store it) is the gridPoint plus the randomOffset.
                // Calculate a vector representing the input point relative to this cell point:
                // p - (gridPoint + randomOffset)
                // = (pFrac + pInt) - ((pInt + gridOffset) + randomOffset)
                // = pFrac + pInt - pInt - gridOffset - randomOffset
                // = pFrac - gridOffset - randomOffset
                val vectorFromCellPointX = pFracX - gridOffsetX - randomOffset.x * 0.5f
                val vectorFromCellPointY = pFracY - gridOffsetY - randomOffset.y * 0.5f

                // Bell-shaped weight function which is 1 at dist 0 and nearly 0 at dist 1.5.
                // Due to the random offsets of up to 0.5, the closest a cell point not in the 4x4
                // grid can be to the current point p is 1.5 units away.
                val sqrDist = sq(vectorFromCellPointX, vectorFromCellPointY)
                var weight = exp(-sqrDist * 2f)

                // Subtract 0.01111 to make the function actually 0 at distance 1.5,
                // which avoids some (very subtle) grid line artifacts.
                weight = max(0f, weight - 0.01111f)

                // Keep track of the total sum of weights.
                weightSum += weight

                // The waveInput is a gradient which increases in value along sideDir. Its rate of
                // change is the freq times tau, due to the multiplier pre-applied to sideDir.
                val waveInput = vectorFromCellPointX * sideDirX + vectorFromCellPointY * sideDirY + offset

                // Add this cell's cosine and sine wave contributions to the interpolated value.
                phaseDirX += cos(waveInput) * weight
                phaseDirY += sin(waveInput) * weight
            }
        }

        // Get the raw interpolated value.
        val interpolatedX = phaseDirX / weightSum
        val interpolatedY = phaseDirY / weightSum

        // Interpret the value as a vector whose length represents the magnitude of both waves.
        var magnitude = hypot(interpolatedX, interpolatedY)

        // Apply a lower threshold to show small magnitudes we're going to fully normalize.
        magnitude = max(1f - normalization, magnitude)

        // Return a vector containing the normalized cosine and sine waves, as well as the direction
        // vector, which can be multiplied onto the sine to get the derivatives of the cosine.
        return dst.set(
            interpolatedX / magnitude,
            interpolatedY / magnitude,
            sideDirX, sideDirY
        )
    }
}