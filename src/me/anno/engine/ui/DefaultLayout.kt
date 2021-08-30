package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.style.Style
import me.anno.utils.hpc.SyncMaster

// todo offer to clip to a certain aspect ratio
// todo zoom into to see the pixels

object DefaultLayout {

    lateinit var library: ECSTypeLibrary

    fun createDefaultMainUI(
        projectFile: FileReference,
        world: Entity,
        syncMaster: SyncMaster,
        isGaming: Boolean,
        style: Style
    ): Panel {

        // val pseudoProject = Project("pseudo", File.createTempFile("sth",""))
        // project = pseudoProject

        val customUI = CustomList(true, style)
        customUI.setWeight(10f)

        val animationWindow = CustomList(false, style)

        val libraryBase = ECSTypeLibrary(projectFile, world, syncMaster, isGaming)
        this.library = libraryBase

        val library = libraryBase.uiLibrary

        val treeFiles = CustomList(true, style)
        treeFiles += CustomContainer(ECSTreeView(libraryBase, isGaming, style), library, style)
        // treeFiles += CustomContainer(FileExplorer(fileExplorerOptions, projectFile, style), library, style)
        animationWindow.add(CustomContainer(treeFiles, library, style), 1f)
        animationWindow.add(CustomContainer(SceneView(libraryBase, style), library, style), 3f)
        animationWindow.add(CustomContainer(PropertyInspector({ libraryBase.selection }, style), library, style), 1f)
        animationWindow.setWeight(1f)
        customUI.add(animationWindow, 2f)

        customUI.add(CustomContainer(ECSFileExplorer(projectFile, syncMaster, style), library, style).setWeight(0.3f))

        /*if (!isGaming) {

            val timeline = GraphEditor(style)
            customUI.add(CustomContainer(timeline, library, style), 0.25f)

            val linear = CuttingView(style)
            customUI.add(CustomContainer(linear, library, style), 0.25f)

        }*/

        return customUI

    }


}