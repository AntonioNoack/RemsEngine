package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS

fun main() {
    ECSRegistry.init()
    if (false) {
        val main = OS.downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "sponza"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        GLTFWriter.write(PrefabCache[sceneMain]!!, OS.desktop.getChild("$name.glb"))
    } else {
        // test for non-packed references
        GLTFWriter.write(PrefabCache[OS.documents.getChild("cube bricks.fbx")]!!, OS.desktop.getChild("bricks.glb"))
    }
    Engine.requestShutdown()
}