package me.anno.tests.game.flatworld

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.tests.game.flatworld.buildings.BuildingInstance
import me.anno.tests.game.flatworld.streets.StreetSegment
import me.anno.tests.game.flatworld.streets.StreetSegmentMeshes

class FlatWorld {

    val scene = Entity("Scene")
    val buildings = Entity("Buildings", scene)
    val terrain = Entity("Terrain", scene)
    val streets = Entity("Streets", scene)

    // todo acceleration structures for these once necessary
    val streetSegments = HashSet<StreetSegment>()
    val buildingInstances = HashSet<BuildingInstance>()

    fun addStreetSegment(segment: StreetSegment) {
        streetSegments.add(segment)
        val mesh = StreetSegmentMeshes.buildMesh(segment, Mesh())
        val comp = MeshComponent(mesh)
        segment.component = comp
        streets.add(Entity(comp))
        streets.invalidateAABBsCompletely() // todo why is this necessary???
    }

    fun removeStreetSegment(segment: StreetSegment) {
        val comp = segment.component!!
        val entity = comp.entity!!
        entity.removeFromParent()
        entity.destroy()
        comp.getMesh()!!.destroy() // free mesh
        streetSegments.remove(segment)
    }
}