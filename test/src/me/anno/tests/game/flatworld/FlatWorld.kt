package me.anno.tests.game.flatworld

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.tests.game.flatworld.buildings.Building
import me.anno.tests.game.flatworld.streets.Intersection
import me.anno.tests.game.flatworld.streets.IntersectionMeshBuilder
import me.anno.tests.game.flatworld.streets.ReversibleSegment
import me.anno.tests.game.flatworld.streets.StreetMeshBuilder
import me.anno.tests.game.flatworld.streets.StreetSegment
import org.joml.Vector3d

class FlatWorld {

    val scene = Entity("Scene")
    val buildings = Entity("Buildings", scene)
    val terrain = Entity("Terrain", scene)
    val streets = Entity("Streets", scene)
    val vehicles = Entity("Vehicles", scene)

    val streetSegments = HashSet<StreetSegment>()
    val buildingInstances = HashSet<Building>()
    val intersections = HashMap<Vector3d, Intersection>()

    val dirtyStreets = HashSet<StreetSegment>()
    val dirtyIntersections = HashSet<Intersection>()

    fun addStreetSegment(segment: StreetSegment) {
        streetSegments.add(segment)
        addAnchor(ReversibleSegment(segment, false))
        addAnchor(ReversibleSegment(segment, true))
    }

    fun removeStreetSegment(segment: StreetSegment) {
        segment.mesh?.destroy() // free mesh
        segment.entity?.removeFromParent()
        streetSegments.remove(segment)
        removeAnchor(ReversibleSegment(segment, false))
        removeAnchor(ReversibleSegment(segment, true))
    }

    private fun addAnchor(segment: ReversibleSegment) {
        val key = segment.a
        val intersection = intersections.getOrPut(key, ::Intersection)
        intersection.segments.add(segment)
        invalidateIntersection(intersection)
    }

    private fun removeAnchor(segment: ReversibleSegment) {
        val key = segment.a
        val intersection = intersections[key] ?: return
        intersection.segments.remove(segment)
        invalidateIntersection(intersection)
        // cleanup
        if (intersection.segments.isEmpty()) {
            intersections.remove(key)
        }
    }

    private fun invalidateIntersection(intersection: Intersection) {
        dirtyIntersections.add(intersection)
        for (segmentI in intersection.segments) {
            dirtyStreets.add(segmentI.segment)
        }
    }

    fun validateMeshes() {
        validateIntersections()
        validateStreetSegments()
    }

    private fun validateIntersections() {
        for (intersection in dirtyIntersections) {
            buildIntersection(intersection)
        }
        dirtyIntersections.clear()
    }

    private fun validateStreetSegments() {
        for (segment in dirtyStreets) {
            buildStreet(segment)
        }
        dirtyStreets.clear()
    }

    fun buildStreet(segment: StreetSegment) {
        val i0 = intersections[segment.a] ?: return
        val i1 = intersections[segment.c] ?: return
        val t0 = IntersectionMeshBuilder.getT0(segment.length, i0, i1)
        val t1 = IntersectionMeshBuilder.getT1(segment.length, i1, i0)
        // the mesh to generate depends on t0 and t1,
        //  which are calculated by the intersection sizes
        val hadMesh = segment.mesh != null
        val mesh = StreetMeshBuilder.buildMesh(segment.splitSegment(t0, t1), segment.mesh ?: Mesh())
        if (hadMesh) {
            mesh.invalidateGeometry()
        } else {
            val entity = Entity(MeshComponent(mesh))
            segment.entity = entity
            segment.mesh = mesh
            streets.add(entity)
        }
    }

    fun buildIntersection(intersection: Intersection) {
        // update intersection:
        if (intersection.segments.isEmpty()) {
            // remove mesh from map
            intersection.mesh?.destroy()
            intersection.mesh = null
            intersection.entity?.removeFromParent()
        } else {
            // build end piece or actual intersection
            if (intersection.mesh == null) {
                val mesh = Mesh()
                val newComp = MeshComponent(mesh)
                val entity = Entity(streets)
                    .add(newComp)
                    .add(intersection)
                entity.position = intersection.segments.first().a
                entity.transform.teleportUpdate()
                intersection.mesh = mesh
            }
            val mesh = intersection.mesh!!
            IntersectionMeshBuilder.createIntersection(intersection, this, mesh)
            intersection.invalidateAABB() // since we changed the mesh
        }
    }
}