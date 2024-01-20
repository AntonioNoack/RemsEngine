package me.anno.tests.image

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.io.files.Reference.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    val ori = getReference("res://icon.png")
    if (true) {
        disableRenderDoc()
        testUI3("Components") {
            FileExplorer(ori, true, style)
        }
    } else {
        HiddenOpenGLContext.createOpenGL()
        val src = ori.getChild("r.png")
        Thumbs[src, 512, false]!!.write(desktop.getChild("icon-thumbs.png"))
        Engine.requestShutdown()
    }
}

// todo add res:// as a root folder (?) to FileExplorer