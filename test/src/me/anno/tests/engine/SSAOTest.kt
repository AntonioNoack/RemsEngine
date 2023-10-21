package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS.downloads

fun main() {
    val scene = Entity("Scene")
    val floor = Entity(scene)
    floor.setPosition(0.0, -64.0, 0.0)
    floor.setScale(64.0)
    floor.add(MeshComponent(flatCube.front).apply {
        materials = listOf(Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0f)
        }.ref)
    })
    val truck = Entity(scene)
    truck.add(MeshComponent(downloads.getChild("MagicaVoxel/vox/truck.vox")))
    testSceneWithUI("SSAO", scene)
}