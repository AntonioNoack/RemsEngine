package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * - test rendering a transparent mesh using 2x2 dithering
 * - test different alpha values: (0,1/3,2/3,1)
 * - dithering is used in lights, so we can test this on shadows
 *
 * Test whether we have a frameIndex-dependent offset for visual-only blending ->
 * No, we don't. There is flickering as soon as the scene or your head moves.
 * (not visible in this scene: engine needs to be modified in quite a few places to implement it)
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity()
    val mesh = IcosahedronModel.createIcosphere(3)

    Entity("Floor", scene)
        .setPosition(0.0, -1.0, 0.0)
        .setScale(10f)
        .add(MeshComponent(plane))

    for (i in 0 until 4) {

        val material = Material()
        material.diffuseBase.w = i / 3f

        Entity("Sphere[$i]", scene)
            .setPosition((i - 2.0) * 2.5, 0.0, 0.0)
            .add(MeshComponent(mesh, material))
    }

    Entity("Dithered Light", scene)
        .add(DirectionalLight().apply {
            autoUpdate = 1
            shadowMapCascades = 1
            color.set(5f)
        })
        .setRotationDegrees(-30f, 0f, 0f)
        .setScale(10f)

    testSceneWithUI("Dither 2x2", scene)
}