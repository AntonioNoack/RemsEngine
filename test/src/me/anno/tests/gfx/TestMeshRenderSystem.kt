package me.anno.tests.gfx

import me.anno.ecs.systems.MeshRenderSystem
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * to test this, disable "root.fill(this, sampleEntity.transform)" in Pipeline.kt/fill
 * */
fun main() {
    val mesh = DefaultAssets.flatCube
    val scene = createMeshBox(mesh, 20)
    Systems.world = scene
    Systems.registerSystem(MeshRenderSystem)
    testSceneWithUI("MeshRenderSystem", scene)
}