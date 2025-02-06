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
package org.recast4j.detour.crowd

import org.recast4j.LongHashMap
import kotlin.math.abs
import kotlin.math.floor

class ProximityGrid(val cellSize: Float) {

    companion object {
        private val cache = ArrayList<ArrayList<CrowdAgent>>()
    }

    private val invCellSize = 1f / cellSize
    private val agents = LongHashMap<ArrayList<CrowdAgent>>()

    fun clear() {
        agents.forEachValue { v ->
            if (cache.size < 512) cache.add(v)
            v.clear()
        }
        agents.clear()
    }

    fun addAgentToBuckets(agent: CrowdAgent, minXf: Float, minYf: Float, maxXf: Float, maxYf: Float) {
        val minX = floor((minXf * invCellSize)).toInt()
        val minY = floor((minYf * invCellSize)).toInt()
        val maxX = floor((maxXf * invCellSize)).toInt()
        val maxY = floor((maxYf * invCellSize)).toInt()
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val key = createItemKey(x, y)
                val agents = agents.getOrPut(key) {
                    synchronized(cache) {
                        cache.removeLastOrNull() ?: ArrayList()
                    }
                }
                agents.add(agent)
            }
        }
    }

    fun queryItems(
        minXf: Float, minYf: Float,
        maxXf: Float, maxYf: Float,
        self: CrowdAgent, range: Float,
        dst: ArrayList<CrowdAgent>
    ) {
        val minX = floor((minXf * invCellSize)).toInt()
        val minY = floor((minYf * invCellSize)).toInt()
        val maxX = floor((maxXf * invCellSize)).toInt()
        val maxY = floor((maxYf * invCellSize)).toInt()
        dst.clear()
        val pos = self.currentPosition
        val height = self.params.height
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val key = createItemKey(x, y)
                val agents = agents[key] ?: continue
                for (i in agents.indices) {
                    val ag = agents[i]
                    if (ag === self) continue
                    // Check for overlap.
                    val cp = ag.currentPosition
                    val dy = pos.y - cp.y
                    if (abs(dy) < (height + ag.params.height) / 2f) {
                        val dx = pos.x - cp.x
                        val dz = pos.z - cp.z
                        val distSqr = dx * dx + dz * dz
                        if (distSqr < range * range && ag !in dst) {
                            dst.add(ag)
                        }
                    }
                }
            }
        }
    }

    private fun createItemKey(x: Int, y: Int) = x.toLong().shl(32) or y.toLong().and(0xffffffffL)
}