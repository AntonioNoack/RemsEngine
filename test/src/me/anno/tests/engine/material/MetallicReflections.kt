package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.tests.gfx.metalRoughness

fun main() {
    val scene = Entity()
    scene.add(metalRoughness())
    scene.add(Skybox())
    testSceneWithUI("Metallic Reflections", scene)
}