package me.anno.tests.mesh.gltf.writer

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.init()
    if (false) {
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "sponza"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        GLTFWriter().write(PrefabCache[sceneMain]!!, desktop.getChild("$name.glb"))
    } else if (false) {
        // test for non-packed references
        GLTFWriter().write(PrefabCache[documents.getChild("cube bricks.fbx")]!!, desktop.getChild("bricks.glb"))
    } else {
        // test for vertex colors
        GLTFWriter().write(PrefabCache[downloads.getChild("3d/seal gltf/scene.gltf")]!!, desktop.getChild("seal.glb"))
    }
    Engine.requestShutdown()
}