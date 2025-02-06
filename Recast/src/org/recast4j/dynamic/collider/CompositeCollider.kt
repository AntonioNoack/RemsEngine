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
package org.recast4j.dynamic.collider

import org.recast4j.recast.Heightfield
import org.recast4j.recast.Telemetry
import java.util.*
import java.util.function.Consumer

class CompositeCollider : Collider {
    private val colliders: List<Collider>
    private val bounds: FloatArray

    constructor(colliders: List<Collider>) {
        this.colliders = colliders
        bounds = bounds(colliders)
    }

    constructor(vararg colliders: Collider) {
        this.colliders = colliders.toList()
        bounds = bounds(this.colliders)
    }

    override fun bounds(): FloatArray {
        return bounds
    }

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        colliders.forEach(Consumer { c: Collider -> c.rasterize(hf, telemetry) })
    }

    companion object {
        fun emptyBounds(): FloatArray {
            return floatArrayOf(
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )
        }
        private fun bounds(colliders: List<Collider>): FloatArray {
            val bounds = emptyBounds()
            for (collider in colliders) {
                val b = collider.bounds()
                bounds[0] = Math.min(bounds[0], b[0])
                bounds[1] = Math.min(bounds[1], b[1])
                bounds[2] = Math.min(bounds[2], b[2])
                bounds[3] = Math.max(bounds[3], b[3])
                bounds[4] = Math.max(bounds[4], b[4])
                bounds[5] = Math.max(bounds[5], b[5])
            }
            return bounds
        }
    }
}