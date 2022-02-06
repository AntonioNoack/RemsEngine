package me.anno.remsstudio.ui

import me.anno.io.files.FileReference
import me.anno.remsstudio.objects.Transform.Companion.toTransform
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.sceneTabs.SceneTabs
import me.anno.ui.style.Style
import me.anno.utils.files.Files

class StudioFileExplorer(file: FileReference?, style: Style) : FileExplorer(file, style) {

    override fun getRightClickOptions(): List<FileExplorerOption> {
        return listOf(StudioUITypeLibrary.createTransform)
    }

    override fun onDoubleClick(file: FileReference) {
        SceneTabs.open(file)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "Transform" -> pasteTransform(data)
            else -> {
                if (!pasteTransform(data)) {
                    if (data.length < 2048) {
                        val ref = FileReference.getReference(data)
                        if (ref.exists) {
                            switchTo(ref)
                        }// else super.onPaste(x, y, data, type)
                    }// else super.onPaste(x, y, data, type)
                }
            }
        }
    }

    fun pasteTransform(data: String): Boolean {
        val transform = data.toTransform() ?: return false
        var name = transform.name.toAllowedFilename() ?: transform.defaultDisplayName
        // make .json lowercase
        if (name.endsWith(".json", true)) {
            name = name.substring(0, name.length - 5)
        }
        name += ".json"
        Files.findNextFile(folder.getChild(name), 1, '-').writeText(data)
        invalidate()
        return true
    }

    override val className: String = "StudioFileExplorer"

}