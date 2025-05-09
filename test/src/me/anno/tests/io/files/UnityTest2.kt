package me.anno.tests.io.files

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    val file = getReference(
        "E:/Assets/Unity/POLYGON_Knights_Unity_Package_2017_1.unitypackage/" +
                "Assets/PolygonKnights/Prefabs/Buildings/SM_Bld_Castle_Tower_03.prefab"
    )
    testSceneWithUI(file.name, file)
}