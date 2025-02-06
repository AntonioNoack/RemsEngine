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

import org.joml.Vector3f
import org.recast4j.LongArrayList
import org.recast4j.Vectors
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.Node
import org.recast4j.detour.NodePool
import org.recast4j.detour.QueryFilter
import java.util.*

class LocalBoundary {

    class Segment {
        val start = Vector3f()
        val end = Vector3f()
        var pruningDist = 0f
    }

    val center = Vector3f(Float.MAX_VALUE)
    val segments = ArrayList<Segment>()
    var polygons = LongArrayList()

    fun reset() {
        center.set(Float.MAX_VALUE)
        polygons.clear()
        segments.clear()
    }

    fun addSegment(dist: Float, s: FloatArray) {
        // Insert neighbour based on the distance.
        val seg = synchronized(cache) {
            cache.removeLastOrNull() ?: Segment()
        }
        seg.start.set(s, 0)
        seg.end.set(s, 3)
        seg.pruningDist = dist
        if (segments.isEmpty()) {
            segments.add(seg)
        } else if (dist >= segments[segments.size - 1].pruningDist) {
            if (segments.size >= MAX_LOCAL_SEGS) {
                return
            }
            segments.add(seg)
        } else {
            // Insert in-between.
            var i = 0
            while (i < segments.size) {
                if (dist <= segments[i].pruningDist) {
                    break
                }
                ++i
            }
            segments.add(i, seg)
        }
        synchronized(cache) {
            while (segments.size > MAX_LOCAL_SEGS) {
                cache.add(segments.removeAt(segments.size - 1))
            }
        }
    }

    fun update(
        ref: Long,
        pos: Vector3f,
        collisionQueryRange: Float,
        navquery: NavMeshQuery,
        filter: QueryFilter,
        tinyNodePool: NodePool,
        pa: FloatArray, pb: FloatArray,
        tmpSegments: ArrayList<FloatArray>,
        tmpInts: ArrayList<NavMeshQuery.SegInterval>,
        tmp: NavMeshQuery.PortalResult, tmpN: Vector3f,
        stack: LinkedList<Node>
    ) {
        if (ref == 0L) {
            reset()
            return
        }
        center.set(pos)
        // First query non-overlapping polygons.
        val res = navquery.findLocalNeighbourhood(
            ref, pos, collisionQueryRange, filter,
            tinyNodePool, pa, pb, polygons, tmp, tmpN, stack
        )
        if (res) {
            synchronized(cache) { cache.addAll(segments) }
            segments.clear()
            // Secondly, store all polygon edges.
            for (i in 0 until polygons.size) {
                val poly = polygons[i]
                val segments = navquery.getPolyWallSegments(poly, false, filter, tmpSegments, tmpInts)
                if (segments != null) {
                    for (k in segments.indices) {
                        val segment = segments[k]
                        // Skip too distant segments.
                        val t = Vectors.distancePtSegSqr2DFirst(pos, segment, 0, 3)
                        if (t < collisionQueryRange * collisionQueryRange) {
                            addSegment(t, segment)
                        }
                    }
                    synchronized(NavMeshQuery.segmentCache) {
                        NavMeshQuery.segmentCache.addAll(segments)
                    }
                }
            }
        }
    }

    fun isValid(navMeshQuery: NavMeshQuery, filter: QueryFilter): Boolean {
        if (polygons.isEmpty()) return false
        // Check, that all polygons still pass query filter.
        var i = 0
        val l0 = polygons.size
        while (i < l0) {
            val ref = polygons[i]
            if (!navMeshQuery.isValidPolyRef(ref, filter)) {
                return false
            }
            i++
        }
        return true
    }

    companion object {
        private const val MAX_LOCAL_SEGS = 8
        private val cache = ArrayList<Segment>()
    }
}