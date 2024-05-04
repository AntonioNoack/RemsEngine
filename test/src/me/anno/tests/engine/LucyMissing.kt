package me.anno.tests.engine

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("Lucy", downloads.getChild("3d/lucy0.fbx"))
}