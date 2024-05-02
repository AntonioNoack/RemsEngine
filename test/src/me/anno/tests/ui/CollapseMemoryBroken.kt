package me.anno.tests.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.OS.desktop

fun main() {
    // todo why does it work here, and didn't work with the other one???
    val scene = Entity("Scene")
    val child = Entity("Child", scene)
    child.add(MeshComponent())

    val tmpFile = desktop.getChild("tmpScene.json")
    val prefab = (scene.ref as PrefabReadable).readPrefab()
    tmpFile.writeText(JsonStringWriter.toText(prefab, InvalidRef))
    testSceneWithUI("Collapse-Memory is broken", tmpFile)
}