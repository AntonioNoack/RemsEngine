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

/** Represents a heightfield layer within a layer set.  */
class Heightfield(
    /** The width of the heightfield. (Along the x-axis in cell units.)  */
    val width: Int,
    /** The height of the heightfield. (Along the z-axis in cell units.)  */
    val height: Int,
    /** The minimum bounds in world space. [(x, y, z)]  */
    val bmin: Vector3f,
    /** The maximum bounds in world space. [(x, y, z)]  */
    val bmax: Vector3f,
    val cellSize: Float,
    /** The minimum increment along the y-axis.  */
    val cellHeight: Float,
    /** Border size in cell units  */
    val borderSize: Int
) {
    /** Heightfield of spans (width*height).  */
    val spans: Array<Span?> = arrayOfNulls(Math.multiplyExact(width, height))

}