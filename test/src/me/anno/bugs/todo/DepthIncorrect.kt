package me.anno.bugs.todo

import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

// fixed depth without clip-control is incorrect
// fixed SSAO is incorrect without clip-control in forward MSAA
// todo position is incorrect in orthographic mode
//  looks like cameraPosition is incorrect, except that should be impossible
fun main() {
    testSceneWithUI("ClipControl-Depth", DefaultAssets.icoSphere) {
        it.renderView.renderMode = RenderMode.DEPTH_TEST
        it.renderView.editorCamera.isPerspective = false
    }
}