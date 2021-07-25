package me.anno.engine.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.custom.Type
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.graphs.GraphEditor

class ECSTypeLibrary(val projectFile: FileReference, val world: Entity, val isGaming: Boolean) {

    var selection: Inspectable? = world

    val typeList = listOf<Pair<String, () -> Panel>>(
        // todo not all stuff here makes sense
        // todo some stuff is (maybe) missing, e.g. animation panels, particle system editors, ...
        Dict["Scene View", "ui.customize.sceneView"] to { RenderView(world, DefaultConfig.style) },
        Dict["Tree View", "ui.customize.treeView"] to { ECSTreeView(this, isGaming, DefaultConfig.style) },
        Dict["Properties", "ui.customize.inspector"] to { PropertyInspector({ selection }, DefaultConfig.style) },
        Dict["Cutting Panel", "ui.customize.cuttingPanel"] to { CuttingView(DefaultConfig.style) },
        Dict["Timeline", "ui.customize.timeline"] to { TimelinePanel(DefaultConfig.style) },
        Dict["Animations", "ui.customize.graphEditor"] to { GraphEditor(DefaultConfig.style) },
        Dict["Files", "ui.customize.fileExplorer"] to { FileExplorer(projectFile, DefaultConfig.style) }
    ).map { Type(it.first, it.second) }.toMutableList()

    val library = UITypeLibrary(typeList)

}