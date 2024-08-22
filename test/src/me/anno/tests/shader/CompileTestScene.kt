package me.anno.tests.shader

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

// test all important shaders, whether they compile fine
// add a scene with most important functions: static meshes, animated meshes, all light types
fun main() {
    testSceneWithUI("CompilerTest", CompileTest().createTestScene())
}
