package me.anno.tests.image

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.ThumbnailCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res

fun main() {
    OfficialExtensions.initForTests()
    val ori = res.getChild("icon.png")
    if (true) {
        testUI3("Components") {
            FileExplorer(ori, true, style)
        }
    } else {
        HiddenOpenGLContext.createOpenGL()
        val src = ori.getChild("r.png")
        ThumbnailCache.getEntry(src, 512).waitFor()!!
            .write(desktop.getChild("icon-thumbs.png"))
        Engine.requestShutdown()
    }
}

// todo add res:// as a root folder to FileExplorer?