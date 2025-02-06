/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour

import org.joml.Vector3f
import org.recast4j.FloatSubArray
import org.recast4j.Vectors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface PolygonByCircleConstraint {

    fun apply(
        polyVertices: FloatSubArray, circleCenter: Vector3f, radius: Float,
        tmp1: FloatSubArray, tmp2: FloatSubArray
    ): FloatSubArray?

    object NoOpPolygonByCircleConstraint : PolygonByCircleConstraint {
        override fun apply(
            polyVertices: FloatSubArray, circleCenter: Vector3f, radius: Float,
            tmp1: FloatSubArray, tmp2: FloatSubArray
        ) = polyVertices
    }

    /**
     * Calculate the intersection between a polygon and a circle. A dodecagon is used as an approximation of the circle.
     */
    object StrictPolygonByCircleConstraint : PolygonByCircleConstraint {
        override fun apply(
            polyVertices: FloatSubArray,
            circleCenter: Vector3f,
            radius: Float,
            tmp1: FloatSubArray,
            tmp2: FloatSubArray
        ): FloatSubArray? {
            val radiusSqr = radius * radius
            var outsideVertex = -1
            var pv = 0
            while (pv < polyVertices.size) {
                if (Vectors.dist2DSqr(circleCenter, polyVertices.data, pv) > radiusSqr) {
                    outsideVertex = pv
                    break
                }
                pv += 3
            }
            if (outsideVertex == -1) {
                // polygon inside circle
                return polyVertices
            }
            circle(circleCenter, radius, tmp1.data)
            val intersection = ConvexConvexIntersection.intersect(polyVertices, tmp1, tmp2)
            return if (intersection == null &&
                Vectors.pointInPolygon(circleCenter, polyVertices.data, polyVertices.size / 3)
            ) tmp1 // circle inside polygon
            else intersection
        }

        private fun circle(center: Vector3f, radius: Float, circle: FloatArray) {
            for (i in unitCircle.indices step 3) {
                circle[i] = unitCircle[i] * radius + center.x
                circle[i + 1] = center.y
                circle[i + 2] = unitCircle[i + 2] * radius + center.z
            }
        }

        const val CIRCLE_SEGMENTS = 12
        private val unitCircle = FloatArray(CIRCLE_SEGMENTS * 3)

        init {
            val da = (PI * 2 / CIRCLE_SEGMENTS).toFloat()
            for (i in 0 until CIRCLE_SEGMENTS) {
                val a = i * da
                unitCircle[3 * i] = cos(a)
                unitCircle[3 * i + 1] = 0f
                unitCircle[3 * i + 2] = -sin(a)
            }
        }

    }

}