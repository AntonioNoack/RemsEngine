package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

// test if forward rendering is using the sky inside its light calculation
fun main() {
    val scene = Entity()
    scene.add(MeshComponent(OS.downloads.getChild("3d/DamagedHelmet.glb")))
    scene.add(MeshComponent(OS.documents.getChild("metal-roughness.glb")))
    scene.add(Skybox())
    testSceneWithUI("Forward Sky", scene) {
        it.renderer.renderMode = RenderMode.FORCE_NON_DEFERRED
    }
}
