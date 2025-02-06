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
package org.recast4j.recast

import org.joml.Vector3f
import org.recast4j.Vectors
import kotlin.math.cos
import kotlin.math.max

object Recast {

    fun calcGridSizeX(bmin: Vector3f, bmax: Vector3f, cellSize: Float): Int {
        return max(1, ((bmax.x - bmin.x) / cellSize + 0.5f).toInt())
    }

    fun calcGridSizeY(bmin: Vector3f, bmax: Vector3f, cellSize: Float): Int {
        return max(1, ((bmax.z - bmin.z) / cellSize + 0.5f).toInt())
    }

    fun calcTileCountX(bmin: Vector3f, bmax: Vector3f, cellSize: Float, tileSizeX: Int): Int {
        val gwd = calcGridSizeX(bmin, bmax, cellSize)
        return max(1, (gwd + tileSizeX - 1) / tileSizeX)
    }

    fun calcTileCountY(bmin: Vector3f, bmax: Vector3f, cellSize: Float, tileSizeZ: Int): Int {
        val gwd = calcGridSizeY(bmin, bmax, cellSize)
        return max(1, (gwd + tileSizeZ - 1) / tileSizeZ)
    }

    /**
     * Modifies the area id of all triangles with a slope below the specified value.
     * See the rcConfig documentation for more information on the configuration parameters.
     */
    fun markWalkableTriangles(
        walkableSlopeAngle: Float, vertices: FloatArray, tris: IntArray, nt: Int,
        areaMod: AreaModification
    ): IntArray {
        val areas = IntArray(nt)
        val walkableThr = cos(walkableSlopeAngle / 180f * Math.PI).toFloat()
        val norm = Vector3f()
        for (i in 0 until nt) {
            val tri = i * 3
            calcTriNormal(vertices, tris[tri], tris[tri + 1], tris[tri + 2], norm)
            // Check if the face is walkable.
            if (norm.y > walkableThr) areas[i] = areaMod.apply(areas[i])
        }
        return areas
    }

    fun calcTriNormal(vertices: FloatArray, v0: Int, v1: Int, v2: Int, norm: Vector3f) {
        val e0 = Vector3f()
        val e1 = Vector3f()
        Vectors.sub(e0, vertices, v1 * 3, v0 * 3)
        Vectors.sub(e1, vertices, v2 * 3, v0 * 3)
        e0.cross(e1, norm).normalize()
    }

    /**
     * Only sets the area id's for the unwalkable triangles. Does not alter the area id's for walkable triangles.
     * See the rcConfig documentation for more information on the configuration parameters.
     */
    fun clearUnwalkableTriangles(
        walkableSlopeAngle: Float, vertices: FloatArray,
        tris: IntArray, nt: Int, areas: IntArray
    ) {
        val walkableThr = cos(walkableSlopeAngle / 180f * Math.PI).toFloat()
        val norm = Vector3f()
        for (i in 0 until nt) {
            val tri = i * 3
            calcTriNormal(vertices, tris[tri], tris[tri + 1], tris[tri + 2], norm)
            // Check if the face is walkable.
            if (norm.y <= walkableThr) areas[i] = RecastConstants.RC_NULL_AREA
        }
    }
}