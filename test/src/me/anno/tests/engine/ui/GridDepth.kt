package me.anno.tests.engine.ui

import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

fun main() {
    // todo nothing is visible here :/
    // grid depth is incorrect by some factor, fixed by mono-world-scale
    // -> fixed by multiplying with it :D
    OfficialExtensions.initForTests()
    workspace = documents.getChild("RemsEngine/YandereSim")
    testSceneWithUI("GridDepth", workspace.getChild("Weapons.json"))
}