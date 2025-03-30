package me.anno.tests.mesh.gltf.writer

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.async.Callback

fun main() {
    OfficialExtensions.initForTests()
    // we load the file using Assimp
    val src = downloads.getChild("3d/azeria/scene.gltf")
    val source = PrefabCache.getPrefabSampleInstance(src)!!
    // then save it ourselves using our GLTFWriter
    val tmp = desktop.getChild("Azeria.glb")
    GLTFWriter().write(source, tmp, Callback.onSuccess {
        // and finally try to load it again, and hopefully, it still looks the same :)
        testSceneWithUI("Animated?", tmp)
    })
}