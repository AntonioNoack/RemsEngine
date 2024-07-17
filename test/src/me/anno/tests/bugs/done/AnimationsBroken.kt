package me.anno.tests.bugs.done

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // bug: animation isn't playing -> gameTimeN was missing a factor of 1e9
    OfficialExtensions.initForTests()
    testSceneWithUI("Animations Broken", downloads.getChild("3d/Talking On Phone.fbx"))
}