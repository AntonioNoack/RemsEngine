package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

/**
 * test if forward rendering is using the sky inside its light calculation
 * */
fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    scene.add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(metalRoughness())
    testSceneWithUI("Forward Sky", scene) {
        it.renderer.renderMode = RenderMode.FORWARD
    }
}
