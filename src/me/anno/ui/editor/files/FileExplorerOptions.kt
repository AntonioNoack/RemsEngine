package me.anno.ui.editor.files

import me.anno.image.thumbs.ThumbnailCache
import me.anno.input.Clipboard.setClipboardContent
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.WindowStack
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelStack
import me.anno.ui.base.image.ImagePanel
import me.anno.ui.base.image.VideoPanel
import me.anno.ui.base.menu.Menu
import me.anno.utils.files.OpenFileExternally

object FileExplorerOptions {

    @JvmField
    val renameDesc = NameDesc(
        "Rename",
        "Change the name of this file",
        "ui.file.rename"
    )

    @JvmField
    val openInExplorerDesc = NameDesc(
        "Open In Explorer",
        "Show the file in your default file explorer",
        "ui.file.openInExplorer"
    )

    @JvmField
    val openInStandardProgramDesc = NameDesc(
        "Show In Standard Program",
        "Open the file using your default viewer",
        "ui.file.openInStandardProgram"
    )

    @JvmField
    val editInStandardProgramDesc = NameDesc(
        "Edit In Standard Program",
        "Edit the file using your default editor",
        "ui.file.editInStandardProgram"
    )

    @JvmField
    val copyPathDesc = NameDesc(
        "Copy Path",
        "Copy the path of the file to clipboard",
        "ui.file.copyPath"
    )

    @JvmField
    val copyNameDesc = NameDesc(
        "Copy Name",
        "Copy the name of the file to clipboard",
        "ui.file.copyName"
    )

    @JvmField
    val deleteDesc = NameDesc(
        "Delete",
        "Delete this file",
        "ui.file.delete"
    )

    @JvmField
    val pasteDesc = NameDesc(
        "Paste",
        "Paste your clipboard",
        "ui.file.paste"
    )

    @JvmField
    val openInImageViewerDesc = NameDesc(
        "Open Image Viewer",
        "If an image is too small, and you don't want to resize everything in the file explorer (mouse wheel)",
        "ui.file.openImageViewer"
    )

    val rename = FileExplorerOption(renameDesc) { p, files ->
        FileExplorerEntry.rename(p.windowStack, p as? FileExplorer, files)
    }
    val openInExplorer = FileExplorerOption(openInExplorerDesc) { _, files ->
        OpenFileExternally.openInExplorer(files)
    }
    val openInStandardProgram = FileExplorerOption(openInStandardProgramDesc) { _, files ->
        OpenFileExternally.openInStandardProgram(files)
    }
    val editInStandardProgram = FileExplorerOption(editInStandardProgramDesc) { _, files ->
        OpenFileExternally.editInStandardProgram(files)
    }
    val copyPath = FileExplorerOption(copyPathDesc) { _, files ->
        setClipboardContent(files.joinToString {
            enquoteIfNecessary(it.absolutePath)
        })
    }
    val copyName = FileExplorerOption(copyNameDesc) { _, files ->
        setClipboardContent(files.joinToString {
            enquoteIfNecessary(it.name)
        })
    }
    val pinToFavourites = FileExplorerOption(
        NameDesc(
            "Pin to Favourites",
            "Add file to quick access bar",
            "ui.file.pinToFavourites"
        )
    ) { _, files ->
        Favourites.addFavouriteFiles(files)
    }
    val invalidateThumbnails = FileExplorerOption(
        NameDesc(
            "Invalidate Thumbnails",
            "Regenerates them when needed",
            "ui.file.invalidateThumbnails"
        )
    ) { _, files ->
        for (file in files) {
            ThumbnailCache.invalidate(file)
        }
    }
    val delete = FileExplorerOption(deleteDesc) { p, files ->
        FileExplorerEntry.askToDeleteFiles(p.windowStack, p as? FileExplorer, files)
    }

    val openImageViewer = FileExplorerOption(openInImageViewerDesc) { p, files ->
        openImageViewerImpl(p.windowStack, files, p.style)
    }

    fun enquoteIfNecessary(str: String): String {
        return if (' ' in str || '"' in str) {
            "\"${str.replace("\"", "\\\"")}\""
        } else str
    }


    fun openImageViewerImpl(windowStack: WindowStack, files: List<FileReference>, style: Style) {
        val nullMeta = getMeta(files.first()).waitFor()
        val deep = style.getChild("deep")
        val imagePanel = if (nullMeta != null && nullMeta.hasVideo && nullMeta.videoFrameCount > 1) {
            // todo switch between videos
            VideoPanel.createSimpleVideoPlayer(files.first(), deep)
        } else {
            ImageViewer(files, deep).enableControls()
        }
        val stack = PanelStack(deep)
        stack.add(imagePanel)
        stack.add(
            TextButton(NameDesc("Close"), style)
                .addLeftClickListener(Menu::close)
                .apply {
                    alignmentX = AxisAlignment.MIN
                    alignmentY =
                        if (imagePanel is ImagePanel) AxisAlignment.MAX
                        else AxisAlignment.MIN
                })
        windowStack.push(stack)
        imagePanel.requestFocus()
    }
}