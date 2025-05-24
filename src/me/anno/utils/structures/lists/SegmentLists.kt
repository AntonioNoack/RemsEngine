package me.anno.utils.structures.lists

import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue
import me.anno.utils.callbacks.VtoD
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

object SegmentLists {

    fun <V> segmentDistance(
        index0: Double, index1: Double,
        segments: List<V>, getSegmentLength: VtoD<V>,
        requestStartSegments: () -> Int,
        requestEndSegments: () -> Int
    ): Double {

        var min = min(index0, index1)
        var max = max(index0, index1)

        val sign = sign(index1 - index0)

        while (min < 0.0) {
            val added = requestStartSegments()
            if (added <= 0) break
            val added1 = added.toDouble()
            min += added1
            max += added1
        }

        while (max > segments.size) {
            val added = requestEndSegments()
            if (added <= 0) break
        }

        val minI = clamp(ceil(min).toIntOr() - 1, 0, segments.lastIndex)
        val maxI = clamp(floor(max).toIntOr(), 0, segments.lastIndex)
        if (minI >= maxI) {
            // sum left+right
            val segmentLength = getSegmentLength.call(segments[minI])
            return sign * (max - min) * segmentLength
        }

        // else sum left + middle[] + right
        val left = (1.0 - (min - minI)) * getSegmentLength.call(segments[minI])
        val right = (max - maxI) * getSegmentLength.call(segments[maxI])
        var sum = left + right
        for (i in minI + 1 until maxI) {
            sum += getSegmentLength.call(segments[i])
        }
        return sign * sum
    }

    /**
     * Given a list of segments with different lengths, add a step to a provided index.
     * */
    fun <V> segmentStep(
        index: Double, delta: Double,
        segments: List<V>, getSegmentLength: VtoD<V>,
        requestStartSegments: () -> Int,
        requestEndSegments: () -> Int,
        clampOutput: Boolean
    ): Double {
        var delta = delta
        var index = index
        when {
            delta < 0.0 -> {
                while (true) {
                    var railIndex = clamp(ceil(index).toIntOr() - 1, 0, segments.lastIndex)
                    val segmentLength = getSegmentLength.call(segments[railIndex])
                    assertTrue(segmentLength > 0.0)

                    val fract = index - railIndex
                    val maxDelta = fract * segmentLength

                    // simple case
                    if (-delta <= maxDelta) {
                        return stepSimple(index, delta, segmentLength)
                    }

                    // handle end
                    if (railIndex == 0) {
                        val added = requestStartSegments()
                        if (added > 0) {
                            // increment index, because a segment was added
                            railIndex += added
                        } else {
                            // reached end point at start
                            return if (clampOutput) 0.0
                            else stepSimple(index, delta, segmentLength)
                        }
                    }

                    delta += maxDelta
                    index = railIndex.toDouble()
                }
            }
            delta > 0.0 -> {
                while (true) {
                    val railIndex = clamp(floor(index).toIntOr(), 0, segments.lastIndex)
                    val segmentLength = getSegmentLength.call(segments[railIndex])
                    assertTrue(segmentLength > 0.0)

                    val fract = index - railIndex
                    val maxDelta = (1.0 - fract) * segmentLength

                    // simple case
                    if (delta <= maxDelta) {
                        return stepSimple(index, delta, segmentLength)
                    }

                    // handle end
                    if (railIndex == segments.lastIndex) {
                        if (requestEndSegments() == 0) {
                            // reached end point at end
                            return if (clampOutput) (segments.size + 1).toDouble()
                            else stepSimple(index, delta, segmentLength)
                        }
                        // else index is fine, doesn't need to be incremented
                    }

                    delta -= maxDelta
                    index = (railIndex + 1).toDouble()
                }
            }
            delta.isInfinite() -> return delta
            else -> return index
        }
    }

    private fun stepSimple(index: Double, delta: Double, segmentLength: Double): Double {
        return index + delta / segmentLength
    }
}