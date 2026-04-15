package me.anno.tests.engine.scenes

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.tests.engine.material.createLighting
import me.anno.utils.OS.downloads

fun main() {
    // todo the roof is missing???
    OfficialExtensions.initForTests()

    val folder = downloads.getChild("3d/ogldev-source/crytek_sponza")
    val scene = Entity("Scene")
        .add(PrefabCache.newInstance(folder.getChild("sponza.fbx")).waitFor() as Entity)

    // todo it would be nice to have some GI
    createLighting(scene, 20f)

    testSceneWithUI("Sponza", scene)
}
