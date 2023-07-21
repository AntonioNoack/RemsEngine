package me.anno.tests.assimp

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    testSceneWithUI("BoneUI", getReference("C:/Users/Antonio/Downloads/Silly Dancing.fbx"))
}