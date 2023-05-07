package me.anno.mesh.blender

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    testSceneWithUI(getReference("C:/XAMPP/htdocs/uvbaker/sample2.blend"))
}