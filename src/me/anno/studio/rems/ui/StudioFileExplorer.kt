package me.anno.studio.rems.ui

import me.anno.io.files.FileReference
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style

class StudioFileExplorer(file: FileReference?, style: Style) : FileExplorer(file, style) {

    override fun getRightClickOptions(): List<FileExplorerOption> {
        return listOf(StudioUITypeLibrary.createTransform)
    }

    override fun onDoubleClick(file: FileReference) {
        SceneTabs.open(file)
    }

    override val className: String = "FileExplorer"

}