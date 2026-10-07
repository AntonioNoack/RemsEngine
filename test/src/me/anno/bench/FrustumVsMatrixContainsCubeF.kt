package me.anno.bench

import me.anno.engine.ui.render.Frustum
import me.anno.utils.Clock
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    // test frustum float vs double speed:
    //   float is 1.5x faster

    val clock = Clock("Frustum Float VS Double")

    val s = 1.0
    val cubes = (-10..10).flatMap { z ->
        (-10..10).flatMap { y ->
            (-10..10).map { x ->
                AABBd(
                    x - s, y - s, z - s,
                    x + s, y + s, z + s
                )
            }
        }
    }.shuffled()
    val cubesF = cubes.map { AABBf(it) }

    val frustum = Frustum()
    frustum.definePerspective(1e-3f, 1e6f, 1f, 1000, 1f, Vector3d(), Quaternionf())

    // 0.28ns/e
    clock.benchmark(100_000, 1000_000, cubes.size, "Floats") {
        for (ci in cubes.indices) {
            frustum.contains(cubesF[ci])
        }
    }

    // 0.43ns/e
    clock.benchmark(100_000, 1000_000, cubes.size, "Doubles") {
        for (ci in cubes.indices) {
            frustum.contains(cubes[ci])
        }
    }

}
