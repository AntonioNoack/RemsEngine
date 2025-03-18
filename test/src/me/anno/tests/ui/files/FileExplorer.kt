package me.anno.tests.ui.files

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.WindowRenderFlags
import me.anno.engine.OfficialExtensions
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.WindowManagement
import me.anno.gpu.shader.GPUShader.Companion.UniformCacheSize
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.pictures
import kotlin.system.exitProcess

fun main() {
    OfficialExtensions.initForTests()
    if (false) {
        disableRenderDoc()
        testUI3("File Explorer") {
            ECSRegistry.init()
            FileExplorer(null, true, style)
        }
    } else {
        runFileExplorerTest()
    }
}

fun runFileExplorerTest() {
    // test, that I was using for DX11, too
    // there, with limited features, I'm getting 1000 fps
    // might be us using compute shaders... no, same fps
    try {
        // we have our own cache
        UniformCacheSize = 0
        WindowManagement.useSeparateGLFWThread = false
        testUI3("Engine in OpenGL") {
            WindowRenderFlags.enableVSync = false
            WindowRenderFlags.showFPS = true
            FileExplorer(pictures, true, style)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    Engine.requestShutdown()
    exitProcess(0)
}