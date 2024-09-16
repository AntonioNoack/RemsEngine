package me.anno.ui.editor.files

import me.anno.gpu.drawing.DrawTexts.drawTextOrFail
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureCache
import me.anno.image.thumbs.Thumbs
import me.anno.input.Clipboard.setClipboardContent
import me.anno.input.Key
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
import kotlin.math.max

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
            Thumbs.invalidate(file)
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

    private class ImageViewer(val files: List<FileReference>, style: Style) : ImagePanel(style) {

        var index = 0
        val file get() = files[index]

        init {
            showAlpha = true
        }

        override fun getTexture(): ITexture2D? {
            return TextureCache[file, true] ?: Thumbs[file, max(width, height), true] ?: return null
        }

        override fun onUpdate() {
            super.onUpdate()
            getTexture() // keep texture loaded
        }

        val font = style.getFont("text")
        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            // todo draw controls into the background
            // todo show image statistics in a corner?
            // todo switch sources on the parent, so folders with images and videos can be (dis)played properly?
            // todo if texture still loads, show loading circle
            val failed = drawTextOrFail( // draw file name at bottom center
                x + width / 2, y + height, font, file.name,
                -1, backgroundColor, width, -1,
                AxisAlignment.CENTER, AxisAlignment.MAX
            )
            if (failed) invalidateDrawing()
        }

        fun step(di: Int) {
            index = (index + di) % files.size
            invalidateDrawing()
        }

        fun prev() = step(files.size - 1)
        fun next() = step(1)
        fun reset() {
            zoom = 1f
            offsetX = 0f
            offsetY = 0f
            invalidateDrawing()
        }

        override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
            if (button == Key.BUTTON_LEFT && !long) next()
            else prev()
        }

        override fun onDoubleClick(x: Float, y: Float, button: Key) {
            onMouseClicked(x, y, button, false)
        }

        override fun onKeyTyped(x: Float, y: Float, key: Key) {
            when (key) {
                Key.KEY_ARROW_LEFT, Key.KEY_ARROW_UP, Key.KEY_PAGE_UP -> prev()
                Key.KEY_ARROW_RIGHT, Key.KEY_ARROW_DOWN, Key.KEY_PAGE_DOWN -> next()
                Key.KEY_0, Key.KEY_KP_0 -> reset()
                Key.KEY_SPACE -> toggleFiltering()
                else -> super.onKeyTyped(x, y, key)
            }
        }

        fun toggleFiltering() {
            filtering =
                if (filtering == Filtering.NEAREST) Filtering.LINEAR
                else Filtering.NEAREST
            invalidateDrawing()
        }
    }

    fun openImageViewerImpl(windowStack: WindowStack, files: List<FileReference>, style: Style) {
        val nullMeta = getMeta(files.first(), false)
        val imagePanel = if (nullMeta != null && nullMeta.hasVideo && nullMeta.videoFrameCount > 1) {
            // todo switch between videos
            VideoPanel.createSimpleVideoPlayer(files.first())
        } else {
            ImageViewer(files, style).enableControls()
        }
        val stack = PanelStack(style)
        stack.add(imagePanel)
        stack.add(TextButton(NameDesc("Close"), style)
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