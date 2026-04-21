package me.anno.tests.mesh.formats.fbx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    val carRef = getReference("/media/antonio/4TB WDRed/Assets/Quaternius/Cars.zip/SportsCar2.fbx/Scene.json")
    Entity(scene)
        .setScale(100f)
        .add(MeshComponent(carRef))
    testSceneWithUI("Car Mesh", scene)
}