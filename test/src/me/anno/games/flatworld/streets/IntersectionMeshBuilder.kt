package me.anno.games.flatworld.streets

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.games.flatworld.FlatWorld
import kotlin.math.max
import kotlin.math.min

object IntersectionMeshBuilder {

    fun getT0(segmentLength: Double, closeIntersection: Intersection, farIntersection: Intersection): Double {
        val value = 4.0 * max(closeIntersection.segments.size - 1, 0) / segmentLength
        // large intersections can consume entire streets, prevent that:
        val limit = if (farIntersection.segments.size < 2) 0.999 else 0.499
        return min(value, limit)
    }

    fun getT1(segmentLength: Double, closeIntersection: Intersection, farIntersection: Intersection): Double {
        return 1.0 - getT0(segmentLength, closeIntersection, farIntersection)
    }

    fun createIntersection(intersection: Intersection, world: FlatWorld, dstMesh: Mesh) {
        // create a mesh
        val position = intersection.segments.first().a
        val entity = Entity()
        entity.transform.localPosition = position
        entity.transform.validate()
        val crossing = SplineCrossing()
        entity.add(crossing)
        for (segment in intersection.segments) {
            val pt = SplineControlPoint()
            pt.profile = StreetMeshBuilder.streetProfile
            val other = world.intersections[segment.c]!!
            val t0 = getT0(segment.length, intersection, other)
            val posI = segment.interpolate(t0)
            val posII = segment.interpolate(t0 + 0.001)
            val ry = StreetSegment.getAngle(posI, posII)
            val child = Entity(entity)
                .setPosition(posI.x - position.x, posI.y - position.y, posI.z - position.z)
                .setRotation(0.0, ry, 0.0)
                .add(pt)
            child.transform.validate()
        }
        crossing.generateMesh(dstMesh)
        dstMesh.invalidateGeometry()
    }
}