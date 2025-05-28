package me.anno.tests.animation

import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    val file = downloads.getChild("3d/azeria/scene.gltf")
    testSceneWithUI("Fox",
        AnimMeshComponent().apply {
            meshFile = file
            animations = listOf(AnimationState(file.getChild("animations/Walk/Imported.json")))
        })
}