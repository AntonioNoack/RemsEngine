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

/**
 * Represents a group of related contours.
 */
class ContourSet {
    /**
     * A list of the contours in the set.
     */
    val contours = ArrayList<Contour>()

    /**
     * The bounds in world space.
     */
    val bmin = Vector3f()
    val bmax = Vector3f()
    var cellSize = 0f
    var cellHeight = 0f

    /**
     * The width of the set. (Along the x-axis in cell units.)
     */
    var width = 0

    /**
     * The height of the set. (Along the z-axis in cell units.)
     */
    var height = 0

    /**
     * The AABB border size used to generate the source data from which the contours were derived.
     */
    var borderSize = 0

    /**
     * The max edge error that this contour set was simplified with.
     */
    var maxError = 0f
}