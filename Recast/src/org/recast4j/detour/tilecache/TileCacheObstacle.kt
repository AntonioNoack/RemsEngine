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
package org.recast4j.detour.tilecache

import org.joml.Vector3f
import org.recast4j.LongArrayList

class TileCacheObstacle(val index: Int) {
    enum class TileCacheObstacleType {
        CYLINDER, BOX, ORIENTED_BOX
    }

    var type: TileCacheObstacleType? = null
    val pos = Vector3f()
    val bmin = Vector3f()
    val bmax = Vector3f()
    var radius = 0f
    var height = 0f
    val center = Vector3f()
    val extents = Vector3f()
    val rotAux = FloatArray(2) // { cos(0.5f*angle)*sin(-0.5f*angle); cos(0.5f*angle)*cos(0.5f*angle) - 0.5 }
    var touched = LongArrayList()
    val pending = LongArrayList()
    var salt = 1
    var state = ObstacleState.DT_OBSTACLE_EMPTY
    var next: TileCacheObstacle? = null
}