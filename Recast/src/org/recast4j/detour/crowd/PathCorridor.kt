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
import org.recast4j.Vectors.dist2D
import org.recast4j.Vectors.dist2DSqr
import org.recast4j.detour.NavMeshQuery
import org.recast4j.detour.NodePool
import org.recast4j.detour.QueryFilter
import org.recast4j.detour.StraightPathItem
import kotlin.math.min

/**
 * Represents a dynamic polygon corridor used to plan agent movement.
 *
 *
 * The corridor is loaded with a path, usually obtained from a #NavMeshQuery::findPath() query. The corridor is then
 * used to plan local movement, with the corridor automatically updating as needed to deal with inaccurate agent
 * locomotion.
 *
 *
 * Example of a common use case:
 *
 *
 * -# Construct the corridor object and call -# Obtain a path from a #dtNavMeshQuery object. -# Use #reset() to set the
 * agent's current position. (At the beginning of the path.) -# Use #setCorridor() to load the path and target. -# Use
 * #findCorners() to plan movement. (This handles dynamic path straightening.) -# Use #movePosition() to feed agent
 * movement back into the corridor. (The corridor will automatically adjust as needed.) -# If the target is moving, use
 * #moveTargetPosition() to update the end of the corridor. (The corridor will automatically adjust as needed.) -#
 * Repeat the previous 3 steps to continue to move the agent.
 *
 *
 * The corridor position and target are always constrained to the navigation mesh.
 *
 *
 * One of the difficulties in maintaining a path is that floating point errors, locomotion inaccuracies, and/or local
 * steering can result in the agent crossing the boundary of the path corridor, temporarily invalidating the path. This
 * class uses local mesh queries to detect and update the corridor as needed to handle these types of issues.
 *
 *
 * The fact that local mesh queries are used to move the position and target locations results in two beahviors that
 * need to be considered:
 *
 *
 * Every time a move function is used there is a chance that the path will become non-optimial. Basically, the further
 * the target is moved from its original location, and the further the position is moved outside the original corridor,
 * the more likely the path will become non-optimal. This issue can be addressed by periodically running the
 * #optimizePathTopology() and #optimizePathVisibility() methods.
 *
 *
 * All local mesh queries have distance limitations. (Review the #dtNavMeshQuery methods for details.) So the most
 * accurate use case is to move the position and target in small increments. If a large increment is used, then the
 * corridor may not be able to accurately find the new location. Because of this limiation, if a position is moved in a
 * large increment, then compare the desired and resulting polygon references. If the two do not match, then path
 * replanning may be needed. E.g. If you move the target, check #getLastPoly() to see if it is the expected polygon.
 */
class PathCorridor {
    /**
     * Gets the current position within the corridor. (In the first polygon.)
     */
    val pos = Vector3f()

    /**
     * Gets the current target within the corridor. (In the last polygon.)
     */
    val target = Vector3f()

    /**
     * The corridor's path.
     */
    var path = LongArrayList()

    private fun mergeCorridorStartMoved(visited: LongArrayList) {

        var i0 = -1
        var i1 = -1

        // Find furthest common polygon.
        search@ for (i in path.size - 1 downTo 0) {
            for (j in visited.size - 1 downTo 0) {
                if (path[i] == visited[j]) {
                    i0 = i
                    i1 = j
                    break@search
                }
            }
        }

        // If no intersection found just return current path.
        if (i0 != -1) {
            // Concatenate paths.
            // Adjust beginning of the buffer to include the visited.
            val lenVisited = visited.size - (i1 + 1)
            path.shiftRight(lenVisited - i0)
            // Store visited
            var j = 0
            for (i in visited.size - 1 downTo i1 + 1) {
                path[j++] = (visited[i])
            }
        }
        // else path stays path
    }

    fun mergeCorridorEndMoved(path: LongArrayList, visited: LongArrayList): LongArrayList {
        var furthestPath = -1
        var furthestVisited = -1

        // Find the furthest common polygon.
        for (i in 0 until path.size) {
            var found = false
            for (j in visited.size - 1 downTo 0) {
                if (path[i] == visited[j]) {
                    furthestPath = i
                    furthestVisited = j
                    found = true
                }
            }
            if (found) {
                break
            }
        }

        // If no intersection found just return current path.
        if (furthestPath == -1) {
            return path
        }

        // Concatenate paths.
        path.shrink(furthestPath)
        path.addAll(visited, furthestVisited, visited.size)
        return path
    }

    fun mergeCorridorStartShortcut(path: LongArrayList, visited: LongArrayList): LongArrayList {
        var furthestPath = -1
        var furthestVisited = -1

        // Find the furthest common polygon.
        for (i in path.size - 1 downTo 0) {
            var found = false
            for (j in visited.size - 1 downTo 0) {
                if (path[i] == visited[j]) {
                    furthestPath = i
                    furthestVisited = j
                    found = true
                }
            }
            if (found) {
                break
            }
        }

        // If no intersection found just return current path.
        if (furthestPath == -1 || furthestVisited <= 0) {
            return path
        }

        // Concatenate paths.

        // Adjust beginning of the buffer to include the visited.
        visited.shrink(furthestVisited)
        visited.addAll(path, furthestPath, path.size)
        return visited
    }

    /**
     * Resets the path corridor to the specified position.
     *
     * @param ref The polygon reference containing the position.
     * @param pos The new position in the corridor. [(x, y, z)]
     */
    fun reset(ref: Long, pos: Vector3f) {
        path.clear()
        path.add(ref)
        this.pos.set(pos)
        target.set(pos)
    }

    /**
     * Finds the corners in the corridor from the position toward the target. (The straightened path.)
     *
     *
     * This is the function used to plan local movement within the corridor. One or more corners can be detected in
     * order to plan movement. It performs essentially the same function as #dtNavMeshQuery::findStraightPath.
     *
     *
     * Due to internal optimizations, the maximum number of corners returned will be (@p maxCorners - 1) For example: If
     * the buffers are sized to hold 10 corners, the function will never return more than 9 corners. So if 10 corners
     * are needed, the buffers should be sized for 11 corners.
     *
     *
     * If the target is within range, it will be the last corner and have a polygon reference id of zero.
     *
     * @return Corners
     * @param navquery The query object used to build the corridor.
     */
    fun findCorners(
        maxCorners: Int, navquery: NavMeshQuery, tmp: NavMeshQuery.PortalResult,
        tmpVertices: FloatArray, tmpEdges0: FloatArray, tmpEdges1: FloatArray
    ): List<StraightPathItem> {
        val path = navquery.findStraightPath(
            pos, target, this.path, maxCorners, 0,
            tmp, tmpVertices, tmpEdges0, tmpEdges1
        )
        if (path != null) {
            // Prune points in the beginning of the path which are too close.
            var start = 0
            for (spi in path) {
                if (spi.flags and NavMeshQuery.DT_STRAIGHTPATH_OFFMESH_CONNECTION != 0
                    || dist2DSqr(spi.pos, pos) > MIN_TARGET_DIST
                ) break
                start++
            }
            var end = path.size
            // Prune points after an off-mesh connection.
            for (i in start until path.size) {
                val spi = path[i]
                if (spi.flags and NavMeshQuery.DT_STRAIGHTPATH_OFFMESH_CONNECTION != 0) {
                    end = i + 1
                    break
                }
            }
            if (end < path.size) StraightPathItem.clear(path.subList(end, path.size))
            if (start > 0) StraightPathItem.clear(path.subList(0, start))
            return path
        } else return emptyList()
    }

    /**
     * Attempts to optimize the path if the specified point is visible from the current position.
     *
     *
     * Inaccurate locomotion or dynamic obstacle avoidance can force the agent position significantly outside the
     * original corridor. Over time this can result in the formation of a non-optimal corridor. Non-optimal paths can
     * also form near the corners of tiles.
     *
     *
     * This function uses an efficient local visibility search to try to optimize the corridor between the current
     * position and @p next.
     *
     *
     * The corridor will change only if @p next is visible from the current position and moving directly toward the
     * point is better than following the existing path.
     *
     *
     * The more inaccurate the agent movement, the more beneficial this function becomes. Simply adjust the frequency of
     * the call to match the needs to the agent.
     *
     *
     * This function is unsuitable for long distance searches.
     *
     * @param next                  The point to search toward. [(x, y, z])
     * @param pathOptimizationRange The maximum range to search. [Limit: > 0]
     * @param navquery              The query object used to build the corridor.
     * @param filter                The filter to apply to the operation.
     */
    fun optimizePathVisibility(
        next: Vector3f, pathOptimizationRange: Float, navquery: NavMeshQuery,
        filter: QueryFilter?
    ) {
        // Clamp the ray to max distance.
        var dist = dist2D(pos, next)

        // If too close to the goal, do not try to optimize.
        if (dist < 0.01f) {
            return
        }

        // Overshoot a little. This helps to optimize open fields in tiled
        // meshes.
        dist = min(dist + 0.01f, pathOptimizationRange)

        // Adjust ray length.
        val goal = Vector3f(pos).lerp(next, pathOptimizationRange / dist)
        val rc = navquery.raycast(path[0], pos, goal, filter!!, 0, 0)
        if (rc.succeeded()) {
            if (rc.result!!.path.size > 1 && rc.result.t > 0.99f) {
                path = mergeCorridorStartShortcut(path, rc.result.path)
            }
        }
    }

    /**
     * Attempts to optimize the path using a local area search. (Partial replanning.)
     *
     *
     * Inaccurate locomotion or dynamic obstacle avoidance can force the agent position significantly outside the
     * original corridor. Over time this can result in the formation of a non-optimal corridor. This function will use a
     * local area path search to try to re-optimize the corridor.
     *
     *
     * The more inaccurate the agent movement, the more beneficial this function becomes. Simply adjust the frequency of
     * the call to match the needs to the agent.
     *
     * @param query The query object used to build the corridor.
     * @param filter   The filter to apply to the operation.
     */
    fun optimizePathTopology(query: NavMeshQuery, filter: QueryFilter, maxIterations: Int) {
        if (path.size < 3) return
        query.initSlicedFindPath(path[0], path[path.size - 1], pos, target, filter, 0)
        query.updateSlicedFindPath(maxIterations)
        val fpr = query.finalizeSlicedFindPathPartial(path)
        if (fpr.succeeded() && fpr.result!!.size > 0) {
            path = mergeCorridorStartShortcut(path, fpr.result)
        }
    }

    fun moveOverOffmeshConnection(
        offMeshConRef: Long,
        refs: LongArray,
        start: Vector3f,
        end: Vector3f,
        navquery: NavMeshQuery
    ): Boolean {
        // Advance the path up to and over the off-mesh connection.
        var prevRef = 0L
        var polyRef = path[0]
        var npos = 0
        while (npos < path.size && polyRef != offMeshConRef) {
            prevRef = polyRef
            polyRef = path[npos]
            npos++
        }
        if (npos == path.size) {
            // Could not find offMeshConRef
            return false
        }

        // Prune path
        path = path.subList(npos, path.size)
        refs[0] = prevRef
        refs[1] = polyRef
        val nav = navquery.nav1
        val (start1, end1) = nav.getOffMeshConnectionPolyEndPoints(refs[0], refs[1]) ?: return false
        pos.set(end1)
        start.set(start1)
        end.set(end1)
        return true
    }

    /**
     * Moves the position from the current location to the desired location, adjusting the corridor as needed to reflect
     * the change.
     *
     *
     * Behavior:
     *
     *
     * - The movement is constrained to the surface of the navigation mesh. - The corridor is automatically adjusted
     * (shorted or lengthened) in order to remain valid. - The new position will be located in the adjusted corridor's
     * first polygon.
     *
     *
     * The expected use case is that the desired position will be 'near' the current corridor. What is considered 'near'
     * depends on local polygon density, query search extents, etc.
     *
     *
     * The resulting position will differ from the desired position if the desired position is not on the navigation
     * mesh, or it can't be reached using a local search.
     *
     * @param npos     The desired new position. [(x, y, z)]
     * @param navquery The query object used to build the corridor.
     * @param filter   The filter to apply to the operation.
     */
    fun movePosition(
        npos: Vector3f, navquery: NavMeshQuery, filter: QueryFilter,
        tinyNodePool: NodePool, tmpVertices: FloatArray, neis: LongArray,
        visited: LongArrayList,
    ): Boolean {
        // Move along navmesh and update new position.
        val masResult = navquery.moveAlongSurface(path[0], pos, npos, filter, tinyNodePool, tmpVertices, neis, visited)
        return if (masResult != null) {
            mergeCorridorStartMoved(masResult.second)
            // Adjust the position to stay on top of the navmesh.
            pos.set(masResult.first)
            val hr = navquery.getPolyHeight(path[0], masResult.first)
            if (hr.isFinite()) pos.y = hr
            true
        } else false
    }

    /**
     * Moves the target from the curent location to the desired location, adjusting the corridor as needed to reflect
     * the change. Behavior: - The movement is constrained to the surface of the navigation mesh. - The corridor is
     * automatically adjusted (shorted or lengthened) in order to remain valid. - The new target will be located in the
     * adjusted corridor's last polygon.
     *
     *
     * The expected use case is that the desired target will be 'near' the current corridor. What is considered 'near'
     * depends on local polygon density, query search extents, etc. The resulting target will differ from the desired
     * target if the desired target is not on the navigation mesh, or it can't be reached using a local search.
     *
     * @param npos     The desired new target position. [(x, y, z)]
     * @param navquery The query object used to build the corridor.
     * @param filter   The filter to apply to the operation.
     */
    fun moveTargetPosition(
        npos: Vector3f,
        navquery: NavMeshQuery,
        filter: QueryFilter,
        adjustPositionToTopOfNavMesh: Boolean,
        tinyNodePool: NodePool, tmpVertices: FloatArray, neis: LongArray,
        visited: LongArrayList,
    ): Boolean {
        // Move along navmesh and update new position.
        val masResult = navquery.moveAlongSurface(
            path[path.size - 1], target, npos, filter,
            tinyNodePool, tmpVertices, neis, visited
        )
        if (masResult != null) {
            path = mergeCorridorEndMoved(path, masResult.second)
            val resultPos = masResult.first
            if (adjustPositionToTopOfNavMesh) {
                val h = target.y
                navquery.getPolyHeight(path[path.size - 1], npos)
                resultPos.y = h
            }
            target.set(resultPos)
            return true
        }
        return false
    }

    /**
     * Loads a new path and target into the corridor. The current corridor position is expected to be within the first
     * polygon in the path. The target is expected to be in the last polygon.
     *
     * @param target The target location within the last polygon of the path. [(x, y, z)]
     * @param path   The path corridor.
     * @warning The size of the path must not exceed the size of corridor's path buffer set during #init().
     */
    fun setCorridor(target: Vector3f, path: LongArrayList) {
        this.target.set(target)
        this.path = LongArrayList(path)
    }

    fun fixPathStart(safeRef: Long, safePos: Vector3f) {
        pos.set(safePos)
        if (path.size in 1..2) {
            val p = path[path.size - 1]
            path.clear()
            path.add(safeRef)
            path.add(0L)
            path.add(p)
        } else {
            path.clear()
            path.add(safeRef)
            path.add(0L)
        }
    }

    fun trimInvalidPath(
        safeRef: Long, safePos: Vector3f, navquery: NavMeshQuery, filter: QueryFilter,
        tmpVertices: FloatArray, tmpEdges0: FloatArray, tmpEdges1: FloatArray
    ) {
        // Keep valid path as far as possible.
        var n = 0
        while (n < path.size && navquery.isValidPolyRef(path[n], filter)) {
            n++
        }
        if (n == 0) {
            // The first polyref is bad, use current safe values.
            pos.set(safePos)
            path.clear()
            path.add(safeRef)
        } else if (n < path.size) {
            path.shrink(n)
            // The path is partially usable.
        }
        // Clamp target pos to last poly
        val result = navquery.closestPointOnPolyBoundary(path[path.size - 1], target, tmpVertices, tmpEdges0, tmpEdges1)
        if (result != null) {
            target.set(result)
        }
    }

    /**
     * Checks the current corridor path to see if its polygon references remain valid. The path can be invalidated if
     * there are structural changes to the underlying navigation mesh, or the state of a polygon within the path changes
     * resulting in it being filtered out. (E.g. An exclusion or inclusion flag changes.)
     *
     * @param maxLookAhead The number of polygons from the beginning of the corridor to search.
     * @param navquery     The query object used to build the corridor.
     * @param filter       The filter to apply to the operation.
     */
    fun isValid(maxLookAhead: Int, navquery: NavMeshQuery, filter: QueryFilter): Boolean {
        // Check that all polygons still pass query filter.
        val n = min(path.size, maxLookAhead)
        for (i in 0 until n) {
            if (!navquery.isValidPolyRef(path[i], filter)) {
                return false
            }
        }
        return true
    }

    /**
     * The polygon reference id of the first polygon in the corridor, the polygon containing the position.
     *
     * @return The polygon reference id of the first polygon in the corridor. (Or zero if there is no path.)
     */
    val firstPoly: Long
        get() = if (path.isEmpty()) 0 else path[0]

    /**
     * The polygon reference id of the last polygon in the corridor, the polygon containing the target.
     *
     * @return The polygon reference id of the last polygon in the corridor. (Or zero if there is no path.)
     */
    val lastPoly: Long
        get() = if (path.isEmpty()) 0 else path[path.size - 1]

    companion object {
        private val MIN_TARGET_DIST = 1e-4f
    }
}