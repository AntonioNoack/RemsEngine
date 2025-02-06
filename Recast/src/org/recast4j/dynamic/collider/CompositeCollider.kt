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

import org.joml.AABBf
import org.recast4j.recast.Heightfield
import org.recast4j.recast.Telemetry

class CompositeCollider : Collider {
    private val colliders: List<Collider>
    override val bounds: AABBf

    constructor(colliders: List<Collider>) {
        this.colliders = colliders
        bounds = bounds(colliders)
    }

    constructor(vararg colliders: Collider) {
        this.colliders = colliders.toList()
        bounds = bounds(this.colliders)
    }

    override fun rasterize(hf: Heightfield, telemetry: Telemetry?) {
        colliders.forEach { c: Collider -> c.rasterize(hf, telemetry) }
    }

    companion object {
        fun emptyBounds(): AABBf {
            return AABBf()
        }

        private fun bounds(colliders: List<Collider>): AABBf {
            val bounds = AABBf()
            for (collider in colliders) {
                bounds.union(collider.bounds)
            }
            return bounds
        }
    }
}