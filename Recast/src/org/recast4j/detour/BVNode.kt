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
import org.recast4j.Vectors
import kotlin.math.max
import kotlin.math.min

/**
 * Bounding volume node.
 *
 * @note This structure is rarely if ever used by the end user.
 * @see MeshTile
 */
class BVNode {

    var minX = 0
    var minY = 0
    var minZ = 0
    var maxX = 0
    var maxY = 0
    var maxZ = 0

    /**
     * Negative for escape sequence.
     */
    var index = 0

    fun setQuantitized(bmin: Vector3f, bmax: Vector3f, hmin: Vector3f, quantFactor: Float) {
        minX = quantitize(bmin.x, hmin.x, quantFactor)
        minY = quantitize(bmin.y, hmin.y, quantFactor)
        minZ = quantitize(bmin.z, hmin.z, quantFactor)
        maxX = quantitize(bmax.x, hmin.x, quantFactor)
        maxY = quantitize(bmax.y, hmin.y, quantFactor)
        maxZ = quantitize(bmax.z, hmin.z, quantFactor)
    }

    fun union(x: Int, y: Int, z: Int) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (z < minZ) minZ = z
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        if (z > maxZ) maxZ = z
    }

    fun union(other: BVNode) {
        minX = min(minX, other.minX)
        minY = min(minY, other.minY)
        minZ = min(minZ, other.minZ)
        maxX = max(maxX, other.maxX)
        maxY = max(maxY, other.maxY)
        maxZ = max(maxZ, other.maxZ)
    }

    fun copyInto(dst: BVNode) {
        copyBoundsInto(dst)
        dst.index = index
    }

    fun copyBoundsInto(dst: BVNode) {
        dst.minX = minX
        dst.minY = minY
        dst.minZ = minZ
        dst.maxX = maxX
        dst.maxY = maxY
        dst.maxZ = maxZ
    }

    companion object {
        fun quantitize(bmin: Float, headerMin: Float, quantFactor: Float): Int {
            return Vectors.clamp(((bmin - headerMin) * quantFactor).toInt(), 0, Int.MAX_VALUE)
        }
    }
}