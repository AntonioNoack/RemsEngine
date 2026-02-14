package me.anno.tests.mesh.unity

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    val source = getReference(
        "E:/Assets/Unity/Polygon/WarMap.unitypackage/Assets/PolygonWarMap/Prefabs/Vehicles/SM_Veh_Artillery_01.prefab"
    )
    source.copyTo(desktop.getChild(source.name))
    testSceneWithUI("Unity", source)
}