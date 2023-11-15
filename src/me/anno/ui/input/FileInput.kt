package me.anno.ui.input

import me.anno.config.DefaultConfig
import me.anno.input.Input
import me.anno.input.Input.setClipboardContent
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.maths.Maths.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorer.Companion.copyPathDesc
import me.anno.ui.editor.files.FileExplorer.Companion.editInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorer.Companion.openInExplorerDesc
import me.anno.ui.editor.files.FileExplorer.Companion.openInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorer.Companion.pasteDesc
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.utils.Color.withAlpha
import me.anno.utils.files.FileChooser
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager

open class FileInput(
    title: String, style: Style,
    val f0: FileReference,
    var extraRightClickOptions: List<FileExplorerOption>,
    var isDirectory: Boolean = false,
) : PanelListX(style), InputPanel<FileReference>, TextStyleable {

    // done file preview, if available
    // todo property inspector, if is mutable prefab
    // (e.g., would be really nice for quick changes to materials)

    val button = TextButton(DefaultConfig["ui.symbol.folder", "\uD83D\uDCC1"], true, style)
    val base = TextInput(title, "", false, f0.toString2(), style)
    val base2 = base.base

    // val text get() = base.text

    init {
        // base.tooltip = f0.absolutePath
        base.apply {
            this += WrapAlign.LeftCenter
            addChangeListener {
                val gf = it.toGlobalFile()
                this@FileInput.changeListener(gf)
                // base.tooltip = gf.absolutePath
            }
        }
        button.apply {
            addLeftClickListener {
                if (isInputAllowed) {
                    var file2 = this@FileInput.value
                    while (file2 != InvalidRef && !file2.exists) {
                        val file3 = file2.getParent() ?: InvalidRef
                        if (file3 == InvalidRef || file3 == file2 || file3.exists) {
                            file2 = file3
                            break
                        } else file2 = file3
                    }
                    FileChooser.selectFiles(
                        !isDirectory, isDirectory, false,
                        file2, false, emptyList()
                    ) { files ->
                        if (files.isNotEmpty()) {
                            setValue(files.first(), true)
                        }
                    }
                }
            }
            tooltip = "Select the file in your default file explorer"
            textColor = textColor.withAlpha(0.5f)
            disableFocusColors()
        }
        this += button
        // for a symmetric border
        val border = style.getPadding("borderSize", 2).left
        if (border > 0) this += SpacerPanel(border, 0, style).apply { backgroundColor = 0 }
        this += base
    }

    private val buttons = ArrayList<TextButton>()
    fun addButton(button: TextButton) {
        button.textColor = textColor.withAlpha(0.5f)
        button.disableFocusColors()
        buttons.add(button)
        add(base.indexInParent, button)
    }

    override var textSize: Float
        get() = base.textSize
        set(value) {
            base.textSize = value
        }

    override var textColor: Int
        get() = base.textColor
        set(value) {
            base.textColor = value
        }

    override var isBold: Boolean
        get() = base.isBold
        set(value) {
            base.isBold = value
        }

    override var isItalic: Boolean
        get() = base.isItalic
        set(value) {
            base.isItalic = value
        }

    override var isInputAllowed: Boolean
        get() = base.isInputAllowed
        set(value) {
            base.isInputAllowed = value
            for (button in buttons) {
                button.isInputAllowed = value
            }
        }

    override fun setValue(newValue: FileReference, mask: Int, notify: Boolean): Panel {
        base.setValue(newValue.toString2(), false)
        if (notify) changeListener(newValue)
        return this
    }

    private fun FileReference.toString2() = toLocalPath()

    override val value: FileReference
        get() = if (base.value == f0.absolutePath)
            f0 else base.value.toGlobalFile()

    var changeListener = { _: FileReference -> }
    fun setChangeListener(listener: (FileReference) -> Unit): FileInput {
        this.changeListener = listener
        return this
    }

    fun setResetListener(listener: () -> FileReference): FileInput {
        base.setResetListener { listener().toLocalPath() }
        return this
    }

    fun setText(text: String, notify: Boolean) {
        base.setValue(text.replace('\\', '/'), notify)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return if (action == "DragStart") {
            val value = value
            val title = value.nameWithoutExtension
            val stringContent = value.absolutePath
            StudioBase.dragged = Draggable(stringContent, "File", value, title, style)
            true
        } else super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_RIGHT -> openMenu(windowStack, listOf(
                MenuOption(openInExplorerDesc) { value.openInExplorer() },
                MenuOption(openInStandardProgramDesc) { value.openInStandardProgram() },
                MenuOption(editInStandardProgramDesc) { value.editInStandardProgram() },
                MenuOption(copyPathDesc) { setClipboardContent(value.absolutePath) },
                MenuOption(pasteDesc) {
                    val newValue = getReference(Input.getClipboardContent()?.toString() ?: "")
                    setValue(newValue, true)
                },
            ) + extraRightClickOptions.map {
                MenuOption(it.nameDesc) { it.onClick(this, listOf(value)) }
            })
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun setIsSelectedListener(listener: () -> Unit): FileInput {
        base.setIsSelectedListener(listener)
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        if (isInputAllowed) {
            if (files.size == 1) {
                setValue(files[0], true)
            } else {
                LOGGER.warn("Can only paste a single file!, got $files")
            }
        } else super.onPasteFiles(x, y, files)
    }

    override fun getTooltipPanel(x: Float, y: Float): Panel? {
        // only if the image is not the default one
        val stdSize = 64
        val size = stdSize - stdSize / 20 // 1/20th is padding
        val file = value
        Thumbs.getThumbnail(file, size, true) ?: return null
        // could be cached...
        val entry = object : FileExplorerEntry(file, style) {
            override fun calculateSize(w: Int, h: Int) {
                super.calculateSize(w, h)
                minW = stdSize
                minH = stdSize
            }
        }
        entry.showTitle = false
        // I like this better than a transparent background
        entry.backgroundRadius = stdSize / 10f
        entry.backgroundColor = mixARGB(backgroundColor, -1, 0.5f)
        return entry
    }

    override val className: String get() = "FileInput"

    companion object {
        private val LOGGER = LogManager.getLogger(FileInput::class)
    }
}