package me.anno.engine.ui

import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.custom.CustomContainer
import me.anno.ui.custom.CustomList
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.Style
import me.anno.utils.OS.documents

object DefaultLayout {

    fun createDefaultMainUI(
        projectFile: FileReference,
        style: Style
    ): Panel {

        // val pseudoProject = Project("pseudo", File.createTempFile("sth",""))
        // project = pseudoProject

        val customUI = CustomList(true, style)
        customUI.weight = 10f

        val animationWindow = CustomList(false, style)

        val libraryBase = EditorState
        val library = libraryBase.uiLibrary

        animationWindow.add(CustomContainer(ECSTreeView(libraryBase, style), library, style), 1f)
        animationWindow.add(CustomContainer(SceneView(libraryBase, PlayMode.EDITING, style), library, style), 3f)
        animationWindow.add(CustomContainer(PropertyInspector({ libraryBase.selection }, style), library, style), 1f)
        animationWindow.weight = 1f
        customUI.add(animationWindow, 2f)

        val explorers = CustomList(false, style).apply { weight = 0.3f }
        explorers.add(CustomContainer(ECSFileExplorer(projectFile, style), library, style))
        explorers.add(CustomContainer(ECSFileExplorer(documents, style), library, style))

        customUI.add(explorers)

        /*if (!isGaming) {

            val timeline = GraphEditor(style)
            customUI.add(CustomContainer(timeline, library, style), 0.25f)

            val linear = CuttingView(style)
            customUI.add(CustomContainer(linear, library, style), 0.25f)

        }*/

        return customUI

    }


}