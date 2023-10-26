package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel

class FileExplorerOption(val nameDesc: NameDesc, val onClick: (Panel, List<FileReference>) -> Unit)