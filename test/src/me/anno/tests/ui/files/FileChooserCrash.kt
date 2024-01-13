package me.anno.tests.ui.files

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.OS.documents
import me.anno.utils.files.FileChooser
import me.anno.utils.files.FileExtensionFilter

/**
 * when we went in subdirectories, and created the file chooser in a new window, it crashed with an OpenGL segfault
 * */
fun main() {
    RenderDoc.disableRenderDoc()

    // primary bug:
    //  GL_INVALID_OPERATION error generated. Object is owned by another context and may not be bound here., source: API, type: ERROR, id: invalid operation, severity: HIGH
    //  https://stackoverflow.com/questions/40954738/how-to-properly-do-context-sharing-with-glfw -> Framebuffers aren't shared by contexts, because they are considered light-weight

    // todo bug: you can get the button in the first window to wobble by selecting it,
    //  and then clicking in the second window -> mouse coordinates are wrong &
    //  isHovered or sth like that is wrong

    testUI3("FileChooserSave") {
        TextButton("Click Me", style).addLeftClickListener {
            FileChooser.openInSeparateWindow = true
            FileChooser.selectFiles(
                NameDesc("Choose a file"), true, false, true, true,
                documents, listOf(
                    FileExtensionFilter(NameDesc("Everything"), emptyList()),
                    FileExtensionFilter(NameDesc("Images"), listOf("png", "jpg", "webp", "svg")),
                    FileExtensionFilter(NameDesc("PNGs"), listOf("png")),
                    FileExtensionFilter(NameDesc("PDFs"), listOf("pdf")),
                    FileExtensionFilter(NameDesc("SVGs"), listOf("svg")),
                )
            ) {
                println("Selected $it")
                Engine.requestShutdown()
            }
        }
    }
}