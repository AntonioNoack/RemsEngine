package me.anno.tests.bugs.done

import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

/**
 * isEnabled-checkbox was missing
 * */
fun main() {
    OfficialExtensions.register()
    workspace = documents.getChild("RemsEngine/AssetIndex")
    testSceneWithUI("Prefabs Broken", workspace.getChild("ringX.json"))
}
