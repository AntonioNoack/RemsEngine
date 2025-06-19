package me.anno.tests.mesh.gltf.writer

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.async.Callback

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.init()
    val callback = Callback.finish {
        Engine.requestShutdown()
    }
    if (false) {
        val main = downloads.getChild("gradientdomain-scenes.zip/gradientdomain-scenes")
        val name = "sponza"
        val sceneMain = main.getChild("$name/$name-gpt.xml/Scene.json")
        GLTFWriter().write(
            PrefabCache[sceneMain].waitFor()!!, desktop.getChild("$name.glb"),
            callback
        )
    } else if (false) {
        // test for non-packed references
        GLTFWriter().write(
            PrefabCache[documents.getChild("cube bricks.fbx")].waitFor()!!, desktop.getChild("bricks.glb"),
            callback
        )
    } else {
        // test for vertex colors
        GLTFWriter().write(
            PrefabCache[downloads.getChild("3d/seal gltf/scene.gltf")].waitFor()!!, desktop.getChild("seal.glb"),
            callback
        )
    }
}