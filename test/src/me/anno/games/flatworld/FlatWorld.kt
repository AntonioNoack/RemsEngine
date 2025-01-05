package me.anno.games.flatworld

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.NamedSaveable
import me.anno.games.flatworld.buildings.Building
import me.anno.games.flatworld.streets.Intersection
import me.anno.games.flatworld.streets.IntersectionMeshBuilder
import me.anno.games.flatworld.streets.ReversibleSegment
import me.anno.games.flatworld.streets.StreetMeshBuilder
import me.anno.games.flatworld.streets.StreetSegment
import me.anno.games.flatworld.streets.StreetSegmentData
import me.anno.games.flatworld.vehicles.Vehicle
import org.joml.Vector3d

class FlatWorld : NamedSaveable() {

    val scene = Entity("Scene")
    val buildings = Entity("Buildings", scene)
    val terrain = Entity("Terrain", scene)
    val streets = Entity("Streets", scene)
    val vehicles = Entity("Vehicles", scene)

    // todo grids of multiple levels:
    //  - simple, placing mode
    //  - for meshing: adds off & on-ramps, maybe round-abouts | for pathfinding, and traffic control
    //  - for pathing: no abrupt rotation

    val streetSegments = HashSet<StreetSegment>()
    val intersections = HashMap<Vector3d, Intersection>()

    val buildingInstances = HashSet<Building>()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "streetSegments", streetSegments.map(StreetSegment::save))
        // writer.writeObjectList(this, "buildingInstances", buildingInstances.toList())
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "streetSegments" -> {
                if (value !is List<*>) return
                for (v in value) {
                    v as? StreetSegmentData ?: continue
                    addStreetSegment(StreetSegment(v.a, v.b, v.c))
                }
            }
            /*"buildingInstances" -> {
                if (value !is List<*>) return
                for (v in value) {
                    v as? Building ?: continue

                }
            }*/
            else -> super.setProperty(name, value)
        }
    }

    private val dirtyStreets = HashSet<StreetSegment>()
    private val dirtyIntersections = HashSet<Intersection>()

    fun addStreetSegment(segment: StreetSegment) {
        streetSegments.add(segment)
        addAnchor(ReversibleSegment(segment, false))
        addAnchor(ReversibleSegment(segment, true))
    }

    fun removeStreetSegment(segment: StreetSegment) {
        segment.streetMesh?.destroy() // free mesh
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
        val hadMesh = segment.streetMesh != null
        val subSegment = segment.splitSegment(t0, t1)
        val streetMesh = segment.streetMesh ?: Mesh()
        StreetMeshBuilder.buildMesh(subSegment, streetMesh)
        if (!hadMesh) {
            segment.entity = Entity(streets)
                .add(MeshComponent(streetMesh))
            segment.streetMesh = streetMesh
        }
    }

    fun buildIntersection(intersection: Intersection) {
        // update intersection:
        if (intersection.segments.isEmpty()) {
            // remove mesh from map
            intersection.streetMesh?.destroy()
            intersection.streetMesh = null
            intersection.entity?.removeFromParent()
        } else {
            // build end piece or actual intersection
            if (intersection.streetMesh == null) {
                val streetMesh = Mesh()
                val entity = Entity(streets)
                    .add(MeshComponent(streetMesh))
                    .add(intersection)
                entity.position = intersection.segments.first().a
                entity.transform.teleportUpdate()
                intersection.streetMesh = streetMesh
            }
            val mesh = intersection.streetMesh!!
            IntersectionMeshBuilder.createIntersection(intersection, this, mesh)
            intersection.invalidateAABB() // since we changed the mesh
        }
    }

    private val vehicleLookup = HashMap<StreetSegment, HashSet<Vehicle>>()
    fun insertVehicle(vehicle: Vehicle, segment: StreetSegment, t: Double) {
        val onSegment = vehicleLookup.getOrPut(segment, ::HashSet)
        val previousVehicle = onSegment
            .filter { it.currentT > t }
            .minByOrNull { it.currentT }
        val nextVehicle = onSegment
            .filter { it.currentT < t }
            .maxByOrNull { it.currentT }
        onSegment.add(vehicle)
        vehicle.previousVehicle = previousVehicle
        nextVehicle?.previousVehicle = vehicle
    }

    fun canInsertVehicle(vehicle: Vehicle, segment: StreetSegment): Boolean {
        val vehicles = vehicleLookup[segment] ?: return true
        val minT = vehicle.lengthPlusExtra / segment.length
        return vehicles.none { it.currentT < minT }
    }

    fun removeVehicle(vehicle: Vehicle, segment: StreetSegment) {
        val onSegment = vehicleLookup[segment]
        if (onSegment != null) {
            onSegment.remove(vehicle)
            for (following in onSegment) {
                if (following.previousVehicle == vehicle) {
                    following.previousVehicle = vehicle.previousVehicle
                }
            }
        }
        vehicle.previousVehicle = null
    }
}