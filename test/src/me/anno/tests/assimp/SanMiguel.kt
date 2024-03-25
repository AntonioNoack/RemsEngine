package me.anno.tests.assimp

import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.GPUImage
import me.anno.image.thumbs.Thumbs
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep

fun main() {

    HiddenOpenGLContext.createOpenGL()

    ShaderLib.init()

    ECSRegistry.init()
    Thumbs.useCacheFolder = true

    val clock = Clock()

    // reading the structure alone: 0.7s
    clock.start()
    // a huge file, which causes the engine to crash :/
    val file = downloads.getChild("San_Miguel.zip/San-Miguel.obj")
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
    val input = file.inputStreamSync()
    while (true) {
        val read = input.read()
        if (read < 0) break
    }
    input.close()
    clock.stop("unzip byte for byte")

    clock.start()
    Sleep.waitUntilDefined(true) {
        PrefabCache[file]
        // loadAssimpStatic(srcFile, null)
    }
    clock.stop("custom")

    clock.start()
    PrefabCache[file]
    clock.stop("assimp")

    val result = Thumbs[file, 512, false]!!
    if (result is Texture2D) GPUImage(result).write(desktop.getChild("miguel.png"))
}