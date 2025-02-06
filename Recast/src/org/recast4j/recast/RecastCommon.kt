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

object RecastCommon {

    /**
     * Gets neighbor connection data for the specified direction.
     * @param s The span to check.
     * @param dir The direction to check. [Limits: 0 <= value < 4]
     * @return The neighbor connection data for the specified direction, or #RC_NOT_CONNECTED if there is no connection.
     * */
    fun getCon(s: CompactSpan, dir: Int): Int {
        val shift = dir * 6
        return s.connectionData shr shift and 0x3f
    }

    private val offset = intArrayOf(-1, 0, 1, 0)

    /**
     * Gets the standard width (x-axis) offset for the specified direction.
     * @param dir The direction. [Limits: 0 <= value < 4]
     * @return The width offset to apply to the current cell position to move in the direction.
     * */
    fun getDirOffsetX(dir: Int): Int {
        return offset[dir and 0x03]
    }

    /**
     * Gets the standard height (z-axis) offset for the specified direction.
     * @param dir The direction. [Limits: 0 <= value < 4]
     * @return The height offset to apply to the current cell position to move in the direction.
     * */
    fun getDirOffsetY(dir: Int): Int {
        return offset[dir + 1 and 0x03]
    }

    private val dirs = intArrayOf(3, 0, -1, 2, 1)

    /**
     * Gets the direction for the specified offset. One of x and y should be 0.
     * @param x The x offset. [Limits: -1 <= value <= 1]
     * @param y The y offset. [Limits: -1 <= value <= 1]
     * @return The direction that represents the offset.
     * */
    fun getDirForOffset(x: Int, y: Int): Int {
        return dirs[(y + 1 shl 1) + x]
    }

    /**
     * Sets the neighbor connection data for the specified direction.
     * @param s The span to update.
     * @param dir The direction to set. [Limits: 0 <= value < 4]
     * @param i The index of the neighbor span.
     * */
    fun setCon(s: CompactSpan, dir: Int, i: Int) {
        val shift = dir * 6
        val con = s.connectionData
        s.connectionData = con and (0x3f shl shift).inv() or (i and 0x3f shl shift)
    }
}