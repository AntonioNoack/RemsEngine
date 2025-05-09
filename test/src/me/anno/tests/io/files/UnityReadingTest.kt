package me.anno.tests.io.files

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    val project = getReference("E:/Assets/Unity/Polygon_Construction_Unity_Package_2017_4.unitypackage")
    val file = project.getChild("Assets/PolygonConstruction/Prefabs/Vehicles/SM_Veh_Mini_Loader_01.prefab")
    testSceneWithUI("Unity", file)
}