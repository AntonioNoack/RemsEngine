package me.anno.tests.assimp

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    // todo animations are only visible with their motion vectors and their outline why?
    testSceneWithUI(getReference("C:/Users/Antonio/Downloads/Silly Dancing.fbx"))
}