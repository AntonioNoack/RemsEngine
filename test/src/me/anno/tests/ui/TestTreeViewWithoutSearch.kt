package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.ECSTreeView
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main(){
    // create tree view
    // todo hide search
    // todo make sure it's gone
    disableRenderDoc()
    testUI3("TreeView: HideSearch", ECSTreeView(style).apply {
        searchPanel.hide()
    })
}