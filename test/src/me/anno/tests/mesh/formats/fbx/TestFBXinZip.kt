package me.anno.tests.mesh.formats.fbx

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    val carRef = getReference("/media/antonio/4TB WDRed/Assets/Quaternius/Cars.zip/SportsCar2.fbx/Scene.json")
    testSceneWithUI("Car Mesh", carRef)
}