package me.anno.tests.ui.input

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.input.ActionManager
import me.anno.io.files.Reference.getReference
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.code.HexEditor
import java.io.File

fun main() {
    disableRenderDoc()
    testUI3("Hex Editor") {
        ActionManager.register("HexEditor.s.t.c", "Save")
        val list = PanelListX(DefaultConfig.style)
        val ideaProjects = getReference(File(".").absolutePath).getParent()
        val files = ideaProjects
            .getChild("RemsStudio/out/artifacts/Universal")
            .listChildren().filter { it.lcExtension == "exe" }
        for (file1 in files) {
            list.add(HexEditor(DefaultConfig.style).apply {
                file = file1
                compareTo.addAll(files)
                compareTo.remove(file1)
                showAddress = list.children.isEmpty()
            })
        }
        ScrollPanelXY(list, Padding.Zero, DefaultConfig.style)
    }
}