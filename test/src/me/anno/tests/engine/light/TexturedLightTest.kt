package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.CircleLight
import me.anno.ecs.components.light.RectangleLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.utils.OS.res

fun main() {
    val scene = Entity("Scene")
    Entity("Floor", scene)
        .add(MeshComponent(DefaultAssets.plane))
        .setScale(1f)

    val rx = -PIf * 0.5f
    val source = res.getChild("textures/Ghost.png")

    Entity("Non-Textured RectangleLight", scene)
        .add(RectangleLight().apply {
            color.set(0.5f)
        })
        .setPosition(-0.3, 0.1, -0.6)
        .setRotation(rx, 0f, 0f)

    Entity("Textured RectangleLight", scene)
        .add(RectangleLight().apply {
            texture = source
            textureSize = 0.2f
            color.set(0.5f)
        })
        .setPosition(+0.3, 0.1, -0.6)
        .setRotation(rx, 0f, 0f)

    Entity("Non-Textured SpotLight", scene)
        .add(SpotLight().apply {
            color.set(2f)
        })
        .setPosition(-0.3, 0.25, 0.0)
        .setRotation(rx, 0f, 0f)

    Entity("Textured SpotLight", scene)
        .add(SpotLight().apply {
            texture = source
            textureSize = 0.2f
            color.set(2f)
        })
        .setPosition(+0.3, 0.25, 0.0)
        .setRotation(rx, 0f, 0f)

    Entity("Non-Textured CircleLight", scene)
        .add(CircleLight().apply {
            color.set(0.5f)
        })
        .setPosition(-0.3, 0.1, +0.6)
        .setRotation(rx, 0f, 0f)

    Entity("Textured CircleLight", scene)
        .add(CircleLight().apply {
            texture = source
            textureSize = 0.2f
            color.set(0.5f)
        })
        .setPosition(+0.3, 0.1, +0.6)
        .setRotation(rx, 0f, 0f)

    testSceneWithUI("TexturedLight", scene)
}