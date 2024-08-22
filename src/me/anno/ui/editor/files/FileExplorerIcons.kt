package me.anno.ui.editor.files

import me.anno.utils.OS.res

object FileExplorerIcons {

    private val folder = res.getChild("textures/fileExplorer")

    @JvmField
    val folderPath = folder.getChild("folder.png")

    @JvmField
    val metadataPath = folder.getChild("metadata.png")

    @JvmField
    val linkPath = folder.getChild("link.png")

    @JvmField
    val musicPath = folder.getChild("music.png")

    @JvmField
    val textPath = folder.getChild("text.png")

    @JvmField
    val imagePath = folder.getChild("image.png")

    @JvmField
    val videoPath = folder.getChild("video.png")

    @JvmField
    val emptyFolderPath = folder.getChild("emptyFolder.png")

    @JvmField
    val exePath = folder.getChild("executable.png")

    @JvmField
    val docsPath = folder.getChild("document.png")

    @JvmField
    val zipPath = folder.getChild("compressed.png")
}