package me.anno.tests.mesh

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.initMeshes()
    testUI("MistubaReader") {
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "veach-lamp"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        SceneView.testScene(PrefabCache.loadScenePrefab(sceneMain).createInstance())
    }
}