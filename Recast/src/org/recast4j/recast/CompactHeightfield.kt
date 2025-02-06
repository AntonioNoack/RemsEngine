/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.recast

import org.joml.Vector3f

/** A compact, static heightfield representing unobstructed space.  */
class CompactHeightfield(val width: Int, val height: Int, val spanCount: Int) {

    /** The walkable height used during the build of the field. (See: RecastConfig::walkableHeight)  */
    var walkableHeight = 0

    /** The walkable climb used during the build of the field. (See: RecastConfig::walkableClimb)  */
    var walkableClimb = 0

    /** The AABB border size used during the build of the field. (See: RecastConfig::borderSize)  */
    var borderSize = 0

    /** The maximum distance value of any span within the field.  */
    var maxDistance = 0

    /** The maximum region id of any span within the field.  */
    var maxRegions = 0

    /** The minimum bounds in world space. [(x, y, z)]  */
    val bmin = Vector3f()

    /** The maximum bounds in world space. [(x, y, z)]  */
    val bmax = Vector3f()

    /** The size of each cell. (On the xz-plane.)  */
    var cellSize = 0f

    /** The height of each cell. (The minimum increment along the y-axis.)  */
    var cellHeight = 0f

    /** Array of cells. [Size: #width*#height]  */
    val index = IntArray(Math.multiplyExact(width, height))
    val endIndex = IntArray(Math.multiplyExact(width, height))

    /** Array of spans. [Size: #spanCount]  */
    val spans = Array(spanCount) { CompactSpan() }

    /** Array containing border distance data. [Size: #spanCount]  */
    val dist = IntArray(spanCount)

    /** Array containing area id data. [Size: #spanCount]  */
    val areas = IntArray(spanCount)
}