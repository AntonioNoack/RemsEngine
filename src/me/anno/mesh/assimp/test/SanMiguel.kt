package me.anno.mesh.assimp.test

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.ui.editor.files.thumbs.Thumbs
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep

fun main() {

    HiddenOpenGLContext.createOpenGL()

    ShaderLib.init()
    TextureLib.init()
    ECSShaderLib.init()

    ECSRegistry.init()
    Thumbs.useCacheFolder = true

    val clock = Clock()

    // reading the structure alone: 0.7s
    clock.start()
    // a huge file, which causes the engine to crash :/
    val file = getReference(downloads, "San_Miguel.zip/San-Miguel.obj")
    clock.stop("creating reference")

    // 3.3s
    /*clock.start()
    val input1 = file.inputStream()
    val buffer = ByteArray(1024)
    while (true) {
        val read = input1.read(buffer)
        if (read < 0) break
    }
    input1.close()
    clock.stop("unzip to buffered")*/

    // same as not buffered, because internally, it is already buffered
    /*clock.start()
    val input0 = file.inputStream().buffered()
    while (true) {
        val read = input0.read()
        if (read < 0) break
    }
    input0.close()
    clock.stop("unzip buffered")*/

    clock.start()
    val input = file.inputStream()
    while (true) {
        val read = input.read()
        if (read < 0) break
    }
    input.close()
    clock.stop("unzip byte for byte")

    clock.start()
    Sleep.waitUntilDefined(true) {
        PrefabCache.loadPrefab(file)
        // loadAssimpStatic(srcFile, null)
    }
    clock.stop("custom")

    clock.start()
    StaticMeshesLoader().load(file)
    clock.stop("assimp")

    // val file = getReference(documents, "sphere.obj")

    // static is fine:
    /*val loaded = StaticMeshesLoader().load(file)
    val entity = loaded.hierarchy
    Thumbs.generateEntityFrame(getReference(desktop, "debug.png"), 512, entity) {}
*/
    Thumbs.generateAssimpMeshFrame(file, getReference(desktop, "miguel.png"), 512) {}

}