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
import org.joml.Vector3i
import org.recast4j.FloatArrayList
import org.recast4j.FloatSubArray
import org.recast4j.LongArrayList
import org.recast4j.Vectors
import org.recast4j.detour.PolygonByCircleConstraint.StrictPolygonByCircleConstraint.CIRCLE_SEGMENTS
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

open class NavMeshQuery(val nav1: NavMesh) {

    val nodePool = NodePool()
    val openList = PriorityQueue<Node> { n1, n2 -> n1.totalCost.compareTo(n2.totalCost) }

    /** Sliced query state. */
    lateinit var queryData: QueryData

    /**
     * Returns random location on navmesh. Polygons are chosen weighted by area. The search runs in linear related to
     * number of polygon.
     *
     * @param filter The polygon filter to apply to the query.
     * @param random Function returning a random number [0..1).
     * @return Random location
     */
    fun findRandomPoint(filter: QueryFilter, random: Random): FindRandomPointResult? {

        // Randomly pick one tile. Assume that all tiles cover roughly the same area.

        var tile: MeshTile? = null
        var tsum = 0f
        for (t in nav1.allTiles) {
            // Choose random tile using reservoi sampling.
            val area = 1f // Could be tile area too.
            tsum += area
            val u = random.nextFloat()
            if (u * tsum <= area) {
                tile = t
            }
        }

        if (tile == null) return null

        // Randomly pick one polygon weighted by polygon area.
        var poly: Poly? = null
        var polyRef = 0L
        val base = nav1.getPolyRefBase(tile)
        var areaSum = 0f
        val tileData = tile.data
        for (i in 0 until tileData.header.polyCount) {
            val p = tileData.polygons[i]
            // Do not return off-mesh connection polygons.
            if (p.type != Poly.DT_POLYTYPE_GROUND) {
                continue
            }
            // Must pass filter
            val ref = base or i.toLong()
            if (!filter.passFilter(ref, tile, p)) {
                continue
            }

            // Calc area of the polygon.
            var polyArea = 0f
            for (j in 2 until p.vertCount) {
                val va = p.vertices[0] * 3
                val vb = p.vertices[j - 1] * 3
                val vc = p.vertices[j] * 3
                polyArea += Vectors.triArea2D(tileData.vertices, va, vb, vc)
            }

            // Choose random polygon weighted by area, using reservoi sampling.
            areaSum += polyArea
            val u = random.nextFloat()
            if (u * areaSum <= polyArea) {
                poly = p
                polyRef = ref
            }
        }
        if (poly == null) return null

        // Randomly pick point on polygon.
        val vertices = FloatArray(3 * nav1.maxVerticesPerPoly)
        val areas = FloatArray(nav1.maxVerticesPerPoly)
        val srcVertices = tile.data.vertices
        for (j in 0 until poly.vertCount) {
            val srcI = poly.vertices[j] * 3
            srcVertices.copyInto(vertices, j * 3, srcI, srcI + 3)
        }
        val s = random.nextFloat()
        val t = random.nextFloat()
        val pt = Vectors.randomPointInConvexPoly(vertices, poly.vertCount, areas, s, t)
        val y = getPolyHeight(polyRef, pt)
        if (!y.isFinite()) return null
        pt.y = y
        return FindRandomPointResult(polyRef, pt)
    }

    /**
     * Returns random location on navmesh within the reach of specified location. Polygons are chosen weighted by area.
     * The search runs in linear related to number of polygon. The location is not exactly constrained by the circle,
     * but it limits the visited polygons.
     *
     * @param startRef  The reference id of the polygon where the search starts.
     * @param centerPos The center of the search circle. [(x, y, z)]
     * @param filter    The polygon filter to apply to the query.
     * @param random    Function returning a random number [0..1).
     * @return Random location
     */
    fun findRandomPointAroundCircle(
        startRef: Long,
        centerPos: Vector3f,
        maxRadius: Float,
        filter: QueryFilter,
        random: Random
    ) = findRandomPointAroundCircle(
        startRef, centerPos, maxRadius, filter, random,
        PolygonByCircleConstraint.NoOpPolygonByCircleConstraint
    )

    /**
     * Returns random location on navmesh within the reach of specified location. Polygons are chosen weighted by area.
     * The search runs in linear related to number of polygon. The location is strictly constrained by the circle.
     *
     * @param startRef  The reference id of the polygon where the search starts.
     * @param centerPos The center of the search circle. [(x, y, z)]
     * @param filter    The polygon filter to apply to the query.
     * @param random     Function returning a random number [0..1).
     * @return Random location
     */
    fun findRandomPointWithinCircle(
        startRef: Long,
        centerPos: Vector3f,
        maxRadius: Float,
        filter: QueryFilter,
        random: Random
    ) = findRandomPointAroundCircle(
        startRef, centerPos, maxRadius, filter, random,
        PolygonByCircleConstraint.StrictPolygonByCircleConstraint
    )

    fun findRandomPointAroundCircle(
        startRef: Long, centerPos: Vector3f, maxRadius: Float,
        filter: QueryFilter, random: Random, constraint: PolygonByCircleConstraint
    ): FindRandomPointResult? {

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !centerPos.isFinite || maxRadius < 0 || !maxRadius.isFinite()) {
            return null
        }

        val startTile = nav1.getTileByRefUnsafe(startRef)
        val startPoly = nav1.getPolyByRefUnsafe(startRef, startTile)
        if (!filter.passFilter(startRef, startTile, startPoly)) {
            return null
        }

        nodePool.clear()
        openList.clear()
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(centerPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = 0f
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        val radiusSqr = maxRadius * maxRadius
        var areaSum = 0f
        var randomPoly: Poly? = null
        var randomTile: MeshTile? = null
        var randomPolyRef = 0L
        val tmp = PortalResult()
        val (polyVertices, tmp1, tmp2) = fsaCache.get()
        while (!openList.isEmpty()) {
            val bestNode = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED
            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRefUnsafe(bestRef)
            val bestPoly = nav1.getPolyByRefUnsafe(bestRef, bestTile)

            // Place random locations on the ground.
            if (bestPoly.type == Poly.DT_POLYTYPE_GROUND) {
                // Calc area of the polygon.
                var polyArea = 0f
                val src = bestTile.data.vertices
                val dst = polyVertices.data
                val srcIndices = bestPoly.vertices
                val vertCount = min(bestPoly.vertCount, maxPolyVertices)
                for (i in 0 until vertCount) {
                    val srcI = srcIndices[i] * 3
                    src.copyInto(dst, i * 3, srcI, srcI + 3)
                }
                polyVertices.size = vertCount * 3 // should be 9
                val constrainedVertices = constraint.apply(polyVertices, centerPos, maxRadius, tmp1, tmp2)
                if (constrainedVertices != null) {
                    val vertCount1 = constrainedVertices.size / 3
                    for (j in 2 until vertCount1) {
                        val va = 0
                        val vb = (j - 1) * 3
                        val vc = j * 3
                        polyArea += Vectors.triArea2D(constrainedVertices.data, va, vb, vc)
                    }
                    // Choose random polygon weighted by area, using reservoir sampling.
                    areaSum += polyArea
                    if (random.nextFloat() * areaSum <= polyArea) {
                        randomTile = bestTile
                        randomPoly = bestPoly
                        randomPolyRef = bestRef
                        // randomPolyVertices = constrainedVertices
                    }
                }
            }

            // Get parent poly and tile.
            var parentRef = 0L
            if (bestNode.parentIndex != 0) {
                parentRef = nodePool.getNodeAtIdx(bestNode.parentIndex)!!.polygonRef
            }
            var i = bestTile.polyLinks[bestPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val link = bestTile.links[i]
                val neighbourRef = link.neighborRef
                // Skip invalid neighbours and do not follow back to parent.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Expand to neighbour
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)

                // Do not advance if the polygon is excluded by the filter.
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Find edge and calc distance to the edge.
                val portalPoints = getPortalPoints(
                    bestRef, bestPoly, bestTile, neighbourRef,
                    neighbourPoly, neighbourTile, 0, 0, tmp
                )
                if (portalPoints == null) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val va = portalPoints.left
                val vb = portalPoints.right

                // If the circle is not touching the next polygon, skip it.
                val (distSqr) = Vectors.distancePtSegSqr2D(centerPos, va, vb)
                if (distSqr > radiusSqr) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef)
                if (neighbourNode.flags and Node.CLOSED != 0) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Cost
                if (neighbourNode.flags == 0) {
                    neighbourNode.pos.set(va).add(vb).mul(0.5f)
                }
                val total = bestNode.totalCost + bestNode.pos.distance(neighbourNode.pos)

                // The node is already in open list, and the new result is worse, skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.flags = neighbourNode.flags and Node.CLOSED.inv()
                neighbourNode.parentIndex = nodePool.getNodeIdx(bestNode)
                neighbourNode.totalCost = total
                if (neighbourNode.flags and Node.OPEN != 0) {
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    neighbourNode.flags = Node.OPEN
                    openList.offer(neighbourNode)
                }
                i = bestTile.links[i].indexOfNextLink
            }
        }

        if (randomPoly == null) {
            return null
        }

        // Randomly pick point on polygon.
        val s = random.nextFloat()
        val t = random.nextFloat()
        val src = randomTile!!.data.vertices
        val dst = polyVertices.data
        val srcIndices = randomPoly.vertices
        val vertCount = min(randomPoly.vertCount, maxPolyVertices)
        for (i in 0 until vertCount) {
            System.arraycopy(src, srcIndices[i] * 3, dst, i * 3, 3)
        }
        polyVertices.size = vertCount * 3
        val randomPolyVertices = constraint.apply(polyVertices, centerPos, maxRadius, tmp1, tmp2) ?: return null
        val areas = FloatArray(randomPolyVertices.size / 3)
        val pt = Vectors.randomPointInConvexPoly(randomPolyVertices.data, randomPolyVertices.size / 3, areas, s, t)
        val y = getPolyHeight(randomPolyRef, pt)
        if (!y.isFinite()) return null
        pt.y = y
        return FindRandomPointResult(randomPolyRef, pt)
    }

    /**
     * Uses the detail polygons to find the surface height. (Most accurate.)
     *
     * @param pos does not have to be within the bounds of the polygon or navigation mesh.
     *
     * See closestPointOnPolyBoundary() for a limited but faster option.
     * Finds the closest point on the specified polygon.
     */
    fun closestPointOnPoly(ref: Long, pos: Vector3f): ClosestPointOnPolyResult? {
        return if (!nav1.isValidPolyRef(ref) || !pos.isFinite) null
        else nav1.closestPointOnPoly(ref, pos)
    }

    /**
     * Much faster than closestPointOnPoly().
     *
     * If the provided position lies within the polygon's xz-bounds (above or below), then @p pos and @p closest will be equal.
     * The height of @p closest will be the polygon boundary. The height detail is not used.
     *
     * @p pos does not have to be within the bounds of the polygon or the navigation mesh.
     *
     * Returns a point on the boundary closest to the source point if the source point is outside the polygon's xz-bounds.
     * */
    fun closestPointOnPolyBoundary(
        ref: Long, pos: Vector3f,
        tmpVertices: FloatArray, // nav1.maxVerticesPerPoly * 3
        tmpEdges0: FloatArray, tmpEdges1: FloatArray, // nav1.maxVerticesPerPoly
    ): Vector3f? {
        if (!pos.isFinite) return null
        val tile = nav1.getTileByRef(ref) ?: return null
        val poly = nav1.getPolyByRef(ref, tile) ?: return null
        // Collect vertices.
        val nv = poly.vertCount
        val srcVertices = tile.data.vertices
        for (i in 0 until nv) {
            val srcI = poly.vertices[i] * 3
            srcVertices.copyInto(tmpVertices, i * 3, srcI, srcI + 3)
        }
        if (Vectors.distancePtPolyEdgesSqr(pos, tmpVertices, nv, tmpEdges0, tmpEdges1)) {
            return pos
        } else {
            // Point is outside the polygon, dtClamp to the nearest edge.
            var dmin = tmpEdges0[0]
            var imin = 0
            for (i in 1 until nv) {
                if (tmpEdges0[i] < dmin) {
                    dmin = tmpEdges0[i]
                    imin = i
                }
            }
            val va = imin * 3
            val vb = (imin + 1) % nv * 3
            val dst = Vector3f()
            Vectors.lerp(tmpVertices, va, vb, tmpEdges1[imin], dst)
            return dst
        }
    }

    /**
     * Gets the height of the polygon at the provided position using the height detail. (Most accurate.)
     * Will return NaN/+/-Inf if the provided position is outside the xz-bounds of the polygon.
     * @param ref The reference id of the polygon.
     * @param pos A position within the xz-bounds of the polygon. [(x, y, z)]
     * @return The height at the surface of the polygon. or !finite for errors
     */
    fun getPolyHeight(ref: Long, pos: Vector3f): Float {
        if (!pos.isFinite) return Float.POSITIVE_INFINITY
        val tile = nav1.getTileByRef(ref) ?: return Float.NEGATIVE_INFINITY
        val poly = nav1.getPolyByRef(ref, tile) ?: return Float.NEGATIVE_INFINITY
        // We used to return success for offmesh connections, but the
        // getPolyHeight in DetourNavMesh does not do this, so special
        // case it here.
        if (poly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
            val vs = tile.data.vertices
            val pi = poly.vertices[0] * 3
            val qi = poly.vertices[1] * 3
            val second = Vectors.distancePtSegSqr2DSecond(pos, vs, pi, qi)
            return vs[pi + 1] + (vs[qi + 1] - vs[pi + 1]) * second
        }
        return nav1.getPolyHeight(tile, poly, pos) // value / NaN
    }

    /**
     * Finds the polygon nearest to the specified center point. If center and nearestPt point to an equal position,
     * isOverPoly will be true; however there's also a special case of climb height inside the polygon
     *
     * @param center      The center of the search box. [(x, y, z)]
     * @param halfExtents The search distance along each axis. [(x, y, z)]
     * @param filter      The polygon filter to apply to the query.
     * @return FindNearestPolyResult containing nearestRef, nearestPt and overPoly
     */
    fun findNearestPoly(
        center: Vector3f,
        halfExtents: Vector3f,
        filter: QueryFilter
    ): Result<FindNearestPolyResult?> {

        // Get nearby polygons from proximity grid.
        val query = FindNearestPolyQuery(this, center)
        val status = queryPolygons(center, halfExtents, filter, query)
        return if (status.isFailed) {
            Result.of(status, null)
        } else Result.success(query.result())
    }

    fun queryPolygonsInTile(
        tile: MeshTile,
        qmin: Vector3f,
        qmax: Vector3f,
        filter: QueryFilter,
        query: PolyQuery
    ) {
        val data = tile.data
        if (data.bvTree != null) {
            var nodeIndex = 0
            val tbmin = data.header.bmin
            val tbmax = data.header.bmax
            val qfac = data.header.bvQuantizationFactor
            // Calculate quantized box
            // dtClamp query box to world box.
            val minx = Vectors.clamp(qmin.x, tbmin.x, tbmax.x) - tbmin.x
            val miny = Vectors.clamp(qmin.y, tbmin.y, tbmax.y) - tbmin.y
            val minz = Vectors.clamp(qmin.z, tbmin.z, tbmax.z) - tbmin.z
            val maxx = Vectors.clamp(qmax.x, tbmin.x, tbmax.x) - tbmin.x
            val maxy = Vectors.clamp(qmax.y, tbmin.y, tbmax.y) - tbmin.y
            val maxz = Vectors.clamp(qmax.z, tbmin.z, tbmax.z) - tbmin.z
            // Quantize
            val bmin = Vector3i(
                (qfac * minx).toInt() and 0x7ffffffe,
                (qfac * miny).toInt() and 0x7ffffffe,
                (qfac * minz).toInt() and 0x7ffffffe
            )
            val bmax = Vector3i(
                (qfac * maxx + 1).toInt() or 1,
                (qfac * maxy + 1).toInt() or 1,
                (qfac * maxz + 1).toInt() or 1
            )
            // Traverse tree
            val base = nav1.getPolyRefBase(tile)
            val end = data.header.bvNodeCount
            while (nodeIndex < end) {
                val node = data.bvTree!![nodeIndex]
                val overlap: Boolean = Vectors.overlapQuantBounds(bmin, bmax, node)
                val isLeafNode = node.index >= 0
                if (isLeafNode && overlap) {
                    val ref = base or node.index.toLong()
                    if (filter.passFilter(ref, tile, data.polygons[node.index])) {
                        query.process(tile, data.polygons[node.index], ref)
                    }
                }
                if (overlap || isLeafNode) {
                    nodeIndex++
                } else {
                    val escapeIndex = -node.index
                    nodeIndex += escapeIndex
                }
            }
        } else {
            val bmin = Vector3f()
            val bmax = Vector3f()
            val base = nav1.getPolyRefBase(tile)
            for (i in 0 until data.polyCount) {
                val p = data.polygons[i]
                // Do not return off-mesh connection polygons.
                if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                    continue
                }
                val ref = base or i.toLong()
                if (!filter.passFilter(ref, tile, p)) {
                    continue
                }
                // Calc polygon bounds.
                var v = p.vertices[0] * 3
                bmin.set(data.vertices, v)
                bmax.set(data.vertices, v)
                for (j in 1 until p.vertCount) {
                    v = p.vertices[j] * 3
                    Vectors.min(bmin, data.vertices, v)
                    Vectors.max(bmax, data.vertices, v)
                }
                if (Vectors.overlapBounds(qmin, qmax, bmin, bmax)) {
                    query.process(tile, p, ref)
                }
            }
        }
    }

    /**
     * Finds polygons that overlap the search box.
     *
     *
     * If no polygons are found, the function will return with a polyCount of zero.
     *
     * @param center      The center of the search box. [(x, y, z)]
     * @param halfExtents The search distance along each axis. [(x, y, z)]
     * @param filter      The polygon filter to apply to the query.
     * @return The reference ids of the polygons that overlap the query box.
     */
    fun queryPolygons(center: Vector3f, halfExtents: Vector3f, filter: QueryFilter, query: PolyQuery): Status {
        if (!center.isFinite || !halfExtents.isFinite) return Status.FAILURE_INVALID_PARAM
        // Find tiles the query touches.
        val bmin = Vector3f(center).sub(halfExtents)
        val bmax = Vector3f(center).add(halfExtents)
        for (t in queryTiles(center, halfExtents)) {
            queryPolygonsInTile(t, bmin, bmax, filter, query)
        }
        return Status.SUCCESS
    }

    /**
     * Finds tiles that overlap the search box.
     */
    fun queryTiles(center: Vector3f, halfExtents: Vector3f): List<MeshTile> {

        val bmin = Vector3f(center).sub(halfExtents)
        val bmax = Vector3f(center).add(halfExtents)

        val minx = max(nav1.calcTileLocX(bmin), -1_000_000_000)
        val miny = max(nav1.calcTileLocY(bmin), -1_000_000_000)
        val maxx = min(nav1.calcTileLocX(bmax), +1_000_000_000)
        val maxy = min(nav1.calcTileLocY(bmax), +1_000_000_000)

        val numTiles = (maxx - minx).toLong() * (maxy - miny)
        if (numTiles < nav1.numTiles) {
            // it's more efficient to use indices
            val tiles = ArrayList<MeshTile>()
            for (y in miny..maxy) {
                for (x in minx..maxx) {
                    // add all siblings (layers) at x,y
                    var tile = nav1.getTilesAt(x, y)
                    while (tile != null) {
                        tiles.add(tile)
                        tile = tile.next
                    }
                }
            }
            return tiles
        } else {
            // it's more efficient to just iterate everything
            return nav1.allTiles.filter { tile ->
                val data = tile.data
                data.x in minx..maxx && data.y in miny..maxy
            }
        }
    }

    /**
     * Finds a path from the start polygon to the end polygon.
     *
     *
     * If the end polygon cannot be reached through the navigation graph, the last polygon in the path will be the
     * nearest the end polygon.
     *
     *
     * The start and end positions are used to calculate traversal costs. (The y-values impact the result.)
     *
     * @param startRef The refrence id of the start polygon.
     * @param endRef   The reference id of the end polygon.
     * @param startPos A position within the start polygon. [(x, y, z)]
     * @param endPos   A position within the end polygon. [(x, y, z)]
     * @param filter   The polygon filter to apply to the query.
     * @return Found path
     */
    open fun findPath(
        startRef: Long, endRef: Long, startPos: Vector3f, endPos: Vector3f,
        filter: QueryFilter
    ) = findPath(startRef, endRef, startPos, endPos, filter, DefaultQueryHeuristic(), 0, 0f)

    open fun findPath(
        startRef: Long, endRef: Long, startPos: Vector3f, endPos: Vector3f, filter: QueryFilter,
        options: Int, raycastLimit: Float
    ) = findPath(startRef, endRef, startPos, endPos, filter, DefaultQueryHeuristic(), options, raycastLimit)

    fun findPath(
        startRef: Long, endRef: Long, startPos: Vector3f, endPos: Vector3f, filter: QueryFilter,
        heuristic: QueryHeuristic, options: Int, raycastLimit: Float
    ): Result<LongArrayList?> {
        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !nav1.isValidPolyRef(endRef) || !startPos.isFinite || !endPos.isFinite) {
            return Result.invalidParam()
        }

        var raycastLimitSqr = raycastLimit * raycastLimit

        // trade quality with performance?
        if (options and DT_FINDPATH_ANY_ANGLE != 0 && raycastLimit < 0f) {
            // limiting to several times the character radius yields nice results. It is not sensitive
            // so it is enough to compute it from the first tile.
            val tile = nav1.getTileByRef(startRef)
            val agentRadius = tile!!.data.walkableRadius
            raycastLimitSqr = Vectors.sq(agentRadius * NavMesh.DT_RAY_CAST_LIMIT_PROPORTIONS)
        }
        if (startRef == endRef) {
            val path = LongArrayList(1)
            path.add(startRef)
            return Result.success(path)
        }
        nodePool.clear()
        openList.clear()
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(startPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = heuristic.getCost(startPos, endPos)
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        var lastBestNode = startNode
        var lastBestNodeCost = startNode.totalCost
        var status = Status.SUCCESS
        val tmp = PortalResult()
        while (!openList.isEmpty()) {
            // Remove node from open list and put it in closed list.
            val bestNode: Node = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED

            // Reached the goal, stop searching.
            if (bestNode.polygonRef == endRef) {
                lastBestNode = bestNode
                break
            }

            // Get current poly and tile.
            // The API input has been checked already, skip checking internal data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRefUnsafe(bestRef)
            val bestPoly = nav1.getPolyByRefUnsafe(bestRef, bestTile)

            // Get parent poly and tile.
            var parentRef = 0L
            var grandpaRef = 0L
            var parentTile: MeshTile? = null
            var parentPoly: Poly? = null
            var parentNode: Node? = null
            if (bestNode.parentIndex != 0) {
                parentNode = nodePool.getNodeAtIdx(bestNode.parentIndex)!!
                parentRef = parentNode.polygonRef
                if (parentNode.parentIndex != 0) {
                    grandpaRef = nodePool.getNodeAtIdx(parentNode.parentIndex)!!.polygonRef
                }
            }
            if (parentRef != 0L) {
                parentTile = nav1.getTileByRefUnsafe(parentRef)
                parentPoly = nav1.getPolyByRefUnsafe(parentRef, parentTile)
            }

            // decide whether to test raycast to previous nodes
            var tryLOS = false
            if (options and DT_FINDPATH_ANY_ANGLE != 0) {
                if (parentRef != 0L && (raycastLimitSqr >= Float.MAX_VALUE
                            || parentNode!!.pos.distanceSquared(bestNode.pos) < raycastLimitSqr)
                ) {
                    tryLOS = true
                }
            }
            var i = bestTile.polyLinks[bestPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val neighbourRef = bestTile.links[i].neighborRef

                // Skip invalid ids and do not expand back to where we came from.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Get neighbour poly and tile.
                // The API input has been checked already, skip checking internal data.
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // get the node
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef, 0)

                // do not expand to nodes that were already visited from the
                // same parent
                if (neighbourNode.parentIndex != 0 && neighbourNode.parentIndex == bestNode.parentIndex) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // If the node is visited the first time, calculate node position.
                var neighbourPos = neighbourNode.pos
                val midpod = if (neighbourRef == endRef) getEdgeIntersectionPoint(
                    bestNode.pos, bestRef, bestPoly, bestTile, endPos, neighbourRef,
                    neighbourPoly, neighbourTile, tmp
                ) else getEdgeMidPoint(bestRef, bestPoly, bestTile, neighbourRef, neighbourPoly, neighbourTile, tmp)
                if (midpod != null) {
                    neighbourPos = midpod
                }

                // Calculate cost and heuristic.
                var cost = 0f
                var heuristicCost = 0f

                // raycast parent
                var foundShortCut = false
                var shortcut: LongArrayList? = null
                if (tryLOS) {
                    val rayHit = raycast(
                        parentRef, parentNode!!.pos, neighbourPos, filter,
                        DT_RAYCAST_USE_COSTS, grandpaRef
                    )
                    if (rayHit.succeeded()) {
                        foundShortCut = rayHit.result!!.t >= 1f
                        if (foundShortCut) {
                            shortcut = rayHit.result.path
                            // shortcut found using raycast. Using shorter cost
                            // instead
                            cost = parentNode.cost + rayHit.result.pathCost
                        }
                    }
                }

                // update move cost
                if (!foundShortCut) {
                    val curCost = filter.getCost(
                        bestNode.pos, neighbourPos, parentRef, parentTile,
                        parentPoly, bestRef, bestTile, bestPoly, neighbourRef, neighbourTile, neighbourPoly
                    )
                    cost = bestNode.cost + curCost
                }

                // Special case for last node.
                if (neighbourRef == endRef) {
                    // Cost
                    val endCost = filter.getCost(
                        neighbourPos, endPos, bestRef, bestTile, bestPoly, neighbourRef,
                        neighbourTile, neighbourPoly, 0L, null, null
                    )
                    cost += endCost
                } else {
                    // Cost
                    heuristicCost = heuristic.getCost(neighbourPos, endPos)
                }
                val total = cost + heuristicCost

                // The node is already in open list, and the new result is worse, skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                // The node is already visited and process, and the new result is worse, skip.
                if (neighbourNode.flags and Node.CLOSED != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Add or update the node.
                neighbourNode.parentIndex = if (foundShortCut) bestNode.parentIndex else nodePool.getNodeIdx(bestNode)
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.flags = neighbourNode.flags and Node.CLOSED.inv()
                neighbourNode.cost = cost
                neighbourNode.totalCost = total
                neighbourNode.pos.set(neighbourPos)
                neighbourNode.shortcut = shortcut
                if (neighbourNode.flags and Node.OPEN != 0) {
                    // Already in open, update node location.
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    // Put the node in open list.
                    neighbourNode.flags = neighbourNode.flags or Node.OPEN
                    openList.offer(neighbourNode)
                }

                // Update nearest node to target so far.
                if (heuristicCost < lastBestNodeCost) {
                    lastBestNodeCost = heuristicCost
                    lastBestNode = neighbourNode
                }
                i = bestTile.links[i].indexOfNextLink
            }
        }
        val path = LongArrayList()
        getPathToNode(lastBestNode, path)
        if (lastBestNode.polygonRef != endRef) {
            status = Status.PARTIAL_RESULT
        }
        return Result.of(status, path)
    }

    fun initSlicedFindPath(
        startRef: Long, endRef: Long,
        startPos: Vector3f, endPos: Vector3f,
        filter: QueryFilter, options: Int, raycastLimit: Float
    ): Status = initSlicedFindPath(
        startRef, endRef, startPos, endPos,
        filter, options, DefaultQueryHeuristic(), raycastLimit
    )

    /**
     * Intializes a sliced path query.
     *
     *
     * Common use case: -# Call initSlicedFindPath() to initialize the sliced path query. -# Call updateSlicedFindPath()
     * until it returns complete. -# Call finalizeSlicedFindPath() to get the path.
     *
     * @param startRef The reference id of the start polygon.
     * @param endRef   The reference id of the end polygon.
     * @param startPos A position within the start polygon. [(x, y, z)]
     * @param endPos   A position within the end polygon. [(x, y, z)]
     * @param filter   The polygon filter to apply to the query.
     * @param options  query options (see: #FindPathOptions)
     */
    @JvmOverloads
    fun initSlicedFindPath(
        startRef: Long,
        endRef: Long,
        startPos: Vector3f,
        endPos: Vector3f,
        filter: QueryFilter,
        options: Int,
        heuristic: QueryHeuristic = DefaultQueryHeuristic(),
        raycastLimit: Float = -1f
    ): Status {

        // Init path state.
        queryData = QueryData()
        queryData.status = Status.FAILURE
        queryData.startRef = startRef
        queryData.endRef = endRef
        queryData.startPos.set(startPos)
        queryData.endPos.set(endPos)
        queryData.filter = filter
        queryData.options = options
        queryData.heuristic = heuristic
        queryData.raycastLimitSqr = raycastLimit * raycastLimit

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !nav1.isValidPolyRef(endRef)
            || !startPos.isFinite || !endPos.isFinite
        ) return Status.FAILURE_INVALID_PARAM

        // trade quality with performance?
        if (options and DT_FINDPATH_ANY_ANGLE != 0 && raycastLimit < 0f) {
            // limiting to several times the character radius yields nice results. It is not sensitive
            // so it is enough to compute it from the first tile.
            val tile = nav1.getTileByRef(startRef)
            val agentRadius = tile!!.data.walkableRadius
            queryData.raycastLimitSqr = Vectors.sq(agentRadius * NavMesh.DT_RAY_CAST_LIMIT_PROPORTIONS)
        }
        if (startRef == endRef) {
            queryData.status = Status.SUCCESS
            return Status.SUCCESS
        }
        nodePool.clear()
        openList.clear()
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(startPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = heuristic.getCost(startPos, endPos)
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        queryData.status = Status.IN_PROGRESS
        queryData.lastBestNode = startNode
        queryData.lastBestNodeCost = startNode.totalCost
        return queryData.status
    }

    /**
     * Updates an in-progress sliced path query.
     *
     * @param maxIter The maximum number of iterations to perform.
     * @return The status flags for the query.
     */
    open fun updateSlicedFindPath(maxIter: Int): Result<Int> {
        if (!queryData.status.isInProgress) {
            return Result.of(queryData.status, 0)
        }

        // Make sure the request is still valid.
        if (!nav1.isValidPolyRef(queryData.startRef) || !nav1.isValidPolyRef(queryData.endRef)) {
            queryData.status = Status.FAILURE
            return Result.of(queryData.status, 0)
        }
        var iter = 0
        val tmp = PortalResult()
        while (iter < maxIter && !openList.isEmpty()) {
            iter++

            // Remove node from open list and put it in closed list.
            val bestNode: Node = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED

            // Reached the goal, stop searching.
            if (bestNode.polygonRef == queryData.endRef) {
                queryData.lastBestNode = bestNode
                queryData.status = Status.SUCCESS
                return Result.of(queryData.status, iter)
            }

            // Get current poly and tile.
            // The API input has been checked already, skip checking internal
            // data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRef(bestRef)
            val bestPoly = nav1.getPolyByRef(bestRef, bestTile)
            if (bestPoly == null) {
                queryData.status = Status.FAILURE
                // The polygon has disappeared during the sliced query, fail.
                return Result.of(queryData.status, iter)
            }
            // Get parent and grand parent poly and tile.
            var parentRef = 0L
            var grandpaRef = 0L
            var parentTile: MeshTile? = null
            var parentPoly: Poly? = null
            var parentNode: Node? = null
            if (bestNode.parentIndex != 0) {
                parentNode = nodePool.getNodeAtIdx(bestNode.parentIndex)
                parentRef = parentNode!!.polygonRef
                if (parentNode.parentIndex != 0) {
                    grandpaRef = nodePool.getNodeAtIdx(parentNode.parentIndex)!!.polygonRef
                }
            }
            if (parentRef != 0L) {
                parentTile = nav1.getTileByRef(parentRef)
                parentPoly = nav1.getPolyByRef(parentRef, parentTile)
                if (parentPoly == null || grandpaRef != 0L && !nav1.isValidPolyRef(grandpaRef)) {
                    // The polygon has disappeared during the sliced query,
                    // fail.
                    queryData.status = Status.FAILURE
                    return Result.of(queryData.status, iter)
                }
            }

            // decide whether to test raycast to previous nodes
            var tryLOS = false
            if (queryData.options and DT_FINDPATH_ANY_ANGLE != 0) {
                if (parentRef != 0L && (queryData.raycastLimitSqr >= Float.MAX_VALUE
                            || parentNode!!.pos.distanceSquared(bestNode.pos) < queryData.raycastLimitSqr)
                ) {
                    tryLOS = true
                }
            }
            var i = bestTile!!.polyLinks[bestPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val neighbourRef = bestTile.links[i].neighborRef

                // Skip invalid ids and do not expand back to where we came
                // from.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Get neighbour poly and tile.
                // The API input has been checked already, skip checking internal
                // data.
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)
                if (!queryData.filter!!.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // get the neighbor node
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef, 0)

                // do not expand to nodes that were already visited from the same parent
                if (neighbourNode.parentIndex != 0 && neighbourNode.parentIndex == bestNode.parentIndex) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // If the node is visited the first time, calculate node position.
                var neighbourPos = neighbourNode.pos
                val midpod = if (neighbourRef == queryData.endRef) getEdgeIntersectionPoint(
                    bestNode.pos, bestRef, bestPoly, bestTile, queryData.endPos,
                    neighbourRef, neighbourPoly, neighbourTile, tmp
                ) else getEdgeMidPoint(bestRef, bestPoly, bestTile, neighbourRef, neighbourPoly, neighbourTile, tmp)
                if (midpod != null) {
                    neighbourPos = midpod
                }

                // Calculate cost and heuristic.
                var cost = 0f
                var heuristic: Float

                // raycast parent
                var foundShortCut = false
                var shortcut: LongArrayList? = null
                if (tryLOS) {
                    val rayHit = raycast(
                        parentRef, parentNode!!.pos, neighbourPos, queryData.filter!!,
                        DT_RAYCAST_USE_COSTS, grandpaRef
                    )
                    if (rayHit.succeeded()) {
                        foundShortCut = rayHit.result!!.t >= 1f
                        if (foundShortCut) {
                            shortcut = rayHit.result.path
                            // shortcut found using raycast. Using shorter cost
                            // instead
                            cost = parentNode.cost + rayHit.result.pathCost
                        }
                    }
                }

                // update move cost
                if (!foundShortCut) {
                    // No shortcut found.
                    val curCost = queryData.filter!!.getCost(
                        bestNode.pos, neighbourPos, parentRef, parentTile,
                        parentPoly, bestRef, bestTile, bestPoly, neighbourRef, neighbourTile, neighbourPoly
                    )
                    cost = bestNode.cost + curCost
                }

                // Special case for last node.
                if (neighbourRef == queryData.endRef) {
                    val endCost = queryData.filter!!.getCost(
                        neighbourPos, queryData.endPos, bestRef, bestTile,
                        bestPoly, neighbourRef, neighbourTile, neighbourPoly, 0, null, null
                    )
                    cost += endCost
                    heuristic = 0f
                } else {
                    heuristic = queryData.heuristic!!.getCost(neighbourPos, queryData.endPos)
                }
                val total = cost + heuristic

                // The node is already in open list and the new result is worse,
                // skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                // The node is already visited and process, and the new result
                // is worse, skip.
                if (neighbourNode.flags and Node.CLOSED != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Add or update the node.
                neighbourNode.parentIndex = if (foundShortCut) bestNode.parentIndex else nodePool.getNodeIdx(bestNode)
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.flags = neighbourNode.flags and Node.CLOSED.inv()
                neighbourNode.cost = cost
                neighbourNode.totalCost = total
                neighbourNode.pos.set(neighbourPos)
                neighbourNode.shortcut = shortcut
                if (neighbourNode.flags and Node.OPEN != 0) {
                    // Already in open, update node location.
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    // Put the node in open list.
                    neighbourNode.flags = neighbourNode.flags or Node.OPEN
                    openList.offer(neighbourNode)
                }

                // Update nearest node to target so far.
                if (heuristic < queryData.lastBestNodeCost) {
                    queryData.lastBestNodeCost = heuristic
                    queryData.lastBestNode = neighbourNode
                }
                i = bestTile.links[i].indexOfNextLink
            }
        }

        // Exhausted all nodes, but could not find path.
        if (openList.isEmpty()) {
            queryData.status = Status.PARTIAL_RESULT
        }
        return Result.of(queryData.status, iter)
    }

    /**
     * Finalizes and returns the results of a sliced path query.
     * @param path An ordered list of polygon references representing the path. (Start to end.)
     * [(polyRef) * @p pathCount]
     * @returns The status flags for the query.
     */
    open fun finalizeSlicedFindPath(): Result<LongArrayList> {
        if (queryData.status.isFailed) {
            // Reset query.
            queryData = QueryData()
            return Result.failure(LongArrayList.empty)
        }
        val path = if (queryData.startRef == queryData.endRef) {
            // Special case: the search starts and ends at same poly.
            val path = LongArrayList(1)
            path.add(queryData.startRef)
            path
        } else {
            // Reverse the path.
            val lastBestNode = queryData.lastBestNode!!
            if (lastBestNode.polygonRef != queryData.endRef) {
                queryData.status = Status.PARTIAL_RESULT
            }
            val path = LongArrayList()
            getPathToNode(lastBestNode, path)
            path
        }
        val status = queryData.status
        // Reset query.
        queryData = QueryData()
        return Result.of(status, path)
    }

    /**
     * Finalizes and returns the results of an incomplete sliced path query, returning the path to the furthest
     * polygon on the existing path that was visited during the search.
     * @param existing An array of polygon references for the existing path.
     * @param existingSize The number of polygon in the @p existing array.
     * @param path An ordered list of polygon references representing the path. (Start to end.)
     * [(polyRef) * @p pathCount]
     * @returns The status flags for the query.
     */
    open fun finalizeSlicedFindPathPartial(existing: LongArrayList): Result<LongArrayList> {
        if (existing.isEmpty()) {
            return Result.failure(LongArrayList.empty)
        }
        if (queryData.status.isFailed) {
            // Reset query.
            queryData = QueryData()
            return Result.failure(LongArrayList.empty)
        }
        val path = if (queryData.startRef == queryData.endRef) {
            // Special case: the search starts and ends at same poly.
            val path = LongArrayList(1)
            path.add(queryData.startRef)
            path
        } else {
            // Find the furthest existing node that was visited.
            var node: Node? = null
            for (i in existing.size - 1 downTo 0) {
                node = nodePool.findNode(existing[i])
                if (node != null) {
                    break
                }
            }
            if (node == null) {
                queryData.status = Status.PARTIAL_RESULT
                node = queryData.lastBestNode!!
            }
            val path = LongArrayList()
            getPathToNode(node, path)
            path
        }
        val status = queryData.status
        // Reset query.
        queryData = QueryData()
        return Result.of(status, path)
    }

    fun appendVertex(
        pos: Vector3f, flags: Int, ref: Long,
        straightPath: MutableList<StraightPathItem>,
        maxStraightPath: Int
    ): Status {
        if (straightPath.size > 0 && Vectors.vEqual(straightPath[straightPath.size - 1].pos, pos)) {
            // The vertices are equal, update flags and poly.
            straightPath[straightPath.size - 1].flags = flags
            straightPath[straightPath.size - 1].ref = ref
        } else {
            if (straightPath.size < maxStraightPath) {
                // Append new vertex.
                straightPath.add(StraightPathItem.create(pos, flags, ref))
            }
            // If reached end of path or there is no space to append more vertices, return.
            if (flags == DT_STRAIGHTPATH_END || straightPath.size >= maxStraightPath) {
                return Status.SUCCESS
            }
        }
        return Status.IN_PROGRESS
    }

    fun appendPortals(
        startIdx: Int, endIdx: Int, endPos: Vector3f, path: LongArrayList,
        straightPath: MutableList<StraightPathItem>, maxStraightPath: Int, options: Int
    ): Status {
        val startPos = straightPath[straightPath.size - 1].pos
        // Append or update last vertex
        var stat: Status
        val tmp = PortalResult()
        for (i in startIdx until endIdx) {
            // Calculate portal
            val from = path[i]
            val fromTile = nav1.getTileByRef(from) ?: return Status.FAILURE
            val fromPoly = nav1.getPolyByRef(from, fromTile) ?: return Status.FAILURE
            val to = path[i + 1]
            val toTile = nav1.getTileByRef(to) ?: return Status.FAILURE
            val toPoly = nav1.getPolyByRef(to, toTile) ?: return Status.FAILURE
            val portals = getPortalPoints(from, fromPoly, fromTile, to, toPoly, toTile, 0, 0, tmp) ?: break

            val left = portals.left
            val right = portals.right
            if (options and DT_STRAIGHTPATH_AREA_CROSSINGS != 0) {
                // Skip intersection if only area crossings are requested.
                if (fromPoly.area == toPoly.area) {
                    continue
                }
            }

            // Append intersection
            val intersect = Vectors.intersectSegSeg2D(startPos, endPos, left, right)
            if (intersect != null) {
                val t = intersect.second
                stat = appendVertex(left.lerp(right, t), 0, path[i + 1], straightPath, maxStraightPath)
                if (!stat.isInProgress) {
                    return stat
                }
            }
        }
        return Status.IN_PROGRESS
    }

    /**
     * Finds the straight path from the start to the end position within the polygon corridor.
     *
     * This method peforms what is often called 'string pulling'.
     *
     * The start position is clamped to the first polygon in the path, and the
     * end position is clamped to the last. So the start and end positions should
     * normally be within or very near the first and last polygons respectively.
     *
     * The returned polygon references represent the reference id of the polygon
     * that is entered at the associated path position. The reference id associated
     * with the end point will always be zero. This allows, for example, matching
     * off-mesh link points to their representative polygons.
     *
     * If the provided result buffers are too small for the entire result set,
     * they will be filled as far as possible from the start toward the end
     * position.
     *
     * @param startPos Path start position. [(x, y, z)]
     * @param endPos Path end position. [(x, y, z)]
     * @param path An array of polygon references that represent the path corridor.
     * @param straightPath Points describing the straight path. [(x, y, z) * @p straightPathCount].
     * @param maxStraightPath The maximum number of points the straight path arrays can hold. [Limit: > 0]
     * @param options Query options. (see: #dtStraightPathOptions)
     * @returns The status flags for the query.
     */
    fun findStraightPath(
        startPos: Vector3f, endPos: Vector3f, path: LongArrayList,
        maxStraightPath: Int, options: Int, tmp: PortalResult,
        tmpVertices: FloatArray, tmpEdges0: FloatArray, tmpEdges1: FloatArray
    ): ArrayList<StraightPathItem>? {
        val straightPath = ArrayList<StraightPathItem>()
        if ((!startPos.isFinite || !endPos.isFinite
                    || path.isEmpty()) || path[0] == 0L || maxStraightPath <= 0
        ) return null

        // TODO: Should this be callers responsibility?
        val closestStartPosRes = closestPointOnPolyBoundary(
            path[0], startPos, tmpVertices, tmpEdges0, tmpEdges1
        ) ?: return null
        var closestEndPosRes = closestPointOnPolyBoundary(
            path[path.size - 1], endPos, tmpVertices, tmpEdges0, tmpEdges1
        ) ?: return null
        var closestEndPos = closestEndPosRes
        // Add start point.
        var stat = appendVertex(closestStartPosRes, DT_STRAIGHTPATH_START, path[0], straightPath, maxStraightPath)
        if (!stat.isInProgress) {
            return straightPath
        }
        if (path.size > 1) {
            var portalApex = closestStartPosRes
            var portalLeft = portalApex
            var portalRight = portalApex
            var apexIndex = 0
            var leftIndex = 0
            var rightIndex = 0
            var leftPolyType = 0
            var rightPolyType = 0
            var leftPolyRef: Long = path[0]
            var rightPolyRef: Long = path[0]
            var i = 0
            while (i < path.size) {
                var left: Vector3f
                var right: Vector3f
                var toType: Int
                if (i + 1 < path.size) {
                    // Next portal.
                    val portalPoints = getPortalPoints(path[i], path[i + 1], tmp)
                    if (portalPoints == null) {
                        closestEndPosRes = closestPointOnPolyBoundary(
                            path[i], endPos,
                            tmpVertices, tmpEdges0, tmpEdges1
                        ) ?: return null
                        closestEndPos = closestEndPosRes
                        // Append portals along the current straight path segment.
                        if (options and (DT_STRAIGHTPATH_AREA_CROSSINGS or DT_STRAIGHTPATH_ALL_CROSSINGS) != 0) {
                            // Ignore status return value as we're just about to return anyway.
                            appendPortals(apexIndex, i, closestEndPos, path, straightPath, maxStraightPath, options)
                        }
                        // Ignore status return value as we're just about to return anyway.
                        appendVertex(closestEndPos, 0, path[i], straightPath, maxStraightPath)
                        return straightPath
                    }
                    left = portalPoints.left
                    right = portalPoints.right
                    toType = portalPoints.toType

                    // If starting really close the portal, advance.
                    if (i == 0) {
                        val (first) = Vectors.distancePtSegSqr2D(portalApex, left, right)
                        if (first < 1e-6f) {
                            ++i
                            continue
                        }
                    }
                } else {
                    // End of the path.
                    left = closestEndPos
                    right = closestEndPos
                    toType = Poly.DT_POLYTYPE_GROUND
                }

                // Right vertex.
                if (Vectors.triArea2D(portalApex, portalRight, right) <= 0f) {
                    if (Vectors.vEqual(portalApex, portalRight) ||
                        Vectors.triArea2D(portalApex, portalLeft, right) > 0f
                    ) {
                        portalRight = (right)
                        rightPolyRef = if (i + 1 < path.size) path[i + 1] else 0
                        rightPolyType = toType
                        rightIndex = i
                    } else {
                        // Append portals along the current straight path segment.
                        if (options and (DT_STRAIGHTPATH_AREA_CROSSINGS or DT_STRAIGHTPATH_ALL_CROSSINGS) != 0) {
                            stat = appendPortals(
                                apexIndex, leftIndex, portalLeft, path, straightPath, maxStraightPath,
                                options
                            )
                            if (!stat.isInProgress) {
                                return straightPath
                            }
                        }
                        portalApex = (portalLeft)
                        apexIndex = leftIndex
                        var flags = 0
                        if (leftPolyRef == 0L) {
                            flags = DT_STRAIGHTPATH_END
                        } else if (leftPolyType == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                            flags = DT_STRAIGHTPATH_OFFMESH_CONNECTION
                        }

                        // Append or update vertex
                        stat = appendVertex(portalApex, flags, leftPolyRef, straightPath, maxStraightPath)
                        if (!stat.isInProgress) {
                            return straightPath
                        }
                        portalLeft = (portalApex)
                        portalRight = (portalApex)
                        rightIndex = apexIndex

                        // Restart
                        i = apexIndex
                        ++i
                        continue
                    }
                }

                // Left vertex.
                if (Vectors.triArea2D(portalApex, portalLeft, left) >= 0f) {
                    if (Vectors.vEqual(portalApex, portalLeft) ||
                        Vectors.triArea2D(portalApex, portalRight, left) < 0f
                    ) {
                        portalLeft = (left)
                        leftPolyRef = if (i + 1 < path.size) path[i + 1] else 0
                        leftPolyType = toType
                        leftIndex = i
                    } else {
                        // Append portals along the current straight path segment.
                        if (options and (DT_STRAIGHTPATH_AREA_CROSSINGS or DT_STRAIGHTPATH_ALL_CROSSINGS) != 0) {
                            stat = appendPortals(
                                apexIndex, rightIndex, portalRight, path, straightPath,
                                maxStraightPath, options
                            )
                            if (!stat.isInProgress) {
                                return straightPath
                            }
                        }
                        portalApex = (portalRight)
                        apexIndex = rightIndex
                        var flags = 0
                        if (rightPolyRef == 0L) {
                            flags = DT_STRAIGHTPATH_END
                        } else if (rightPolyType == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                            flags = DT_STRAIGHTPATH_OFFMESH_CONNECTION
                        }

                        // Append or update vertex
                        stat = appendVertex(portalApex, flags, rightPolyRef, straightPath, maxStraightPath)
                        if (!stat.isInProgress) {
                            return straightPath
                        }
                        portalLeft = portalApex
                        portalRight = portalApex
                        leftIndex = apexIndex

                        // Restart
                        i = apexIndex
                    }
                }
                ++i
            }

            // Append portals along the current straight path segment.
            if (options and (DT_STRAIGHTPATH_AREA_CROSSINGS or DT_STRAIGHTPATH_ALL_CROSSINGS) != 0) {
                stat =
                    appendPortals(apexIndex, path.size - 1, closestEndPos, path, straightPath, maxStraightPath, options)
                if (!stat.isInProgress) {
                    return straightPath
                }
            }
        }

        // Ignore status return value as we're just about to return anyway.
        appendVertex(closestEndPos, DT_STRAIGHTPATH_END, 0, straightPath, maxStraightPath)
        return straightPath
    }

    /**
     * This method is optimized for small delta movement and a small number of
     * polygons. If used for too great a distance, the result set will form an
     * incomplete path.
     *
     * @p resultPos will equal the @p endPos if the end is reached.
     * Otherwise the closest reachable position will be returned.
     *
     * @p resultPos is not projected onto the surface of the navigation
     * mesh. Use #getPolyHeight if this is needed.
     *
     * This method treats the end position in the same manner as
     * the #raycast method. (As a 2D point.) See that method's documentation
     * for details.
     *
     * If the @p visited array is too small to hold the entire result set, it will
     * be filled as far as possible from the start position toward the end
     * position.
     *
     * Moves from the start to the end position constrained to the navigation mesh.
     * @param startRef The reference id of the start polygon.
     * @param startPos A position of the mover within the start polygon. [(x, y, x)]
     * @param endPos The desired end position of the mover. [(x, y, z)]
     * @param filter The polygon filter to apply to the query.
     * @returns Path
     */
    fun moveAlongSurface(
        startRef: Long, startPos: Vector3f, endPos: Vector3f,
        filter: QueryFilter, tinyNodePool: NodePool,
        tmpVertices: FloatArray, neis: LongArray,
        visited: LongArrayList
    ): MoveAlongSurfaceResult? {

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !startPos.isFinite || !endPos.isFinite) {
            return null
        }

        tinyNodePool.clear()
        val startNode = tinyNodePool.getOrCreateNode(startRef)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = 0f
        startNode.polygonRef = startRef
        startNode.flags = Node.CLOSED

        val stack = LinkedList<Node>()
        stack.add(startNode)

        val bestPos = Vector3f(startPos)
        var bestDist = Float.MAX_VALUE
        var bestNode: Node? = null

        // Search constraints
        val searchPosX = (startPos.x + endPos.x) * 0.5f
        val searchPosZ = (startPos.z + endPos.z) * 0.5f
        val searchRad = startPos.distance(endPos) * 0.5f + 0.001f
        val searchRadSqr = searchRad * searchRad
        while (!stack.isEmpty()) {
            // Pop front.
            val curNode: Node = stack.pop()

            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val curRef = curNode.polygonRef
            val curTile = nav1.getTileByRefUnsafe(curRef)
            val curPoly = nav1.getPolyByRefUnsafe(curRef, curTile)

            // Collect vertices.
            val nvertices = curPoly.vertCount
            val srcVertices = curTile.data.vertices
            for (i in 0 until nvertices) {
                val srcI = curPoly.vertices[i] * 3
                srcVertices.copyInto(tmpVertices, i * 3, srcI, srcI + 3)
            }

            // If target is inside the poly, stop search.
            if (Vectors.pointInPolygon(endPos, tmpVertices, nvertices)) {
                bestNode = curNode
                bestPos.set(endPos)
                break
            }

            // Find wall edges and find the nearest point inside the walls.
            var i = 0
            var j = curPoly.vertCount - 1
            while (i < curPoly.vertCount) {

                // Find links to neighbours.
                var nneis = 0
                if (curPoly.neighborData[j] and NavMesh.DT_EXT_LINK != 0) {
                    // Tile border.
                    var k = curTile.polyLinks[curPoly.index]
                    while (k != NavMesh.DT_NULL_LINK) {
                        val link = curTile.links[k]
                        if (link.indexOfPolyEdge == j) {
                            if (link.neighborRef != 0L) {
                                val ref = link.neighborRef
                                val neiTile = nav1.getTileByRefUnsafe(ref)
                                val neiPoly = nav1.getPolyByRefUnsafe(ref, neiTile)
                                if (filter.passFilter(ref, neiTile, neiPoly)) {
                                    if (nneis < MAX_NEIS) neis[nneis++] = ref
                                }
                            }
                        }
                        k = curTile.links[k].indexOfNextLink
                    }
                } else if (curPoly.neighborData[j] != 0) {
                    val idx = curPoly.neighborData[j] - 1
                    val ref = nav1.getPolyRefBase(curTile) or idx.toLong()
                    if (filter.passFilter(ref, curTile, curTile.data.polygons[idx])) {
                        // Internal edge, encode id.
                        neis[nneis++] = ref
                    }
                }
                if (nneis == 0) {
                    // Wall edge, calc distance.
                    val vj = j * 3
                    val vi = i * 3
                    val (distSqr, tseg) = Vectors.distancePtSegSqr2D(endPos, tmpVertices, vj, vi)
                    if (distSqr < bestDist) {
                        // Update nearest distance.
                        Vectors.lerp(tmpVertices, vj, vi, tseg, bestPos)
                        bestDist = distSqr
                        bestNode = curNode
                    }
                } else {
                    for (k in 0 until nneis) {
                        val neighbourNode = tinyNodePool.getOrCreateNode(neis[k])
                        // Skip if already visited.
                        if (neighbourNode.flags and Node.CLOSED != 0) {
                            continue
                        }

                        // Skip the link if it is too far from search constraint.
                        // TODO: Maybe should use getPortalPoints(), but this one is way faster.
                        val distSqr = Vectors.distancePtSegSqr2DFirst(searchPosX, searchPosZ, tmpVertices, j * 3, i * 3)
                        if (distSqr > searchRadSqr) {
                            continue
                        }

                        // Mark as the node as visited and push to queue.
                        neighbourNode.parentIndex = tinyNodePool.getNodeIdx(curNode)
                        neighbourNode.flags = neighbourNode.flags or Node.CLOSED
                        stack.add(neighbourNode)
                    }
                }
                j = i++
            }
        }

        visited.clear()
        if (bestNode != null) {
            reversePath(bestNode, tinyNodePool, visited)
        }
        return MoveAlongSurfaceResult(bestPos, visited)
    }

    private fun reversePath(bestNode: Node, tinyNodePool: NodePool, visited: LongArrayList) {
        // Reverse the path.
        var prev: Node? = null
        var node: Node = bestNode
        while (true) {
            val next = tinyNodePool.getNodeAtIdx(node.parentIndex)
            node.parentIndex = tinyNodePool.getNodeIdx(prev)
            prev = node
            node = next ?: break
        }

        // Store result
        var nodeI = prev!!
        while (true) {
            visited.add(nodeI.polygonRef)
            nodeI = tinyNodePool.getNodeAtIdx(nodeI.parentIndex) ?: break
        }
    }

    class PortalResult(val left: Vector3f, val right: Vector3f, var fromType: Int, var toType: Int) {
        constructor() : this(Vector3f(), Vector3f(), 0, 0)
    }

    fun getPortalPoints(from: Long, to: Long, dst: PortalResult): PortalResult? {
        val fromTile = nav1.getTileByRef(from) ?: return null
        val fromPoly = nav1.getPolyByRef(from, fromTile) ?: return null
        val toTile = nav1.getTileByRef(to) ?: return null
        val toPoly = nav1.getPolyByRef(to, toTile) ?: return null
        return getPortalPoints(from, fromPoly, fromTile, to, toPoly, toTile, fromPoly.type, toPoly.type, dst)
    }

    // Returns portal points between two polygons.
    fun getPortalPoints(
        from: Long, fromPoly: Poly, fromTile: MeshTile?, to: Long, toPoly: Poly,
        toTile: MeshTile?, fromType: Int, toType: Int, dst: PortalResult
    ): PortalResult? {

        val left = dst.left
        val right = dst.right
        dst.fromType = fromType
        dst.toType = toType

        // Find the link that points to the 'to' polygon.
        var link: Link? = null
        var j = fromTile!!.polyLinks[fromPoly.index]
        while (j != NavMesh.DT_NULL_LINK) {
            if (fromTile.links[j].neighborRef == to) {
                link = fromTile.links[j]
                break
            }
            j = fromTile.links[j].indexOfNextLink
        }
        if (link == null) {
            return null
        }

        // Handle off-mesh connections.
        if (fromPoly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
            // Find link that points to first vertex.
            var i = fromTile.polyLinks[fromPoly.index]
            val fromTileVertices = fromTile.data.vertices
            val fromPolyVertices = fromPoly.vertices
            while (i != NavMesh.DT_NULL_LINK) {
                if (fromTile.links[i].neighborRef == to) {
                    val v = fromTile.links[i].indexOfPolyEdge
                    left.set(fromTileVertices, fromPolyVertices[v] * 3)
                    right.set(fromTileVertices, fromPolyVertices[v] * 3)
                    return dst
                }
                i = fromTile.links[i].indexOfNextLink
            }
            return null
        }
        if (toPoly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION && toTile != null) {
            var i = toTile.polyLinks[toPoly.index]
            val toTileVertices = toTile.data.vertices
            val toPolyVertices = toPoly.vertices
            while (i != NavMesh.DT_NULL_LINK) {
                if (toTile.links[i].neighborRef == from) {
                    val v = toTile.links[i].indexOfPolyEdge
                    left.set(toTileVertices, toPolyVertices[v] * 3)
                    right.set(toTileVertices, toPolyVertices[v] * 3)
                    return dst
                }
                i = toTile.links[i].indexOfNextLink
            }
            return null
        }

        // Find portal vertices.
        val v0 = fromPoly.vertices[link.indexOfPolyEdge]
        val v1 = fromPoly.vertices[(link.indexOfPolyEdge + 1) % fromPoly.vertCount]
        left.set(fromTile.data.vertices, v0 * 3)
        right.set(fromTile.data.vertices, v1 * 3)

        // If the link is at tile boundary, dtClamp the vertices to
        // the link width.
        if (link.side != 0xff) {
            // Unpack portal limits.
            if (link.bmin != 0 || link.bmax != 255) {
                val s = 1f / 255f
                val tmin = link.bmin * s
                val tmax = link.bmax * s
                Vectors.lerp(fromTile.data.vertices, v0 * 3, v1 * 3, tmin, left)
                Vectors.lerp(fromTile.data.vertices, v0 * 3, v1 * 3, tmax, right)
            }
        }
        return dst
    }

    fun getEdgeMidPoint(
        from: Long, fromPoly: Poly, fromTile: MeshTile?,
        to: Long, toPoly: Poly, toTile: MeshTile?,
        tmp: PortalResult
    ): Vector3f? {
        val portal = getPortalPoints(from, fromPoly, fromTile, to, toPoly, toTile, 0, 0, tmp) ?: return null
        val left = portal.left
        val right = portal.right
        return left.add(right).mul(0.5f)
    }

    fun getEdgeIntersectionPoint(
        fromPos: Vector3f, from: Long, fromPoly: Poly, fromTile: MeshTile?,
        toPos: Vector3f, to: Long, toPoly: Poly, toTile: MeshTile?, tmp: PortalResult
    ): Vector3f? {
        val portal = getPortalPoints(from, fromPoly, fromTile, to, toPoly, toTile, 0, 0, tmp) ?: return null
        val left = portal.left
        val right = portal.right
        var t = 0.5f
        val intersect = Vectors.intersectSegSeg2D(fromPos, toPos, left, right)
        if (intersect != null) {
            t = Vectors.clamp(intersect.second, 0.1f, 0.9f)
        }
        return left.lerp(right, t)
    }

    /**
     * This method is meant to be used for quick, short distance checks.
     *
     * If the path array is too small to hold the result, it will be filled as
     * far as possible from the start position toward the end position.
     *
     * <b>Using the Hit Parameter t of RaycastHit</b>
     *
     * If the hit parameter is a very high value (FLT_MAX), then the ray has hit
     * the end position. In this case the path represents a valid corridor to the
     * end position, and the value of @p hitNormal is undefined.
     *
     * If the hit parameter is zero, then the start position is on the wall that
     * was hit, and the value of @p hitNormal is undefined.
     *
     * If 0 < t < 1.0 then the following applies:
     *
     * @code
     * distanceToHitBorder = distanceToEndPosition * t
     * hitPoint = startPos + (endPos - startPos) * t
     * @endcode
     *
     * <b>Use Case Restriction</b>
     *
     * The raycast ignores the y-value of the end position. (2D check.) This
     * places significant limits on how it can be used. For example:
     *
     * Consider a scene where there is a main floor with a second floor balcony
     * that hangs over the main floor. So the first floor mesh extends below the
     * balcony mesh. The start position is somewhere on the first floor. The end
     * position is on the balcony.
     *
     * The raycast will search toward the end position along the first floor mesh.
     * If it reaches the end position's xz-coordinates it will indicate FLT_MAX
     * (no wall hit), meaning it reached the end position. This is one example of why
     * this method is meant for short distance checks.
     *
     * Casts a 'walkability' ray along the surface of the navigation mesh from
     * the start position toward the end position.
     * @note A wrapper around raycast(..., RaycastHit*). Retained for backward compatibility.
     * @param startRef The reference id of the start polygon.
     * @param startPos A position within the start polygon representing
     * the start of the ray. [(x, y, z)]
     * @param endPos The position to cast the ray toward. [(x, y, z)]
     * @param t The hit parameter. (FLT_MAX if no wall hit.)
     * @param hitNormal The normal of the nearest wall hit. [(x, y, z)]
     * @param filter The polygon filter to apply to the query.
     * @param path The reference ids of the visited polygons. [opt]
     * @param pathCount The number of visited polygons. [opt]
     * @param maxPath The maximum number of polygons the @p path array can hold.
     * @returns The status flags for the query.
     */
    fun raycast(
        startRef: Long, startPos: Vector3f, endPos: Vector3f, filter: QueryFilter, options: Int,
        prevRef: Long
    ): Result<RaycastHit> {
        // Validate input
        var prevRef = prevRef
        if ((!nav1.isValidPolyRef(startRef) || !startPos.isFinite || !endPos.isFinite) || prevRef != 0L &&
            !nav1.isValidPolyRef(prevRef)
        ) return Result.invalidParam()

        val hit = RaycastHit()
        val vertices = FloatArray(nav1.maxVerticesPerPoly * 3 + 3)
        val curPos = Vector3f(startPos)
        val lastPos = Vector3f()
        val dir = endPos.sub(startPos, Vector3f())
        var prevTile: MeshTile?
        var tile: MeshTile?
        var nextTile: MeshTile?
        var prevPoly: Poly
        var poly: Poly
        var nextPoly: Poly

        // The API input has been checked already, skip checking internal data.
        var curRef = startRef
        tile = nav1.getTileByRefUnsafe(curRef)
        poly = nav1.getPolyByRefUnsafe(curRef, tile)
        prevTile = tile
        nextTile = prevTile
        prevPoly = poly
        nextPoly = prevPoly
        if (prevRef != 0L) {
            prevTile = nav1.getTileByRefUnsafe(prevRef)
            prevPoly = nav1.getPolyByRefUnsafe(prevRef, prevTile)
        }
        while (curRef != 0L) {
            // Cast ray against current polygon.

            // Collect vertices.
            var nv = 0
            for (i in 0 until poly.vertCount) {
                System.arraycopy(tile!!.data.vertices, poly.vertices[i] * 3, vertices, nv * 3, 3)
                nv++
            }
            val iresult = Vectors.intersectSegmentPoly2D(startPos, endPos, vertices, nv)
            if (!iresult.intersects) {
                // Could not hit the polygon, keep the old t and report hit.
                return Result.success(hit)
            }
            hit.hitEdgeIndex = iresult.segMax

            // Keep track of furthest t so far.
            if (iresult.tmax > hit.t) {
                hit.t = iresult.tmax
            }

            // Store visited polygons.
            hit.path.add(curRef)

            // Ray end is completely inside the polygon.
            if (iresult.segMax == -1) {
                hit.t = Float.MAX_VALUE

                // add the cost
                if (options and DT_RAYCAST_USE_COSTS != 0) {
                    hit.pathCost += filter.getCost(
                        curPos, endPos, prevRef, prevTile, prevPoly, curRef, tile, poly,
                        curRef, tile, poly
                    )
                }
                return Result.success(hit)
            }

            // Follow neighbours.
            var nextRef = 0L
            var i = tile!!.polyLinks[poly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val link = tile.links[i]

                // Find link, which contains this edge.
                if (link.indexOfPolyEdge != iresult.segMax) {
                    i = tile.links[i].indexOfNextLink
                    continue
                }

                // Get pointer to the next polygon.
                nextTile = nav1.getTileByRefUnsafe(link.neighborRef)
                nextPoly = nav1.getPolyByRefUnsafe(link.neighborRef, nextTile)
                // Skip off-mesh connections.
                if (nextPoly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                    i = tile.links[i].indexOfNextLink
                    continue
                }

                // Skip links based on filter.
                if (!filter.passFilter(link.neighborRef, nextTile, nextPoly)) {
                    i = tile.links[i].indexOfNextLink
                    continue
                }

                // If the link is internal, just return the ref.
                if (link.side == 0xff) {
                    nextRef = link.neighborRef
                    break
                }

                // If the link is at tile boundary,

                // Check if the link spans the whole edge, and accept.
                if (link.bmin == 0 && link.bmax == 255) {
                    nextRef = link.neighborRef
                    break
                }

                // Check for partial edge links.
                val v0 = poly.vertices[link.indexOfPolyEdge]
                val v1 = poly.vertices[(link.indexOfPolyEdge + 1) % poly.vertCount]
                val left = v0 * 3
                val right = v1 * 3

                // Check, that the intersection lies inside the link portal.
                val s = 1f / 255f
                if (link.side == 0 || link.side == 4) {
                    // Calculate link size.
                    val vs = tile.data.vertices
                    var lmin = (vs[left + 2] + (vs[right + 2] - vs[left + 2]) * (link.bmin * s))
                    var lmax = (vs[left + 2] + (vs[right + 2] - vs[left + 2]) * (link.bmax * s))
                    if (lmin > lmax) {
                        val temp = lmin
                        lmin = lmax
                        lmax = temp
                    }

                    // Find Z intersection.
                    val z = startPos.z + (endPos.z - startPos.z) * iresult.tmax
                    if (z in lmin..lmax) {
                        nextRef = link.neighborRef
                        break
                    }
                } else if (link.side == 2 || link.side == 6) {
                    // Calculate link size.
                    val vs = tile.data.vertices
                    var lmin = (vs[left] + (vs[right] - vs[left]) * (link.bmin * s))
                    var lmax = (vs[left] + (vs[right] - vs[left]) * (link.bmax * s))
                    if (lmin > lmax) {
                        val temp = lmin
                        lmin = lmax
                        lmax = temp
                    }

                    // Find X intersection.
                    val x = startPos.x + (endPos.x - startPos.x) * iresult.tmax
                    if (x in lmin..lmax) {
                        nextRef = link.neighborRef
                        break
                    }
                }
                i = tile.links[i].indexOfNextLink
            }

            // add the cost
            if (options and DT_RAYCAST_USE_COSTS != 0) {
                // compute the intersection point at the furthest end of the polygon
                // and correct the height (since the raycast moves in 2d)
                lastPos.set(curPos)
                Vectors.mad(startPos, dir, hit.t, curPos)
                val v1 = iresult.segMax * 3
                val v2 = (iresult.segMax + 1) % nv * 3
                val ex = vertices[v2] - vertices[v1]
                val ey = vertices[v2 + 1] - vertices[v1 + 1]
                val ez = vertices[v2 + 2] - vertices[v1 + 2]
                val dx = curPos.x - vertices[v1]
                val dz = curPos.z - vertices[v1 + 2]
                val s = if (ex * ex > ez * ez) dx / ex else dz / ez
                curPos.y = vertices[v1 + 1] + ey * s
                hit.pathCost += filter.getCost(
                    lastPos, curPos, prevRef, prevTile, prevPoly, curRef, tile, poly,
                    nextRef, nextTile, nextPoly
                )
            }
            if (nextRef == 0L) {
                // No neighbour, we hit a wall.
                // Calculate hit normal.
                val a: Int = iresult.segMax
                val b = if (iresult.segMax + 1 < nv) iresult.segMax + 1 else 0
                val va = a * 3
                val vb = b * 3
                hit.hitNormal.x = vertices[vb + 2] - vertices[va + 2]
                hit.hitNormal.y = 0f
                hit.hitNormal.z = -(vertices[vb] - vertices[va])
                hit.hitNormal.normalize()
                return Result.success(hit)
            }

            // No hit, advance to neighbour polygon.
            prevRef = curRef
            curRef = nextRef
            prevTile = tile
            tile = nextTile
            prevPoly = poly
            poly = nextPoly
        }
        return Result.success(hit)
    }

    /**
     * At least one result array must be provided.
     *
     * The order of the result set is from least to highest cost to reach the polygon.
     *
     * A common use case for this method is to perform Dijkstra searches.
     * Candidate polygons are found by searching the graph beginning at the start polygon.
     *
     * If a polygon is not found via the graph search, even if it intersects the
     * search circle, it will not be included in the result set. For example:
     *
     * polyA is the start polygon.
     * polyB shares an edge with polyA. (Is adjacent.)
     * polyC shares an edge with polyB, but not with polyA
     * Even if the search circle overlaps polyC, it will not be included in the
     * result set unless polyB is also in the set.
     *
     * The value of the center point is used as the start position for cost
     * calculations. It is not projected onto the surface of the mesh, so its
     * y-value will effect the costs.
     *
     * Intersection tests occur in 2D. All polygons and the search circle are
     * projected onto the xz-plane. So the y-value of the center point does not
     * effect intersection tests.
     *
     * If the result arrays are to small to hold the entire result set, they will be
     * filled to capacity.
     *
     * @}
     * @name Dijkstra Search Functions
     * @{
     * Finds the polygons along the navigation graph that touch the specified circle.
     * @param startRef The reference id of the polygon where the search starts.
     * @param centerPos The center of the search circle. [(x, y, z)]
     * @param radius The radius of the search circle.
     * @param filter The polygon filter to apply to the query.
     * @param resultRef The reference ids of the polygons touched by the circle. [opt]
     * @param resultParent The reference ids of the parent polygons for each result.
     * Zero if a result polygon has no parent. [opt]
     * @param resultCost The search cost from @p centerPos to the polygon. [opt]
     * @param resultCount The number of polygons found. [opt]
     * @param maxResult The maximum number of polygons the result arrays can hold.
     * @returns The status flags for the query.
     */
    fun findPolysAroundCircle(
        startRef: Long, centerPos: Vector3f, radius: Float,
        filter: QueryFilter
    ): Result<FindPolysAroundResult> {

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !centerPos.isFinite ||
            radius < 0 || !java.lang.Float.isFinite(radius)
        ) {
            return Result.invalidParam()
        }
        val resultRef = LongArrayList()
        val resultParent = LongArrayList()
        val resultCost = FloatArrayList()
        nodePool.clear()
        openList.clear()
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(centerPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = 0f
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        val radiusSqr = radius * radius
        val tmp = PortalResult()
        while (!openList.isEmpty()) {
            val bestNode: Node = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED

            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRefUnsafe(bestRef)
            val bestPoly = nav1.getPolyByRefUnsafe(bestRef, bestTile)

            // Get parent poly and tile.
            var parentRef = 0L
            var parentTile: MeshTile? = null
            var parentPoly: Poly? = null
            if (bestNode.parentIndex != 0) {
                parentRef = nodePool.getNodeAtIdx(bestNode.parentIndex)!!.polygonRef
            }
            if (parentRef != 0L) {
                parentTile = nav1.getTileByRefUnsafe(parentRef)
                parentPoly = nav1.getPolyByRefUnsafe(parentRef, parentTile)
            }
            resultRef.add(bestRef)
            resultParent.add(parentRef)
            resultCost.add(bestNode.totalCost)
            var i = bestTile.polyLinks[bestPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val link = bestTile.links[i]
                val neighbourRef = link.neighborRef
                // Skip invalid neighbours and do not follow back to parent.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Expand to neighbour
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)

                // Do not advance if the polygon is excluded by the filter.
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Find edge and calc distance to the edge.
                val pp = getPortalPoints(
                    bestRef, bestPoly, bestTile, neighbourRef, neighbourPoly,
                    neighbourTile, 0, 0, tmp
                )
                if (pp == null) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val va = pp.left
                val vb = pp.right

                // If the circle is not touching the next polygon, skip it.
                val (distSqr) = Vectors.distancePtSegSqr2D(centerPos, va, vb)
                if (distSqr > radiusSqr) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef)
                if (neighbourNode.flags and Node.CLOSED != 0) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Cost
                if (neighbourNode.flags == 0) {
                    neighbourNode.pos.set(va).add(vb).mul(0.5f)
                }
                val cost = filter.getCost(
                    bestNode.pos, neighbourNode.pos, parentRef, parentTile, parentPoly, bestRef,
                    bestTile, bestPoly, neighbourRef, neighbourTile, neighbourPoly
                )
                val total = bestNode.totalCost + cost
                // The node is already in open list and the new result is worse, skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.parentIndex = nodePool.getNodeIdx(bestNode)
                neighbourNode.totalCost = total
                if (neighbourNode.flags and Node.OPEN != 0) {
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    neighbourNode.flags = Node.OPEN
                    openList.offer(neighbourNode)
                }
                i = bestTile.links[i].indexOfNextLink
            }
        }
        return Result.success(FindPolysAroundResult(resultRef, resultParent, resultCost))
    }

    /**
     * The order of the result set is from least to highest cost.
     *
     * At least one result array must be provided.
     *
     * A common use case for this method is to perform Dijkstra searches.
     * Candidate polygons are found by searching the graph beginning at the start
     * polygon.
     *
     * The same intersection test restrictions that apply to findPolysAroundCircle()
     * method apply to this method.
     *
     * The 3D centroid of the search polygon is used as the start position for cost
     * calculations.
     *
     * Intersection tests occur in 2D. All polygons are projected onto the
     * xz-plane. So the y-values of the vertices do not effect intersection tests.
     *
     * If the result arrays are is too small to hold the entire result set, they will
     * be filled to capacity.
     *
     * Finds the polygons along the naviation graph that touch the specified convex polygon.
     * @param startRef The reference id of the polygon where the search starts.
     * @param vertices The vertices describing the convex polygon. (CCW)
     * [(x, y, z) * @p nvertices]
     * @param nvertices The number of vertices in the polygon.
     * @param filter The polygon filter to apply to the query.
     * @param resultRef The reference ids of the polygons touched by the search polygon. [opt]
     * @param resultParent The reference ids of the parent polygons for each result. Zero if a
     * result polygon has no parent. [opt]
     * @param resultCost The search cost from the centroid point to the polygon. [opt]
     * @param resultCount The number of polygons found.
     * @param maxResult The maximum number of polygons the result arrays can hold.
     * @returns The status flags for the query.
     */
    fun findPolysAroundShape(startRef: Long, vertices: FloatArray, filter: QueryFilter): Result<FindPolysAroundResult> {
        // Validate input
        val nvertices = vertices.size / 3
        if (!nav1.isValidPolyRef(startRef) || nvertices < 3) {
            return Result.invalidParam()
        }

        val resultRef = LongArrayList()
        val resultParent = LongArrayList()
        val resultCost = FloatArrayList()
        nodePool.clear()
        openList.clear()
        val centerPos = Vector3f()
        for (i in 0 until nvertices) {
            centerPos.x += vertices[i * 3]
            centerPos.y += vertices[i * 3 + 1]
            centerPos.z += vertices[i * 3 + 2]
        }
        centerPos.div(nvertices.toFloat())
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(centerPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = 0f
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        val tmp = PortalResult()
        while (!openList.isEmpty()) {
            val bestNode: Node = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED

            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRefUnsafe(bestRef)
            val bestPoly = nav1.getPolyByRefUnsafe(bestRef, bestTile)

            // Get parent poly and tile.
            var parentRef = 0L
            var parentTile: MeshTile? = null
            var parentPoly: Poly? = null
            if (bestNode.parentIndex != 0) {
                parentRef = nodePool.getNodeAtIdx(bestNode.parentIndex)!!.polygonRef
            }
            if (parentRef != 0L) {
                parentTile = nav1.getTileByRefUnsafe(parentRef)
                parentPoly = nav1.getPolyByRefUnsafe(parentRef, parentTile)
            }
            resultRef.add(bestRef)
            resultParent.add(parentRef)
            resultCost.add(bestNode.totalCost)
            var i = bestTile.polyLinks[bestPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val link = bestTile.links[i]
                val neighbourRef = link.neighborRef
                // Skip invalid neighbours and do not follow back to parent.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Expand to neighbour
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)

                // Do not advance if the polygon is excluded by the filter.
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Find edge and calc distance to the edge.
                val pp = getPortalPoints(
                    bestRef, bestPoly, bestTile, neighbourRef, neighbourPoly,
                    neighbourTile, 0, 0, tmp
                )
                if (pp == null) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val va = pp.left
                val vb = pp.right

                // If the poly is not touching the edge to the next polygon, skip the connection it.
                val ir = Vectors.intersectSegmentPoly2D(va, vb, vertices, nvertices)
                if (!ir.intersects) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                if (ir.tmin > 1f || ir.tmax < 0f) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef)
                if (neighbourNode.flags and Node.CLOSED != 0) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }

                // Cost
                if (neighbourNode.flags == 0) {
                    neighbourNode.pos.set(va).add(vb).mul(0.5f)
                }
                val cost = filter.getCost(
                    bestNode.pos, neighbourNode.pos, parentRef, parentTile, parentPoly, bestRef,
                    bestTile, bestPoly, neighbourRef, neighbourTile, neighbourPoly
                )
                val total = bestNode.totalCost + cost

                // The node is already in open list and the new result is worse, skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    i = bestTile.links[i].indexOfNextLink
                    continue
                }
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.parentIndex = nodePool.getNodeIdx(bestNode)
                neighbourNode.totalCost = total
                if (neighbourNode.flags and Node.OPEN != 0) {
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    neighbourNode.flags = Node.OPEN
                    openList.offer(neighbourNode)
                }
                i = bestTile.links[i].indexOfNextLink
            }
        }
        return Result.success(FindPolysAroundResult(resultRef, resultParent, resultCost))
    }

    /**
     * This method is optimized for a small search radius and small number of result polygons.
     *
     * Candidate polygons are found by searching the navigation graph beginning at the start polygon.
     *
     * The same intersection test restrictions that apply to the findPolysAroundCircle method applies to this method.
     *
     * The value of the center point is used as the start point for cost calculations.
     * It is not projected onto the surface of the mesh, so its y-value will effect the costs.
     *
     * Intersection tests occur in 2D. All polygons and the search circle are
     * projected onto the xz-plane. So the y-value of the center point does not effect intersection tests.
     *
     * If the result arrays are is too small to hold the entire result set, they will be filled to capacity.
     *
     * Finds the non-overlapping navigation polygons in the local neighbourhood around the center position.
     * @param startRef The reference id of the polygon where the search starts.
     * @param centerPos The center of the query circle. [(x, y, z)]
     * @param radius The radius of the query circle.
     * @param filter The polygon filter to apply to the query.
     * @param resultRef The reference ids of the polygons touched by the circle.
     * @param resultParent The reference ids of the parent polygons for each result.
     * Zero if a result polygon has no parent. [opt]
     * @param resultCount The number of polygons found.
     * @param maxResult The maximum number of polygons the result arrays can hold.
     * @returns The status flags for the query.
     */
    fun findLocalNeighbourhood(
        startRef: Long, centerPos: Vector3f, radius: Float,
        filter: QueryFilter,
        tinyNodePool: NodePool, pa: FloatArray, pb: FloatArray, // FloatArray(nav1.maxVerticesPerPoly * 3)
        resultRef: LongArrayList, tmp: PortalResult, tmpN: Vector3f,
        stack: LinkedList<Node>
    ): Boolean {

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !centerPos.isFinite || radius < 0 || !radius.isFinite()) {
            return false
        }

        resultRef.clear()
        tinyNodePool.clear()
        val startNode = tinyNodePool.getOrCreateNode(startRef)
        startNode.parentIndex = 0
        startNode.polygonRef = startRef
        startNode.flags = Node.CLOSED
        stack.add(startNode)
        resultRef.add(startNode.polygonRef)
        val radiusSqr = radius * radius

        while (!stack.isEmpty()) {
            // Pop front.
            val curNode: Node = stack.pop()

            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val curRef = curNode.polygonRef
            val curTile = nav1.getTileByRefUnsafe(curRef)
            val curPoly = nav1.getPolyByRefUnsafe(curRef, curTile)
            var i = curTile.polyLinks[curPoly.index]
            while (i != NavMesh.DT_NULL_LINK) {
                val link = curTile.links[i]
                val neighbourRef = link.neighborRef
                // Skip invalid neighbours.
                if (neighbourRef == 0L) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }
                val neighbourNode = tinyNodePool.getOrCreateNode(neighbourRef)
                // Skip visited.
                if (neighbourNode.flags and Node.CLOSED != 0) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }

                // Expand to neighbour
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)

                // Skip off-mesh connections.
                if (neighbourPoly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }

                // Do not advance if the polygon is excluded by the filter.
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }

                // Find edge and calc distance to the edge.
                val pp = getPortalPoints(
                    curRef, curPoly, curTile, neighbourRef, neighbourPoly,
                    neighbourTile, 0, 0, tmp
                )
                if (pp == null) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }
                val va = pp.left
                val vb = pp.right

                // If the circle is not touching the next polygon, skip it.
                val (distSqr) = Vectors.distancePtSegSqr2D(centerPos, va, vb)
                if (distSqr > radiusSqr) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }

                // Mark node visited, this is done before the overlap test so that
                // we will not visit the poly again if the test fails.
                neighbourNode.flags = neighbourNode.flags or Node.CLOSED
                neighbourNode.parentIndex = tinyNodePool.getNodeIdx(curNode)

                // Check, that the polygon does not collide with existing polygons.

                // Collect vertices of the neighbour poly.
                val npa = neighbourPoly.vertCount
                for (k in 0 until npa) {
                    System.arraycopy(neighbourTile.data.vertices, neighbourPoly.vertices[k] * 3, pa, k * 3, 3)
                }
                var overlap = false
                var idx = 0
                val len = resultRef.size
                while (idx < len) {
                    val pastRef: Long = resultRef[idx]
                    // Connected polys do not overlap.
                    var connected = false

                    var k = curTile.polyLinks[curPoly.index]
                    while (k != NavMesh.DT_NULL_LINK) {
                        if (curTile.links[k].neighborRef == pastRef) {
                            connected = true
                            break
                        }
                        k = curTile.links[k].indexOfNextLink
                    }

                    if (connected) {
                        idx++
                        continue
                    }

                    // Potentially overlapping.
                    val pastTile = nav1.getTileByRefUnsafe(pastRef)
                    val pastPoly = nav1.getPolyByRefUnsafe(pastRef, pastTile)

                    // Get vertices and test overlap
                    val npb = pastPoly.vertCount
                    for (l in 0 until npb) {
                        System.arraycopy(pastTile.data.vertices, pastPoly.vertices[l] * 3, pb, l * 3, 3)
                    }
                    if (Vectors.overlapPolyPoly2D(pa, npa, pb, npb, tmpN)) {
                        overlap = true
                        break
                    }
                    idx++
                }
                if (overlap) {
                    i = curTile.links[i].indexOfNextLink
                    continue
                }
                resultRef.add(neighbourRef)
                stack.add(neighbourNode)
                i = curTile.links[i].indexOfNextLink
            }
        }
        return true
    }

    class SegInterval(var ref: Long, var tmin: Int, var tmax: Int)

    fun insertInterval(ints: MutableList<SegInterval>, tmin: Int, tmax: Int, ref: Long) {
        // Find insertion point.
        var idx = 0
        while (idx < ints.size) {
            if (tmax <= ints[idx].tmin) {
                break
            }
            idx++
        }
        // Store
        ints.add(idx, SegInterval(ref, tmin, tmax))
    }

    /**
     * @par
     *
     * If the @p segmentRefs parameter is provided, then all polygon segments will be returned.
     * Otherwise only the wall segments are returned.
     *
     * A segment that is normally a portal will be included in the result set as a
     * wall if the @p filter results in the neighbor polygon becoomming impassable.
     *
     * The @p segmentVertices and @p segmentRefs buffers should normally be sized for the
     * maximum segments per polygon of the source navigation mesh.
     *
     * Returns the segments for the specified polygon, optionally including portals.
     * @param ref The reference id of the polygon.
     * @param filter The polygon filter to apply to the query.
     * @param segmentVertices The segments. [(ax, ay, az, bx, by, bz) * segmentCount]
     * @param segmentRefs The reference ids of each segment's neighbor polygon.
     * Or zero if the segment is a wall. [opt] [(parentRef) * @p segmentCount]
     * @param segmentCount The number of segments returned.
     * @param maxSegments The maximum number of segments the result arrays can hold.
     * @returns The status flags for the query.
     */
    fun getPolyWallSegments(
        ref: Long,
        storePortals: Boolean,
        filter: QueryFilter,
        segmentVertices: ArrayList<FloatArray>,
        ints: ArrayList<SegInterval>,
    ): List<FloatArray>? {

        segmentVertices.clear()
        ints.clear()

        val tile = nav1.getTileByRef(ref) ?: return null
        val poly = nav1.getPolyByRef(ref, tile) ?: return null
        val vs = tile.data.vertices
        var i = 0
        var j = poly.vertCount - 1
        while (i < poly.vertCount) {

            // Skip non-solid edges.
            ints.clear()
            if (poly.neighborData[j] and NavMesh.DT_EXT_LINK != 0) {
                // Tile border.
                var k = tile.polyLinks[poly.index]
                while (k != NavMesh.DT_NULL_LINK) {
                    val link = tile.links[k]
                    if (link.indexOfPolyEdge == j) {
                        if (link.neighborRef != 0L) {
                            val neighbourRef = link.neighborRef
                            val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                            val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)
                            if (filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                                insertInterval(ints, link.bmin, link.bmax, neighbourRef)
                            }
                        }
                    }
                    k = tile.links[k].indexOfNextLink
                }
            } else {
                // Internal edge
                var neiRef = 0L
                if (poly.neighborData[j] != 0) {
                    val idx = poly.neighborData[j] - 1
                    neiRef = nav1.getPolyRefBase(tile) or idx.toLong()
                    if (!filter.passFilter(neiRef, tile, tile.data.polygons[idx])) {
                        neiRef = 0
                    }
                }
                // If the edge leads to another polygon and portals are not stored, skip.
                if (neiRef != 0L && !storePortals) {
                    j = i++
                    continue
                }
                val vj = poly.vertices[j] * 3
                val vi = poly.vertices[i] * 3
                val seg = createSegment()
                System.arraycopy(vs, vj, seg, 0, 3)
                System.arraycopy(vs, vi, seg, 3, 3)
                segmentVertices.add(seg)
                j = i++
                continue
            }

            // Add sentinels
            insertInterval(ints, -1, 0, 0)
            insertInterval(ints, 255, 256, 0)

            // Store segments.
            val vj = poly.vertices[j] * 3
            val vi = poly.vertices[i] * 3
            val inv = 1f / 255f
            for (k in 1 until ints.size) {
                // Portal segment.
                if (storePortals && ints[k].ref != 0L) {
                    val tmin = ints[k].tmin * inv
                    val tmax = ints[k].tmax * inv
                    val seg = createSegment()
                    Vectors.lerp(vs, vj, vi, tmin, seg, 0)
                    Vectors.lerp(vs, vj, vi, tmax, seg, 3)
                    segmentVertices.add(seg)
                }

                // Wall segment.
                val imin = ints[k - 1].tmax
                val imax = ints[k].tmin
                if (imin != imax) {
                    val tmin = imin * inv
                    val tmax = imax * inv
                    val seg = createSegment()
                    Vectors.lerp(vs, vj, vi, tmin, seg, 0)
                    Vectors.lerp(vs, vj, vi, tmax, seg, 3)
                    segmentVertices.add(seg)
                }
            }
            j = i++
        }
        return segmentVertices
    }

    /**
     * @p hitPos is not adjusted using the height detail data.
     *
     * @p hitDist will equal the search radius if there is no wall within the
     * radius. In this case the values of @p hitPos and @p hitNormal are
     * undefined.
     *
     * The normal will become unpredicable if @p hitDist is a very small number.
     *
     * Finds the distance from the specified position to the nearest polygon wall.
     * @param startRef The reference id of the polygon containing @p centerPos.
     * @param centerPos The center of the search circle. [(x, y, z)]
     * @param maxRadius The radius of the search circle.
     * @param filter The polygon filter to apply to the query.
     * @param hitDist The distance to the nearest wall from @p centerPos.
     * @param hitPos The nearest position on the wall that was hit. [(x, y, z)]
     * @param hitNormal The normalized ray formed from the wall point to the
     * source point. [(x, y, z)]
     * @returns The status flags for the query.
     */
    open fun findDistanceToWall(
        startRef: Long, centerPos: Vector3f,
        maxRadius: Float,
        filter: QueryFilter
    ): Result<FindDistanceToWallResult> {

        // Validate input
        if (!nav1.isValidPolyRef(startRef) || !centerPos.isFinite || maxRadius < 0 || !maxRadius.isFinite()) {
            return Result.invalidParam()
        }
        nodePool.clear()
        openList.clear()
        val startNode = nodePool.getOrCreateNode(startRef)
        startNode.pos.set(centerPos)
        startNode.parentIndex = 0
        startNode.cost = 0f
        startNode.totalCost = 0f
        startNode.polygonRef = startRef
        startNode.flags = Node.OPEN
        openList.offer(startNode)
        var radiusSqr = maxRadius * maxRadius
        val hitPos = Vector3f()
        var bestvi = -1
        var bestvj = -1
        var bestData: FloatArray? = null
        val tmp = PortalResult()
        while (!openList.isEmpty()) {
            val bestNode: Node = openList.poll()
            bestNode.flags = bestNode.flags and Node.OPEN.inv()
            bestNode.flags = bestNode.flags or Node.CLOSED

            // Get poly and tile.
            // The API input has been checked already, skip checking internal data.
            val bestRef = bestNode.polygonRef
            val bestTile = nav1.getTileByRefUnsafe(bestRef)
            val bestPoly = nav1.getPolyByRefUnsafe(bestRef, bestTile)

            // Get parent poly and tile.
            var parentRef = 0L
            if (bestNode.parentIndex != 0) {
                parentRef = nodePool.getNodeAtIdx(bestNode.parentIndex)!!.polygonRef
            }

            // Hit test walls.
            var i = 0
            var j = bestPoly.vertCount - 1
            while (i < bestPoly.vertCount) {

                // Skip non-solid edges.
                if (bestPoly.neighborData[j] and NavMesh.DT_EXT_LINK != 0) {
                    // Tile border.
                    var solid = true
                    var k = bestTile.polyLinks[bestPoly.index]
                    while (k != NavMesh.DT_NULL_LINK) {
                        val link = bestTile.links[k]
                        if (link.indexOfPolyEdge == j) {
                            if (link.neighborRef != 0L) {
                                val ref = link.neighborRef
                                val neiTile = nav1.getTileByRefUnsafe(ref)
                                val neiPoly = nav1.getPolyByRefUnsafe(ref, neiTile)
                                if (filter.passFilter(ref, neiTile, neiPoly)) {
                                    solid = false
                                }
                            }
                            break
                        }
                        k = bestTile.links[k].indexOfNextLink
                    }
                    if (!solid) {
                        j = i++
                        continue
                    }
                } else if (bestPoly.neighborData[j] != 0) {
                    // Internal edge
                    val idx = bestPoly.neighborData[j] - 1
                    val ref = nav1.getPolyRefBase(bestTile) or idx.toLong()
                    if (filter.passFilter(ref, bestTile, bestTile.data.polygons[idx])) {
                        j = i++
                        continue
                    }
                }

                // Calc distance to the edge.
                val vj = bestPoly.vertices[j] * 3
                val vi = bestPoly.vertices[i] * 3
                val (distSqr, tseg) = Vectors.distancePtSegSqr2D(centerPos, bestTile.data.vertices, vj, vi)

                // Edge is too far, skip.
                if (distSqr > radiusSqr) {
                    j = i++
                    continue
                }

                // Hit wall, update radius.
                radiusSqr = distSqr
                val data = bestTile.data
                // Calculate hit pos.
                hitPos.x = data.vertices[vj] + (data.vertices[vi] - data.vertices[vj]) * tseg
                hitPos.y = data.vertices[vj + 1] + (data.vertices[vi + 1] - data.vertices[vj + 1]) * tseg
                hitPos.z = data.vertices[vj + 2] + (data.vertices[vi + 2] - data.vertices[vj + 2]) * tseg
                bestvj = vj
                bestvi = vi
                bestData = data.vertices
                j = i++
            }

            var k = bestTile.polyLinks[bestPoly.index]
            while (k != NavMesh.DT_NULL_LINK) {
                val link = bestTile.links[k]
                val neighbourRef = link.neighborRef
                // Skip invalid neighbours and do not follow back to parent.
                if (neighbourRef == 0L || neighbourRef == parentRef) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }

                // Expand to neighbour.
                val neighbourTile = nav1.getTileByRefUnsafe(neighbourRef)
                val neighbourPoly = nav1.getPolyByRefUnsafe(neighbourRef, neighbourTile)

                // Skip off-mesh connections.
                if (neighbourPoly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }

                // Calc distance to the edge.
                val va = bestPoly.vertices[link.indexOfPolyEdge] * 3
                val vb = bestPoly.vertices[(link.indexOfPolyEdge + 1) % bestPoly.vertCount] * 3
                val distSqr = Vectors.distancePtSegSqr2DFirst(centerPos, bestTile.data.vertices, va, vb)
                // If the circle is not touching the next polygon, skip it.
                if (distSqr > radiusSqr) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }
                if (!filter.passFilter(neighbourRef, neighbourTile, neighbourPoly)) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }
                val neighbourNode = nodePool.getOrCreateNode(neighbourRef)
                if (neighbourNode.flags and Node.CLOSED != 0) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }

                // Cost
                if (neighbourNode.flags == 0) {
                    val midPoint =
                        getEdgeMidPoint(bestRef, bestPoly, bestTile, neighbourRef, neighbourPoly, neighbourTile, tmp)
                    if (midPoint != null) {
                        neighbourNode.pos.set(midPoint)
                    }
                }
                val total = bestNode.totalCost + bestNode.pos.distance(neighbourNode.pos)

                // The node is already in open list and the new result is worse, skip.
                if (neighbourNode.flags and Node.OPEN != 0 && total >= neighbourNode.totalCost) {
                    k = bestTile.links[k].indexOfNextLink
                    continue
                }
                neighbourNode.polygonRef = neighbourRef
                neighbourNode.flags = neighbourNode.flags and Node.CLOSED.inv()
                neighbourNode.parentIndex = nodePool.getNodeIdx(bestNode)
                neighbourNode.totalCost = total
                if (neighbourNode.flags and Node.OPEN != 0) {
                    openList.remove(neighbourNode)
                    openList.offer(neighbourNode)
                } else {
                    neighbourNode.flags = neighbourNode.flags or Node.OPEN
                    openList.offer(neighbourNode)
                }
                k = bestTile.links[k].indexOfNextLink
            }
        }

        // Calc hit normal.
        val hitNormal = Vector3f()
        if (bestData != null) {
            hitNormal.x = bestData[bestvi + 2] - bestData[bestvj + 2]
            hitNormal.y = 0f
            hitNormal.z = bestData[bestvj] - bestData[bestvi]
            hitNormal.normalize()
        }
        return Result.success(FindDistanceToWallResult(sqrt(radiusSqr), hitPos, hitNormal))
    }

    /**
     * Returns true if the polygon reference is valid and passes the filter restrictions.
     * @param ref The polygon reference to check.
     * @param filter The filter to apply.
     */
    fun isValidPolyRef(ref: Long, filter: QueryFilter): Boolean {
        val tile = nav1.getTileByRef(ref) ?: return false
        val poly = nav1.getPolyByRef(ref, tile) ?: return false
        // If cannot pass filter, assume flags has changed and boundary is invalid.
        return filter.passFilter(ref, tile, poly)
    }

    /**
     * Gets a path from the explored nodes in the previous search.
     *
     * @param endRef The reference id of the end polygon.
     * @returns An ordered list of polygon references representing the path. (Start to end.)
     * @remarks The result of this function depends on the state of the query object. For that reason it should only be
     * used immediately after one of the two Dijkstra searches, findPolysAroundCircle or findPolysAroundShape.
     */
    fun getPathFromDijkstraSearch(endRef: Long): Result<LongArrayList?> {
        if (!nav1.isValidPolyRef(endRef)) {
            return Result.invalidParam("Invalid end ref")
        }
        val nodes = nodePool.findNodes(endRef)
        if (nodes.size != 1) {
            return Result.invalidParam("Invalid end ref")
        }
        val endNode = nodes[0]
        return if (endNode.flags and Node.CLOSED == 0) {
            Result.invalidParam("Invalid end ref")
        } else {
            val path = LongArrayList()
            getPathToNode(endNode, path)
            Result.success(path)
        }
    }

    /**
     * Gets the path leading to the specified end node.
     */
    fun getPathToNode(endNode: Node, dst: LongArrayList) {
        // Reverse the path.
        var curNode = endNode
        while (true) {
            dst.add(curNode.polygonRef)
            val nextNode = nodePool.getNodeAtIdx(curNode.parentIndex)
            if (curNode.shortcut != null) {
                // remove potential duplicates from shortcut path
                for (i in curNode.shortcut!!.size - 1 downTo 0) {
                    val id = curNode.shortcut!![i]
                    if (id != curNode.polygonRef && id != nextNode!!.polygonRef) {
                        dst.add(id)
                    }
                }
            }
            curNode = nextNode ?: break
        }
        dst.reverse()
    }

    /**
     * The closed list is the list of polygons that were fully evaluated during the last navigation graph search. (A* or
     * Dijkstra)
     */
    fun isInClosedList(ref: Long): Boolean {
        for (n in nodePool.findNodes(ref)) {
            if (n.flags and Node.CLOSED != 0) {
                return true
            }
        }
        return false
    }

    companion object {
        /**
         * Use raycasts during pathfind to "shortcut" (raycast still consider costs) Options for
         * NavMeshQuery::initSlicedFindPath and updateSlicedFindPath
         */
        const val DT_FINDPATH_ANY_ANGLE = 0x02

        /**
         * Raycast should calculate movement cost along the ray and fill RaycastHit::cost
         */
        const val DT_RAYCAST_USE_COSTS = 0x01

        /**
         * The vertex is the start position in the path.
         */
        const val DT_STRAIGHTPATH_START = 0x01

        /**
         * The vertex is the end position in the path.
         */
        const val DT_STRAIGHTPATH_END = 0x02

        /**
         * The vertex is the start of an off-mesh connection.
         */
        const val DT_STRAIGHTPATH_OFFMESH_CONNECTION = 0x04

        /**
         *  Add a vertex at every polygon edge crossing. Options for findStraightPath.
         * */
        const val DT_STRAIGHTPATH_AREA_CROSSINGS = 0x01

        /**
         * Add a vertex at every polygon edge crossing. where area changes.
         * */
        const val DT_STRAIGHTPATH_ALL_CROSSINGS = 0x02

        val MAX_NEIS = 8

        private const val maxPolyVertices = 12
        private fun createCachedFSAs() = arrayOf(
            FloatSubArray(maxPolyVertices * 3), // navmesh polygon
            FloatSubArray(CIRCLE_SEGMENTS * 3), // circle polygon
            FloatSubArray(CIRCLE_SEGMENTS * 9) // intersection polygon
        )

        private val fsaCache = object : ThreadLocal<Array<FloatSubArray>>() {
            override fun initialValue() = createCachedFSAs()
        }

        val segmentCache = ArrayList<FloatArray>()
        fun createSegment(): FloatArray {
            return synchronized(segmentCache) {
                segmentCache.removeLastOrNull() ?: FloatArray(6)
            }
        }

    }
}