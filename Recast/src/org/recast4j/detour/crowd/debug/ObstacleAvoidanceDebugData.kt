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
package org.recast4j.detour.crowd.debug

import org.joml.Vector3f
import org.recast4j.Vectors.clamp
import kotlin.math.max
import kotlin.math.min

class ObstacleAvoidanceDebugData(capacity: Int) {

    var size = 0

    val velocities = FloatArray(3 * capacity)
    val sizes = FloatArray(capacity)
    val penalties = FloatArray(capacity)
    val desiredVelocityPenalties = FloatArray(capacity)
    val currentVelocityPenalties = FloatArray(capacity)
    val preferredSidePenalties = FloatArray(capacity)
    val collisionTimePenalties = FloatArray(capacity)

    fun reset() {
        size = 0
    }

    val capacity get() = sizes.size

    fun normalizeArray(arr: FloatArray, n: Int) {
        // Normalize penalty range.
        var minValue = Float.MAX_VALUE
        var maxValue = -Float.MAX_VALUE
        for (i in 0 until n) {
            minValue = min(minValue, arr[i])
            maxValue = max(maxValue, arr[i])
        }
        val range = maxValue - minValue
        val scale = if (range > 0.001f) 1f / range else 1f
        for (i in 0 until n) {
            arr[i] = clamp((arr[i] - minValue) * scale, 0f, 1f)
        }
    }

    fun normalizeSamples() {
        normalizeArray(penalties, size)
        normalizeArray(desiredVelocityPenalties, size)
        normalizeArray(currentVelocityPenalties, size)
        normalizeArray(preferredSidePenalties, size)
        normalizeArray(collisionTimePenalties, size)
    }

    fun addSample(
        velocity: Vector3f,
        sizeI: Float, penalty: Float,
        desiredVelocityPenalty: Float,
        currentVelocityPenalty: Float,
        preferredSidePenalty: Float,
        collisionTimePenalty: Float
    ) {
        if (size >= capacity) return
        velocity.get(velocities, size * 3)
        sizes[size] = sizeI
        penalties[size] = penalty
        desiredVelocityPenalties[size] = desiredVelocityPenalty
        currentVelocityPenalties[size] = currentVelocityPenalty
        preferredSidePenalties[size] = preferredSidePenalty
        collisionTimePenalties[size] = collisionTimePenalty
        size++
    }
}