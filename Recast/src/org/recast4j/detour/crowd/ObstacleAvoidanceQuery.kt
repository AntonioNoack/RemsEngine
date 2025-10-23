/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.crowd

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import org.joml.Vector3f
import org.recast4j.Vectors
import org.recast4j.detour.crowd.debug.ObstacleAvoidanceDebugData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ObstacleAvoidanceQuery(maxNumCircles: Int, maxNumSegments: Int) {

    private lateinit var params: ObstacleAvoidanceParams
    private var invHorizTime = 0f
    private var invMaxVelocity = 0f

    val circles = Array(maxNumCircles) { ObstacleCircle() }
    private val maxCircles get() = circles.size

    val segments = Array(maxNumSegments) { ObstacleSegment() }
    private val maxNumSegments get() = segments.size

    var numCircles = 0
    var numSegments = 0

    fun reset() {
        numCircles = 0
        numSegments = 0
    }

    fun addCircle(position: Vector3f, radius: Float, actualVelocity: Vector3f, desiredVelocity: Vector3f) {
        if (numCircles >= maxCircles) return
        val circle = circles[numCircles++]
        circle.position.set(position)
        circle.radius = radius
        circle.actualVelocity.set(actualVelocity)
        circle.desiredVelocity.set(desiredVelocity)
    }

    fun addSegment(segmentStart: Vector3f, segmentEnd: Vector3f) {
        if (numSegments >= maxNumSegments) return
        val segment = segments[numSegments++]
        segment.segmentStart.set(segmentStart)
        segment.segmentEnd.set(segmentEnd)
    }

    private fun prepare(position: Vector3f, desiredVelocity: Vector3f) {
        // Prepare obstacles
        val deltaVelocity = Vector3f()
        val origin = Vector3f()
        for (i in 0 until numCircles) {
            val circle = circles[i]

            // Side
            circle.position.sub(position, circle.dp).normalize()
            circle.desiredVelocity.sub(desiredVelocity, deltaVelocity)
            val a = Vectors.triArea2D(origin, circle.dp, deltaVelocity)
            if (a < 0.01f) {
                circle.np.x = -circle.dp.z
                circle.np.z = circle.dp.x
            } else {
                circle.np.x = circle.dp.z
                circle.np.z = -circle.dp.x
            }
        }
        for (i in 0 until numSegments) {
            val segment = segments[i]

            // Precalculate if the agent is really close to the segment.
            val epsilon = 0.01f
            val first = Vectors.distancePtSegSqr2DFirst(position, segment.segmentStart, segment.segmentEnd)
            segment.isTouchedByAgent = first < epsilon * epsilon
        }
    }

    fun sweepCircleCircle(
        center0: Vector3f, radius0: Float, velocity: Vector3f,
        center1: Vector3f, radius1: Float
    ): SweepCircleCircleResult? {
        val epsilon = 0.0001f
        val sx = center1.x - center0.x
        val sz = center1.z - center0.z
        val radiusSum = radius0 + radius1
        val c = (sx * sx + sz * sz) - radiusSum * radiusSum
        var velSq = velocity.lengthXZSquared()
        if (velSq < epsilon) return null // not moving
        // Overlap, calc time to exit.
        val b = velocity.x * sx + velocity.z * sz
        val d = b * b - velSq * c
        if (d < 0f) return null // no intersection.
        velSq = 1f / velSq
        val rd = sqrt(d)
        return SweepCircleCircleResult((b - rd) * velSq, (b + rd) * velSq)
    }

    fun intersectRaySegment(
        rayOrigin: Vector3f, rayDirection: Vector3f,
        segmentStart: Vector3f, segmentEnd: Vector3f
    ): Float {
        val vx = segmentEnd.x - segmentStart.x
        val vz = segmentEnd.z - segmentStart.z
        val wx = rayOrigin.x - segmentStart.x
        val wz = rayOrigin.z - segmentStart.z
        var len = (rayDirection.z * vx - rayDirection.x * vz)
        if (abs(len) < 1e-6f) return -1f
        len = 1f / len
        val paramT = (vz * wx - vx * wz) * len
        if (paramT < 0 || paramT > 1) return -1f
        val paramS = (rayDirection.z * wx - rayDirection.x * wz) * len
        return if (paramS < 0 || paramS > 1) -1f else paramT
    }

    /**
     * Calculate the collision penalty for a given velocity vector
     *
     * @param minPenalty threshold penalty for early out
     */
    private fun processSample(
        sampledVelocity: Vector3f, cs: Float, position: Vector3f, radius: Float,
        actualVelocity: Vector3f, desiredVelocity: Vector3f,
        minPenalty: Float, debug: ObstacleAvoidanceDebugData?
    ): Float {
        // penalty for straying away from the desired and current velocities
        val desiredVelocityPenalty =
            params.weightDesiredVelocity * (sampledVelocity.distanceXZ(desiredVelocity) * invMaxVelocity)
        val currentVelocityPenalty =
            params.weightActualVelocity * (sampledVelocity.distanceXZ(actualVelocity) * invMaxVelocity)

        // find the threshold hit time to bail out based on the early out penalty
        // (see how the penalty is calculated below to understnad)
        val minPen = minPenalty - desiredVelocityPenalty - currentVelocityPenalty
        val tThreshold = (params.weightToi / minPen - 0.1f) * params.horizTime
        if (tThreshold - params.horizTime > -Float.MIN_VALUE) return minPenalty // already too much

        // Find min time of impact and exit amongst all obstacles.
        var tmin = params.horizTime
        var side = 0f
        var numSides = 0

        val vab = Vector3f()
        for (i in 0 until numCircles) {
            val cir = circles[i]

            // RVO
            sampledVelocity.mul(2f, vab)
            vab.sub(actualVelocity).sub(cir.actualVelocity)

            // Side
            side += clamp(min(Vectors.dot2D(cir.dp, vab) * 0.5f + 0.5f, Vectors.dot2D(cir.np, vab) * 2), 0f, 1f)
            numSides++
            val circleResult = sweepCircleCircle(position, radius, vab, cir.position, cir.radius) ?: continue
            var yMin = circleResult.yMin
            val yMax = circleResult.yMax

            // Handle overlapping obstacles.
            if (yMin < 0f && yMax > 0f) {
                // Avoid more when overlapped.
                yMin = -yMin * 0.5f
            }
            if (yMin >= 0f) {
                // The closest obstacle is somewhere ahead of us, keep track of nearest obstacle.
                if (yMin < tmin) {
                    tmin = yMin
                    if (tmin < tThreshold) return minPenalty
                }
            }
        }
        val snorm = Vector3f()
        for (i in 0 until numSegments) {
            val segment = segments[i]
            var htmin = if (segment.isTouchedByAgent) {
                // Special case when the agent is very close to the segment.
                segment.segmentEnd.sub(segment.segmentStart, snorm)
                snorm.set(-snorm.z, 0f, snorm.x)
                // If the velocity is pointing towards the segment, no collision.
                if (Vectors.dot2D(snorm, sampledVelocity) < 0f) continue
                // Else immediate collision.
                0f
            } else {
                val ires = intersectRaySegment(position, sampledVelocity, segment.segmentStart, segment.segmentEnd)
                if (ires < 0f) continue
                ires
            }

            // Avoid less when facing walls.
            htmin *= 2f

            // The closest obstacle is somewhere ahead of us, keep track of nearest obstacle.
            if (htmin < tmin) {
                tmin = htmin
                if (tmin < tThreshold) {
                    return minPenalty
                }
            }
        }

        // Normalize side bias, to prevent it dominating too much.
        if (numSides != 0) side /= numSides.toFloat()
        val preferredSidePenalty = params.weightSide * side
        val collisionTimePenalty = params.weightToi * (1f / (0.1f + tmin * invHorizTime))
        val penalty = desiredVelocityPenalty + currentVelocityPenalty + preferredSidePenalty + collisionTimePenalty
        // Store different penalties for debug viewing
        debug?.addSample(
            sampledVelocity, cs, penalty,
            desiredVelocityPenalty, currentVelocityPenalty,
            preferredSidePenalty, collisionTimePenalty
        )
        return penalty
    }

    fun sampleVelocityGrid(
        position: Vector3f, radius: Float, maxVelocity: Float, actualVelocity: Vector3f, desiredVelocity: Vector3f,
        params: ObstacleAvoidanceParams, debug: ObstacleAvoidanceDebugData?
    ): Pair<Int, Vector3f> {
        prepare(position, desiredVelocity)
        this.params = params
        invHorizTime = 1f / this.params.horizTime
        invMaxVelocity = if (maxVelocity > 0) 1f / maxVelocity else Float.MAX_VALUE
        val newVelocity = Vector3f()
        debug?.reset()
        val cvx = desiredVelocity.x * params.velocityBias
        val cvz = desiredVelocity.z * params.velocityBias
        val cs = maxVelocity * 2 * (1 - params.velocityBias) / (params.gridSize - 1)
        val half = (params.gridSize - 1) * cs * 0.5f
        var minPenalty = Float.MAX_VALUE
        var numValidSamples = 0
        for (y in 0 until params.gridSize) {
            for (x in 0 until params.gridSize) {
                val vcand = Vector3f(cvx + x * cs - half, 0f, cvz + y * cs - half)
                val maxVelocity2 = maxVelocity + cs / 2
                if (vcand.x * vcand.x + vcand.z * vcand.z > maxVelocity2 * maxVelocity2) continue
                val penalty =
                    processSample(vcand, cs, position, radius, actualVelocity, desiredVelocity, minPenalty, debug)
                numValidSamples++
                if (penalty < minPenalty) {
                    minPenalty = penalty
                    newVelocity.set(vcand)
                }
            }
        }
        return Pair(numValidSamples, newVelocity)
    }

    // vector normalization that ignores the y-component.
    fun dtNormalize2D(v: FloatArray) {
        var len = hypot(v[0], v[2])
        if (len == 0f) return
        len = 1f / len
        v[0] *= len
        v[2] *= len
    }

    fun sampleVelocityAdaptive(
        position: Vector3f, radius: Float, maxVelocity: Float, actualVelocity: Vector3f,
        desiredVelocity: Vector3f, params: ObstacleAvoidanceParams, debug: ObstacleAvoidanceDebugData?
    ): Pair<Int, Vector3f> {
        prepare(position, desiredVelocity)
        this.params = params
        invHorizTime = 1f / params.horizTime
        invMaxVelocity = if (maxVelocity > 0) 1f / maxVelocity else Float.MAX_VALUE
        val nvel = Vector3f()
        debug?.reset()

        // Build sampling pattern aligned to desired velocity.
        val pattern = FloatArray((DT_MAX_PATTERN_DIVS * DT_MAX_PATTERN_RINGS + 1) * 2)
        val numDivs = params.numAdaptiveDivs
        val numRings = params.numAdaptiveRings
        val depth = params.adaptiveDepth
        val nd: Int = clamp(numDivs, 1, DT_MAX_PATTERN_DIVS)
        val nr: Int = clamp(numRings, 1, DT_MAX_PATTERN_RINGS)
        val deltaAngle = 1f / nd * DT_PI * 2
        val ca = cos(deltaAngle)
        val sa = sin(deltaAngle)

        // desired direction
        val desiredDirection = floatArrayOf(
            desiredVelocity.x, desiredVelocity.y, desiredVelocity.z,
            0f, 0f, 0f
        )
        dtNormalize2D(desiredDirection)
        val rotated = Vector3f(desiredDirection).rotateY(deltaAngle * 0.5f) // rotated by da/2
        desiredDirection[3] = rotated.x
        desiredDirection[4] = rotated.y
        desiredDirection[5] = rotated.z
        var numSamples = 1
        for (j in 0 until nr) {
            val r = (nr - j).toFloat() / nr.toFloat()
            pattern[numSamples * 2] = desiredDirection[(j % 2) * 3] * r
            pattern[numSamples * 2 + 1] = desiredDirection[(j % 2) * 3 + 2] * r
            var last1 = numSamples * 2
            var last2 = last1
            numSamples++
            var i = 1
            while (i < nd - 1) {

                // get next point on the "right" (rotate CW)
                pattern[numSamples * 2] = pattern[last1] * ca + pattern[last1 + 1] * sa
                pattern[numSamples * 2 + 1] = -pattern[last1] * sa + pattern[last1 + 1] * ca
                // get next point on the "left" (rotate CCW)
                pattern[numSamples * 2 + 2] = pattern[last2] * ca - pattern[last2 + 1] * sa
                pattern[numSamples * 2 + 3] = pattern[last2] * sa + pattern[last2 + 1] * ca
                last1 = numSamples * 2
                last2 = last1 + 2
                numSamples += 2
                i += 2
            }
            if (nd and 1 == 0) {
                pattern[numSamples * 2 + 2] = pattern[last2] * ca - pattern[last2 + 1] * sa
                pattern[numSamples * 2 + 3] = pattern[last2] * sa + pattern[last2 + 1] * ca
                numSamples++
            }
        }

        // Start sampling.
        var cr = maxVelocity * (1f - params.velocityBias)
        val res = Vector3f(desiredVelocity.x * params.velocityBias, 0f, desiredVelocity.z * params.velocityBias)
        var numProcessedSamples = 0
        repeat(depth) {
            var minPenalty = Float.MAX_VALUE
            val bVel = Vector3f()
            for (i in 0 until numSamples) {
                val vcand = Vector3f(res.x + pattern[i * 2] * cr, 0f, res.z + pattern[i * 2 + 1] * cr)
                if (sq(vcand.x) + sq(vcand.z) > sq(maxVelocity + 0.001f)) continue
                val penalty = processSample(
                    vcand, cr / 10, position, radius,
                    actualVelocity, desiredVelocity, minPenalty, debug
                )
                numProcessedSamples++
                if (penalty < minPenalty) {
                    minPenalty = penalty
                    bVel.set(vcand)
                }
            }
            res.set(bVel)
            cr *= 0.5f
        }
        nvel.set(res)
        return Pair(numProcessedSamples, nvel)
    }

    companion object {
        /** Max number of adaptive divs.  */
        private const val DT_MAX_PATTERN_DIVS = 32

        /** Max number of adaptive rings.  */
        private const val DT_MAX_PATTERN_RINGS = 4
        const val DT_PI = 3.1415927f
    }
}