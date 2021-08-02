package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc

class FileExplorerOption(val nameDesc: NameDesc, val onClick: (FileReference) -> Unit)