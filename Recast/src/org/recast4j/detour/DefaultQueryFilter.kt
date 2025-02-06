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
package org.recast4j.detour

import org.joml.Vector3f
import kotlin.math.min

/**
 * **The Default Implementation**
 *
 * At construction: All area costs default to 1.0. All flags are included and none are excluded.
 *
 * If a polygon has both an include and an exclude flag, it will be excluded.
 *
 * The way filtering works, a navigation mesh polygon must have at least one flag set to ever be considered by a query.
 * So a polygon with no flags will never be considered.
 *
 * Setting the include flags to 0 will result in all polygons being excluded.
 *
 * **Custom Implementations**
 *
 * Implement a custom query filter by overriding the virtual passFilter() and getCost() functions. If this is done, both
 * functions should be as fast as possible. Use cached local copies of data rather than accessing your own objects where
 * possible.
 *
 * Custom implementations do not need to adhere to the flags or cost logic used by the default implementation.
 *
 * In order for A* searches to work properly, the cost should be proportional to the travel distance. Implementing a
 * cost modifier less than 1.0 is likely to lead to problems during pathfinding.
 *
 * @see NavMeshQuery
 */
class DefaultQueryFilter : QueryFilter {

    var excludeFlags: Int
    var includeFlags: Int
    private val areaCosts = FloatArray(NavMesh.DT_MAX_AREAS)

    constructor() {
        includeFlags = 0xffff
        excludeFlags = 0
        for (i in 0 until NavMesh.DT_MAX_AREAS) {
            areaCosts[i] = 1f
        }
    }

    constructor(includeFlags: Int, excludeFlags: Int, areaCost: FloatArray) {
        this.includeFlags = includeFlags
        this.excludeFlags = excludeFlags
        System.arraycopy(areaCost, 0, areaCosts, 0, min(NavMesh.DT_MAX_AREAS, areaCost.size))
        for (i in areaCost.size until NavMesh.DT_MAX_AREAS) {
            areaCosts[i] = 1f
        }
    }

    override fun passFilter(ref: Long, tile: MeshTile?, poly: Poly): Boolean {
        return poly.flags and includeFlags != 0 && poly.flags and excludeFlags == 0
    }

    override fun getCost(
        pa: Vector3f, pb: Vector3f, prevRef: Long, prevTile: MeshTile?, prevPoly: Poly?, curRef: Long,
        curTile: MeshTile?, curPoly: Poly, nextRef: Long, nextTile: MeshTile?, nextPoly: Poly?
    ): Float {
        return pa.distance(pb) * areaCosts[curPoly.area]
    }
}