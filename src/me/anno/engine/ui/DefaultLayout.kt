package me.anno.engine.ui

import me.anno.engine.ECSWorld
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.cutting.CuttingView
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.graphs.GraphEditor
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.style.Style

// todo offer to clip to a certain aspect ratio
// todo zoom into to see the pixels

object DefaultLayout {

    fun createDefaultMainUI(world: ECSWorld, isGaming: Boolean, style: Style): Panel {

        // val pseudoProject = Project("pseudo", File.createTempFile("sth",""))
        // project = pseudoProject

        val customUI = CustomList(true, style)
        customUI.setWeight(10f)

        val animationWindow = CustomList(false, style)
        customUI.add(animationWindow, 2f)

        // todo use a different library, because we have different elements
        val library = ECSTypeLibrary(world, isGaming).library

        val treeFiles = CustomList(true, style)
        treeFiles += CustomContainer(ECSTreeView(world, isGaming, style), library, style)
        treeFiles += CustomContainer(FileExplorer(style), library, style)
        animationWindow.add(CustomContainer(treeFiles, library, style), 0.5f)
        animationWindow.add(CustomContainer(SceneView(style), library, style), 2f)
        animationWindow.add(CustomContainer(PropertyInspector(style), library, style), 0.5f)
        animationWindow.setWeight(1f)

        if (!isGaming) {
            val timeline = GraphEditor(style)
            customUI.add(CustomContainer(timeline, library, style), 0.25f)

            val linear = CuttingView(style)
            customUI.add(CustomContainer(linear, library, style), 0.25f)
        }

        return customUI

    }

}