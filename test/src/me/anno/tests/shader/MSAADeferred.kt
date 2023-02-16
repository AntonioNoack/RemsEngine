package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    val scene = Entity()
    scene.add(SkyBox())
    scene.add(MeshComponent(downloads.getChild("3d/DamagedHelmet.glb")))
    testSceneWithUI(scene) {
        it.renderer.renderMode = RenderMode.MSAA_DEFERRED
    }
}