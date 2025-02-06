package org.recast4j.detour

import org.joml.Vector3f
import org.recast4j.Vectors
import kotlin.math.abs

class FindNearestPolyQuery(private val query: NavMeshQuery, private val center: Vector3f) : PolyQuery {
    private var nearestRef = 0L
    private var nearestPt: Vector3f
    private var overPoly = false
    private var nearestDistanceSqr: Float

    init {
        nearestDistanceSqr = Float.MAX_VALUE
        nearestPt = Vector3f(center)
    }

    override fun process(tile: MeshTile, poly: Poly, ref: Long) {
        // Find the nearest polygon amongst the nearby polygons.
        val closest = query.closestPointOnPoly(ref, center)!!
        val posOverPoly = closest.isPosOverPoly
        val closestPtPoly = closest.pos

        // If a point is directly over a polygon and closer than
        // climb height, favor that instead of straight line nearest point.
        var d: Float
        if (posOverPoly) {
            d = abs(center.y - closestPtPoly.y) - tile.data.walkableClimb
            d = if (d > 0) d * d else 0f
        } else {
            d = center.distanceSquared(closestPtPoly)
        }
        if (d < nearestDistanceSqr) {
            nearestPt = closestPtPoly
            nearestDistanceSqr = d
            nearestRef = ref
            overPoly = posOverPoly
        }
    }

    fun result(): FindNearestPolyResult {
        return FindNearestPolyResult(nearestRef, nearestPt, overPoly)
    }
}