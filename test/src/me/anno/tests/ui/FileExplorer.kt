package me.anno.tests.ui

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.gpu.GFXBase
import me.anno.gpu.GFXBase.disableRenderDoc
import me.anno.gpu.shader.OpenGLShader.Companion.UniformCacheSize
import me.anno.io.files.FileReference
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.utils.OS.pictures
import kotlin.system.exitProcess

fun main() {
    if (false) {
        disableRenderDoc()
        testUI("File Explorer") {
            ECSRegistry.init()
            object : FileExplorer(null, style) {
                override fun getFolderOptions() = emptyList<FileExplorerOption>()
                override fun onDoubleClick(file: FileReference) {}
                override fun onPaste(x: Float, y: Float, data: String, type: String) {}
            }
        }
    } else {
        // test, that I was using for DX11, too
        // there, with limited features, I'm getting 1000 fps
        // might be us using compute shaders... no, same fps
        try {
            // we have our own cache
            UniformCacheSize = 0
            GFXBase.useSeparateGLFWThread = false
            testUI("Engine in OpenGL") {
                StudioBase.instance?.enableVSync = false
                FileExplorer(pictures, style)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        Engine.requestShutdown()
        exitProcess(0)
    }
}