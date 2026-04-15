package me.anno.tests.engine.scenes

import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.tests.engine.material.createLighting

fun main() {

    OfficialExtensions.initForTests()

    // todo this easily runs OOM... can we extend it/find out why?
    val folder = getReference("/media/antonio/4TB WDRed/Assets/SampleScenes")
    val scene = Entity("Scene")
        .add(PrefabCache.newInstance(folder.getChild("main_sponza/NewSponza_Main_glTF_003.gltf")).waitFor() as Entity)
        .add(PrefabCache.newInstance(folder.getChild("pkg_a_curtains/NewSponza_Curtains_glTF.gltf")).waitFor() as Entity)
        .add(PrefabCache.newInstance(folder.getChild("pkg_b_ivy1/NewSponza_IvyGrowth_glTF.gltf")).waitFor() as Entity)

    // todo it would be nice to have some GI
    createLighting(scene, 20f)

    testSceneWithUI("Sponza", scene)
}
