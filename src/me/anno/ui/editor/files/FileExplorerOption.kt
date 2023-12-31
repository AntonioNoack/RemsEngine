package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.menu.MenuOption

class FileExplorerOption(val nameDesc: NameDesc, val onClick: (Panel, List<FileReference>) -> Unit) {
    fun toMenu(panel: Panel, files: List<FileReference>): MenuOption {
        return MenuOption(nameDesc) {
            onClick(panel, files)
        }
    }
}