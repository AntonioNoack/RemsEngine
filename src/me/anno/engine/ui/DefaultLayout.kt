package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.style.Style

// todo offer to clip to a certain aspect ratio
// todo zoom into to see the pixels

object DefaultLayout {

    fun createDefaultMainUI(projectFile: FileReference, world: Entity, isGaming: Boolean, style: Style): Panel {

        // val pseudoProject = Project("pseudo", File.createTempFile("sth",""))
        // project = pseudoProject

        val customUI = CustomList(true, style)
        customUI.setWeight(10f)

        val animationWindow = CustomList(false, style)
        customUI.add(animationWindow, 2f)

        val libraryBase = ECSTypeLibrary(projectFile, world, isGaming)
        val library = libraryBase.library

        val treeFiles = CustomList(true, style)
        treeFiles += CustomContainer(ECSTreeView(libraryBase, isGaming, style), library, style)
        treeFiles += CustomContainer(FileExplorer(projectFile, style), library, style)
        animationWindow.add(CustomContainer(treeFiles, library, style), 1f)
        animationWindow.add(CustomContainer(RenderView(world, RenderView.Mode.EDITING, style), library, style), 3f)
        animationWindow.add(CustomContainer(PropertyInspector({ libraryBase.selection }, style), library, style), 1f)
        animationWindow.setWeight(1f)

        /*if (!isGaming) {

            val timeline = GraphEditor(style)
            customUI.add(CustomContainer(timeline, library, style), 0.25f)

            val linear = CuttingView(style)
            customUI.add(CustomContainer(linear, library, style), 0.25f)

        }*/

        return customUI

    }

}