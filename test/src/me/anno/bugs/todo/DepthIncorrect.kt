package me.anno.bugs.todo

import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testScene2
import me.anno.ui.debug.TestEngine.Companion.testUI3

// fixed depth without clip-control is incorrect
// fixed SSAO is incorrect without clip-control in forward MSAA
// todo position is incorrect in orthographic mode without clip control
//  looks like cameraPosition is incorrect, except that should be impossible
fun main() {
    testUI3("ClipControl-Depth") {
        testScene2(DefaultAssets.icoSphere) {
            it.renderView.renderMode = RenderMode.DEPTH_TEST
        }
    }
}