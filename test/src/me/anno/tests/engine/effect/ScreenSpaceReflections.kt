package me.anno.tests.engine.effect

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    val scene = Entity("Scene")
    Entity("Grass", scene)
        .setPosition(0.0, -1.5, 0.0).setScale(150f, 0.5f, 150f)
        .add(MeshComponent(flatCube, Material.diffuse(0x81BB71)))
    Entity("Wet Road", scene)
        .setPosition(0.0, -0.5, 0.0).setScale(24f, 0.5f, 150f)
        .add(MeshComponent(flatCube, Material.metallic(0x333333, 0f)))
    Entity("Truck", scene)
        .setPosition(-16.0, 0.0, -50.0)
        .add(MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox")))
    testSceneWithUI("SSAO", scene)
}