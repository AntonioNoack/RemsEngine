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
import org.recast4j.LongArrayList

/**
 * Provides information about raycast hit. Filled by NavMeshQuery::raycast
 */
class RaycastHit {
    /** The hit parameter. (Float.MAX_VALUE if no wall hit.)  */
    var t = 0f

    /** hitNormal The normal of the nearest wall hit. [(x, y, z)]  */
    val hitNormal = Vector3f()

    /** Visited polygons.  */
    val path = LongArrayList()

    /** The cost of the path until hit.  */
    var pathCost = 0f

    /** The index of the edge on the final polygon where the wall was hit.  */
    var hitEdgeIndex = 0
}