package me.anno.tests.game.flatworld.vehicles

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.maths.Maths
import me.anno.maths.paths.PathFinding.aStar
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.game.flatworld.FlatWorld
import me.anno.tests.game.flatworld.streets.ReversibleSegment

object RandomVehicle {

    fun spawnRandomVehicle(world: FlatWorld) {
        val start = getRandomStart(world) ?: return
        val reachable = findReachablePoints(world, start)
        val end = getRandomEnd(reachable, start) ?: return
        val route = findRoute(world, start, end) ?: return
        val entity = createRandomVehicle(route)
        world.vehicles.add(entity)
    }

    fun createRandomVehicle(route: List<ReversibleSegment>): Entity {
        val entity = Entity()
        // todo better car model
        // todo random car colors
        entity.add(MeshComponent(flatCube.front))
        entity.add(Vehicle(Maths.random(), route))
        return entity
    }

    fun distance(start: ReversibleSegment, end: ReversibleSegment): Double {
        return start.a.distance(end.a)
    }

    fun findRoute(world: FlatWorld, start: ReversibleSegment, end: ReversibleSegment): List<ReversibleSegment>? {
        return aStar(
            start, end, distance(start, end), 1e9, 16,
            includeStart = true, includeEnd = true
        ) { src, cb ->
            val intersection = world.intersections[src.c]
            if (intersection != null) {
                for (seg in intersection.segments) {
                    cb(seg, distance(src, seg), distance(seg, end))
                }
            }
        }
    }

    fun <V> getRandomEnd(reachable: HashSet<V>, start: V): V? {
        reachable.remove(start)
        return reachable.randomOrNull()
    }

    fun getRandomStart(world: FlatWorld): ReversibleSegment? {
        return world.intersections.values
            .flatMap { it.segments }
            .randomOrNull()
    }

    fun findReachablePoints(world: FlatWorld, start: ReversibleSegment): HashSet<ReversibleSegment> {
        val reachable = HashSet<ReversibleSegment>()
        val remaining = ArrayList<ReversibleSegment>()
        reachable.add(start)
        remaining.add(start)
        while (remaining.isNotEmpty()) {
            val pt = remaining.removeLast()
            val crossing = world.intersections[pt.c] ?: continue
            for (seg in crossing.segments) {
                if (reachable.add(seg)) {
                    remaining.add(seg)
                }
            }
        }
        return reachable
    }
}