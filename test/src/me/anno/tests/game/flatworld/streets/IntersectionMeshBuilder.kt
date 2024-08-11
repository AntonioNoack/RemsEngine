package me.anno.tests.game.flatworld.streets

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import kotlin.math.max
import kotlin.math.min

object IntersectionMeshBuilder {

    fun getT0(segment: ReversibleSegment, intersection: Intersection): Double {
        return getT0(segment.segment, intersection)
    }

    fun getT0(segment: StreetSegment, intersection: Intersection): Double {
        val value = 4.0 * max(intersection.segments.size - 1, 0) / segment.length
        // large intersections can consume entire streets, prevent that:
        return min(value, 0.499)
    }

    fun getT1(segment: StreetSegment, intersection: Intersection): Double {
        return 1.0 - getT0(segment, intersection)
    }

    fun createIntersection(intersection: Intersection, dst: Mesh) {
        // create a mesh
        val position = intersection.segments.first().a
        val entity = Entity()
        entity.transform.localPosition = position
        entity.transform.teleportUpdate()
        val crossing = SplineCrossing()
        entity.add(crossing)
        for (segment in intersection.segments) {
            val pt = SplineControlPoint()
            pt.profile = StreetMeshBuilder.streetProfile
            val t0 = getT0(segment, intersection)
            val posI = segment.interpolate(t0)
            val posII = segment.interpolate(t0 + 0.001)
            val ry = StreetSegment.getAngle(posI, posII)
            val child = Entity(entity)
                .setPosition(posI.x - position.x, posI.y - position.y, posI.z - position.z)
                .setRotation(0.0, ry, 0.0)
                .add(pt)
            child.transform.teleportUpdate()
        }
        crossing.generateMesh(dst)
        dst.invalidateGeometry()
    }
}