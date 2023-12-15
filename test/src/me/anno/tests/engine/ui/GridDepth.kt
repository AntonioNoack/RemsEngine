package me.anno.tests.engine.ui

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.utils.OS.documents

fun main() {
    // grid depth is incorrect by some factor, fixed by mono-world-scale
    // -> fixed by multiplying with it :D
    workspace = documents.getChild("RemsEngine/YandereSim")
    ECSRegistry.initPrefabs()
    ECSRegistry.initMeshes()
    testSceneWithUI("GridDepth", workspace.getChild("Weapons.json"))
}