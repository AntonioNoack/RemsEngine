package me.anno.bugs.todo

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // todo bug: BoneByBone is 100µm in size, Imported is 1cm in size
    //   they should be the same size
    OfficialExtensions.initForTests()
    testSceneWithUI("AnimTest", downloads.getChild("3d/Driving.fbx/animations/mixamo.com/Imported.json"))
    // testSceneWithUI("AnimTest", downloads.getChild("3d/Driving.fbx/animations/mixamo.com/BoneByBone.json"))
}