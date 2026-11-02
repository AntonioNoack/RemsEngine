package me.anno.bugs.done

import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX

// fixed depth without clip-control is incorrect
// fixed SSAO is incorrect without clip-control in forward MSAA
// done position is incorrect in orthographic mode
//  looks like cameraPosition is incorrect, except that should be impossible
fun main() {
    GFX.debugLimitedGPUs = false
    testSceneWithUI(
        "ClipControl-Depth", DefaultAssets.icoSphere.apply { scaleUp10x() },
        RenderMode.DEPTH_TEST
    )
}