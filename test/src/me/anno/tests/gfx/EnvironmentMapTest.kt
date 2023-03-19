package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

fun main() {
    // test environment map
    ECSRegistry.init()
    val scene = Entity()
    scene.add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(MeshComponent(OS.documents.getChild("metal-roughness.glb")))
    scene.add(SkyBox())
    scene.add(Entity("Environment Map").apply {
        add(EnvironmentMap())
    })
    testSceneWithUI(scene) {
        it.renderer.renderMode = RenderMode.FORCE_NON_DEFERRED
    }
}
