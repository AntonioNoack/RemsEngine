package me.anno.ui.editor.files

import me.anno.io.files.FileReference.Companion.getReference

object FileExplorerIcons {

    @JvmField
    val folderPath = getReference("res://file/folder.png")

    @JvmField
    val musicPath = getReference("res://file/music.png")

    @JvmField
    val textPath = getReference("res://file/text.png")

    @JvmField
    val imagePath = getReference("res://file/image.png")

    @JvmField
    val videoPath = getReference("res://file/video.png")

    @JvmField
    val emptyFolderPath = getReference("res://file/empty_folder.png")

    @JvmField
    val exePath = getReference("res://file/executable.png")

    @JvmField
    val docsPath = getReference("res://file/document.png")

    @JvmField
    val zipPath = getReference("res://file/compressed.png")
}