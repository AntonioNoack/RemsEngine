package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

/**
 * test if forward rendering is using the sky inside its light calculation
 * */
fun main() {
    // todo where is non-deferred lighting handled? too bright, probably using wrong color space
    val scene = Entity()
    scene.add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(metalRoughness())
    testSceneWithUI("Forward Sky", scene) {
        it.renderer.renderMode = RenderMode.NON_DEFERRED
    }
}
