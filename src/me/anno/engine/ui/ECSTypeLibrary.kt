package me.anno.engine.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.custom.Type
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.hpc.SyncMaster

class ECSTypeLibrary(
    val projectFile: FileReference,
    var getWorld: () -> Entity,
    val syncMaster: SyncMaster,
    val isGaming: Boolean
) {

    val world get() = getWorld()
    // todo box selecting with shift

    // todo we should be able to edit multiple values at the same time
    var selected: List<Inspectable> = listOf(world)
    var fineSelection: List<Inspectable> = selected

    fun select(major: Inspectable?, minor: Inspectable? = major) {
        selected = if (major == null) emptyList() else listOf(major)
        fineSelection = if (minor == null) emptyList() else listOf(minor)
        lastSelection = major
    }

    val typeList = listOf<Pair<String, () -> Panel>>(
        // todo not all stuff here makes sense
        // todo some stuff is (maybe) missing, e.g. animation panels, particle system editors, ...
        Dict["Scene View", "ui.customize.sceneView"] to { SceneView(this, DefaultConfig.style) },
        Dict["Tree View", "ui.customize.treeView"] to { ECSTreeView(this, isGaming, DefaultConfig.style) },
        Dict["Properties", "ui.customize.inspector"] to { PropertyInspector({ selected }, DefaultConfig.style) },
        // Dict["Cutting Panel", "ui.customize.cuttingPanel"] to { CuttingView(DefaultConfig.style) },
        // Dict["Timeline", "ui.customize.timeline"] to { TimelinePanel(DefaultConfig.style) },
        // Dict["Animations", "ui.customize.graphEditor"] to { GraphEditor(DefaultConfig.style) },
        Dict["Files", "ui.customize.fileExplorer"] to { ECSFileExplorer(projectFile, syncMaster, DefaultConfig.style) }
    ).map { Type(it.first, it.second) }.toMutableList()

    val uiLibrary = UITypeLibrary(typeList)

    companion object {

        var lastSelection: Inspectable? = null

    }

}