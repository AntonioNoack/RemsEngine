package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.io.files.HasChildrenCache
import me.anno.io.files.ImportType.AUDIO
import me.anno.io.files.ImportType.CONTAINER
import me.anno.io.files.ImportType.CUBEMAP_EQU
import me.anno.io.files.ImportType.EXECUTABLE
import me.anno.io.files.ImportType.IMAGE
import me.anno.io.files.ImportType.LINK
import me.anno.io.files.ImportType.METADATA
import me.anno.io.files.ImportType.TEXT
import me.anno.io.files.ImportType.VIDEO
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
    val musicFolderPath = folder.getChild("musicFolder.png")

    @JvmField
    val textPath = folder.getChild("text.png")

    @JvmField
    val imagePath = folder.getChild("image.png")

    @JvmField
    val imageFolderPath = folder.getChild("imageFolder.png")

    @JvmField
    val videoPath = folder.getChild("video.png")

    @JvmField
    val videoFolderPath = folder.getChild("videoFolder.png")

    @JvmField
    val emptyFolderPath = folder.getChild("emptyFolder.png")

    @JvmField
    val downloadsPath = folder.getChild("downloads.png")

    @JvmField
    val exePath = folder.getChild("executable.png")

    @JvmField
    val documentPath = folder.getChild("document.png")

    @JvmField
    val documentFolderPath = folder.getChild("documentFolder.png")

    @JvmField
    val zipPath = folder.getChild("compressed.png")

    // todo call this every frame where needed, and make file.hasChildren a cache
    fun getDefaultIconPath(isParent: Boolean, file: FileReference, importType: String?): FileReference {
        return if (isParent || file.isDirectory) {
            if (isParent) {
                folderPath
            } else {
                when (file.name.lowercase()) {
                    "music", "musik" -> musicFolderPath
                    "videos", "movies" -> videoFolderPath
                    "documents", "dokumente", "my documents", "meine dokumente" -> documentFolderPath
                    "images", "pictures", "bilder" -> imageFolderPath
                    "downloads" -> downloadsPath
                    else -> if (HasChildrenCache.hasChildren(file, true) ?: true)
                        folderPath else emptyFolderPath
                }
            }
        } else {
            // actually checking the type would need to be done async, because it's slow to ready many, many files
            when (importType) {
                CONTAINER -> zipPath
                IMAGE, "Cubemap", CUBEMAP_EQU -> imagePath // is there a cubemap format?
                TEXT -> textPath
                AUDIO -> musicPath
                VIDEO -> videoPath
                EXECUTABLE -> exePath
                METADATA -> metadataPath
                LINK -> linkPath
                else -> documentPath
            }
        }
    }
}