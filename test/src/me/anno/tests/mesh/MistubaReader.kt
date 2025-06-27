package me.anno.tests.mesh

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ScenePrefab
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.OS.downloads

fun loadScenePrefab(file: FileReference): Prefab {
    val prefab = PrefabCache[file, maxPrefabDepth].waitFor()?.prefab
        ?: Prefab("Entity").apply { this.parentPrefabFile = ScenePrefab }
    prefab.sourceFile = file
    return prefab
}

fun main() {
    OfficialExtensions.initForTests()
    testUI("MistubaReader") {
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "veach-lamp"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        SceneView.createSceneUI(loadScenePrefab(sceneMain).newInstance())
    }
}