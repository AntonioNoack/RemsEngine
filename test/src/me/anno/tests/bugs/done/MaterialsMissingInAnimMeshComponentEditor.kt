package me.anno.tests.bugs.done

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // fixed bug: materials list was missing in AnimMeshComponent
    // fixed bug: materials list can be edited, even though the asset is read-only
    OfficialExtensions.initForTests()
    testSceneWithUI("Material list missing", downloads.getChild("3d/Talking On Phone.fbx"))
}