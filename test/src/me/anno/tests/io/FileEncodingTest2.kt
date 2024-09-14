package me.anno.tests.io

import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

fun main() {
    // todo this is showing "Scene not found"... why???
    OfficialExtensions.initForTests()
    workspace = documents.getChild("RemsEngine/YandereSim")
    testSceneWithUI("Binary Save File", workspace.getChild("Seat.rem"))
}