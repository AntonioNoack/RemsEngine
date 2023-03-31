package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.physics.FlagMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

// generate cloth mesh
// simulate cloth using gfx shader
// render cloth
// apply forces like wind and gravity
fun main() {
    val scene = Entity()
    testSceneWithUI(scene) {
        val comp = FlagMesh()
        comp.material.diffuseMap = OS.pictures.getChild("Anime/70697252_p4_master1200.png")
        scene.add(comp)
    }
}