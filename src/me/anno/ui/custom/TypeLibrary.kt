package me.anno.ui.custom

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.language.translation.Dict
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
        val internalName: String = constructor().javaClass.simpleName
    }

    val typeList get() = listOf<Pair<String, () -> Panel>>(
        Dict["Scene View", "ui.customize.sceneView"] to { SceneView(DefaultConfig.style) },
        Dict["Tree View", "ui.customize.treeView"] to { TreeView(DefaultConfig.style) },
        Dict["Properties", "ui.customize.inspector"] to { PropertyInspector(DefaultConfig.style) },
        Dict["Cutting Panel", "ui.customize.cuttingPanel"] to { CuttingView(DefaultConfig.style) },
        Dict["Timeline", "ui.customize.timeline"] to { TimelinePanel(DefaultConfig.style) },
        Dict["Animations", "ui.customize.graphEditor"] to { GraphEditor(DefaultConfig.style) },
        Dict["Files", "ui.customize.fileExplorer"] to { FileExplorer(DefaultConfig.style) }
    ).map { Type(it.first, it.second) }

    val types get() = typeList.associateBy { it.internalName }

}