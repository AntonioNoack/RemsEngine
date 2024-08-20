package me.anno.tests.game.flatworld.vehicles

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.maths.Maths
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.game.flatworld.FlatWorld
import me.anno.tests.game.flatworld.streets.ReversibleSegment
import me.anno.tests.game.flatworld.vehicles.Routes.findRoute
import me.anno.utils.structures.Recursion
import me.anno.utils.structures.lists.Lists.weightedRandomOrNull
import kotlin.random.Random

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

    fun getRandomEnd(reachable: HashSet<ReversibleSegment>, start: ReversibleSegment): ReversibleSegment? {
        reachable.remove(start)
        return reachable.randomOrNull()
    }

    fun getRandomStart(world: FlatWorld): ReversibleSegment? {
        return world.intersections.values
            .flatMap { it.segments }
            .weightedRandomOrNull(Random(Time.nanoTime), ReversibleSegment::length)
    }

    fun findReachablePoints(world: FlatWorld, start: ReversibleSegment): HashSet<ReversibleSegment> {
        return Recursion.collectRecursive(start) { pt, remaining ->
            val crossing = world.intersections[pt.c]
            if (crossing != null) {
                remaining.addAll(crossing.segments)
            }
        }
    }
}