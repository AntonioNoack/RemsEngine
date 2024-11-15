package me.anno.games.flatworld.vehicles

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.game.flatworld.FlatWorld
import me.anno.tests.game.flatworld.streets.IntersectionMeshBuilder
import me.anno.tests.game.flatworld.streets.ReversibleSegment
import me.anno.tests.game.flatworld.streets.StreetSegment
import me.anno.tests.game.flatworld.vehicles.Routes.findRoute
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.Recursion
import me.anno.utils.structures.lists.Lists.weightedRandomOrNull
import me.anno.utils.types.Strings.isNotBlank2
import kotlin.random.Random

object RandomVehicle {

    fun spawnRandomVehicle(world: FlatWorld) {
        val start = getRandomStart(world) ?: return
        val reachable = findReachablePoints(world, start)
        val end = getRandomEnd(reachable, start) ?: return
        val route0 = findRoute(world, start, end) ?: return
        val route1 = addLanesToRoute(world, route0)
        val entity = createRandomVehicle(world, route1)
        world.vehicles.add(entity)
    }

    val carFiles = listOf(
        "NormalCar1.fbx" to "Blue",
        "NormalCar2.fbx" to "Light Blue",
        "SportsCar.fbx" to "Orange",
        "SportsCar2.fbx" to "White",
        "SUV.fbx" to "White",
        "Taxi.fbx" to "",
        "Cop.fbx" to "",
    )

    /**
     * adds curves to intersections, so they are smooth; also adds a lane-offset,
     * so cars of opposite directions don't overlap
     * */
    fun addLanesToRoute(world: FlatWorld, route: List<ReversibleSegment>): List<StreetSegment> {
        assertTrue(route.isNotEmpty())
        val dx = 1.5
        val result = ArrayList<StreetSegment>(route.size * 2 - 1)
        val r0 = route[0]
        val r0t1 = IntersectionMeshBuilder.getT1(
            r0.length, world.intersections[r0.c]!!,
            world.intersections[r0.a]!!
        )
        result.add(r0.splitSegmentDx(0.0, r0t1, dx))
        for (i in 1 until route.size) {
            val rj = route[i]
            val rjt0 = IntersectionMeshBuilder.getT0(
                rj.length, world.intersections[rj.a]!!,
                world.intersections[rj.c]!!
            )
            val rjt1 = if (i < route.lastIndex) {
                IntersectionMeshBuilder.getT1(
                    rj.length, world.intersections[rj.c]!!,
                    world.intersections[rj.a]!!
                )
            } else 1.0
            val split = rj.splitSegmentDx(rjt0, rjt1, dx)
            val rja = rj.interpolateDx(0.0, dx)
            result.add(StreetSegment(result.last().c, rja, split.a))
            result.add(split)
        }
        return result
    }

    val carsMeshes = getReference("G:/Assets/Quaternius/Cars.zip")
    fun createRandomVehicle(world: FlatWorld, route: List<StreetSegment>): Entity {
        val entity = Entity()
        val (meshName, matName) = carFiles.random()
        val mesh = MeshCache[carsMeshes.getChild(meshName)]
        val meshComponent = MeshComponent(mesh ?: flatCube.front)
        if (mesh != null && matName.isNotBlank2()) {
            meshComponent.materials = mesh.materials.map { ref ->
                if (ref.nameWithoutExtension == matName)
                    Material.diffuse(Maths.randomInt()).ref
                else ref
            }
        }
        entity.add(meshComponent)
        val t = Maths.random()
        val vehicle = Vehicle(t, route)
        vehicle.maxSpeed = 5.0 + 5.0 * Maths.random()
        vehicle.world = world
        vehicle.length = mesh?.getBounds()?.deltaZ?.toDouble() ?: 4.0
        route[0].interpolate(t, vehicle.prevPosition)
        world.insertVehicle(vehicle, route[0], t)
        entity.add(vehicle)
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