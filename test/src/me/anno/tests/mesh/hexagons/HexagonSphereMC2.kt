package me.anno.tests.mesh.hexagons

import me.anno.ecs.Entity
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.chunks.spherical.HexagonSphere

// create a Minecraft world on a hex sphere :3
// use chunks and a visibility system for them

fun main() {

    val n = 200_000
    val t = 25 // good chunk size
    val s = n / t

    val sphere = HexagonSphere(n, s)
    val world = HexagonSphereMCWorld(sphere)

    OfficialExtensions.initForTests()

    val scene = Entity()
    scene.add(HSChunkLoader(sphere, world, false, diffuseHexMaterial))
    scene.add(HSChunkLoader(sphere, world, true, transparentHexMaterial))
    // scene.add(MeshComponent(IcosahedronModel.createIcosphere(5, 0.999f)))
    testSceneWithUI("HexSphere MC/2", scene) {
        addEvent(500) {
            it.renderView.orbitCenter.set(0.0, 1.0, 0.0)
            it.renderView.radius = 10f * sphere.len
            it.renderView.near = it.renderView.radius * 0.01f
            it.renderView.far = it.renderView.radius * 1e5f
        }
    }
}

