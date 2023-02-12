package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads

fun main() {
    ECSRegistry.init()
    if (false) {
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "sponza"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        GLTFWriter.write(PrefabCache[sceneMain]!!, desktop.getChild("$name.glb"))
    } else {
        // test for non-packed references
        GLTFWriter.write(PrefabCache[documents.getChild("cube bricks.fbx")]!!, desktop.getChild("bricks.glb"))
    }
    Engine.requestShutdown()
}