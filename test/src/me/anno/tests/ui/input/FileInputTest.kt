package me.anno.tests.ui.input

import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

// todo Java-AWT doesn't support colored emojis, only outlines
//  we'd love to have colorful emojis...
//  so let's download a partial pack, but it somewhere into our files,
//  and when we encounter an emoji, use that packed emoji/download it?

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    FontManager.getTexture(Font("", 15f), "Folder: \uD83D\uDCC1", -1, -1, 10_000)
        .waitFor()!!.write(desktop.getChild("FolderIcon.png"))
}