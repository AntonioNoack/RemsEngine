package me.anno.ui.custom

import me.anno.config.DefaultConfig
import me.anno.ui.base.Panel
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.editor.treeView.TreeView

object TypeLibrary {

    class Type(val displayName: String, val constructor: () -> Panel){
        val internalName = constructor().javaClass.simpleName
    }

    val types = listOf(
        "Scene View" to { SceneView(DefaultConfig.style) },
        "Tree View" to { TreeView(DefaultConfig.style) },
        "Inspector" to { PropertyInspector(DefaultConfig.style) },
        "Cutting Panel" to { CuttingView(DefaultConfig.style) },
        "Timeline" to { TimelinePanel(DefaultConfig.style) },
        "Graph Editor" to { GraphEditor(DefaultConfig.style) },
        "Files" to { FileExplorer(DefaultConfig.style) }
    ).map { Type(it.first, it.second) }.associateBy { it.internalName }

}