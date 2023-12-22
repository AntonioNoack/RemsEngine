package me.anno.tests.shader

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.utils.OS.documents

fun main() {
    // todo world with MSAA is completely dark, except for glass, why?? but only if glass is used... is it setting tint???
    //  light sum is dark, too
    workspace = documents.getChild("RemsEngine/YandereSim")
    val file = workspace.getChild("ClassRoom.json")
    testSceneWithUI("Light Broken", file)
}