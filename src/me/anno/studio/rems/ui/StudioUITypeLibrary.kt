package me.anno.studio.rems.ui

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.Selection
import me.anno.ui.base.Panel
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.custom.Type
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorer.Companion.invalidateFileExplorers
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneView.SceneView

class StudioUITypeLibrary : UITypeLibrary(typeList) {

    companion object {

        val createTransform = FileExplorerOption(
            NameDesc("Create Component", "Create a new folder component", "ui.newComponent")
        ) { folder ->
            askName(
                NameDesc("Name", "", "ui.newComponent.askName"),
                "",
                NameDesc("Create"),
                { -1 }) {
                val validName = it.toAllowedFilename()
                if (validName != null) {
                    getReference(folder, "${validName}.json").writeText(
                        Transform()
                            .apply { name = it }
                            .toString())
                    invalidateFileExplorers()
                }
            }
        }

        val typeList = listOf<Pair<String, () -> Panel>>(
            Dict["Scene View", "ui.customize.sceneView"] to { SceneView(DefaultConfig.style) },
            Dict["Tree View", "ui.customize.treeView"] to { StudioTreeView(DefaultConfig.style) },
            Dict["Properties", "ui.customize.inspector"] to
                    { PropertyInspector({ Selection.selectedInspectable }, DefaultConfig.style, Unit) },
            Dict["Cutting Panel", "ui.customize.cuttingPanel"] to { CuttingView(DefaultConfig.style) },
            Dict["Timeline", "ui.customize.timeline"] to { TimelinePanel(DefaultConfig.style) },
            Dict["Animations", "ui.customize.graphEditor"] to { GraphEditor(DefaultConfig.style) },
            Dict["Files", "ui.customize.fileExplorer"] to
                    { StudioFileExplorer(RemsStudio.project?.scenes, DefaultConfig.style) }
        ).map { Type(it.first, it.second) }.toMutableList()
    }

}