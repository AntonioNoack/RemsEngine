package me.anno.bugs.done

import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

// fixed scene loading is broken: only partially loaded -> not reproducible with this setup, needs RemsEngine().run()
// fixed scene editing is broken: when changing booleans, the whole scene disappears
fun main() {
    workspace = documents.getChild("RemsEngine/New Project")
    val scene = workspace.getChild("dragon.json")
    testSceneWithUI("Scene Loading Broken", scene)
}