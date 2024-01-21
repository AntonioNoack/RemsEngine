package me.anno.ui.input

import me.anno.config.DefaultConfig
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.language.translation.NameDesc
import me.anno.engine.EngineBase
import me.anno.input.Clipboard.getClipboardContent
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.FileExplorerOptions.copyPath
import me.anno.ui.editor.files.FileExplorerOptions.editInStandardProgram
import me.anno.ui.editor.files.FileExplorerOptions.openInExplorer
import me.anno.ui.editor.files.FileExplorerOptions.openInStandardProgram
import me.anno.ui.editor.files.FileExplorerOptions.pasteDesc
import me.anno.utils.Color.mixARGB
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

    private val buttons = ArrayList<TextButton>()
    fun addButton(button: TextButton) {
        button.textColor = textColor.withAlpha(0.5f)
        button.disableFocusColors()
        buttons.add(button)
        add(children.lastIndex, button)
    }

    init {
        // base.tooltip = f0.absolutePath
        base.apply {
            alignmentX = AxisAlignment.CENTER
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
                        NameDesc(if (isDirectory) "Select folder" else "Select file"), !isDirectory,
                        isDirectory, false, false, file2.getParent() ?: file2, emptyList()
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
        // for a symmetric border
        val border = style.getPadding("borderSize", 2).left
        if (border > 0) this += SpacerPanel(border, 0, style).apply { backgroundColor = 0 }
        base.weight = 1f
        base2.weight = 1f
        this += base
        addButton(button)
    }

    override var textSize: Float
        get() = base.textSize
        set(value) {
            base.textSize = value
            for (button in buttons) {
                // todo make border size relative
                button.textSize = value
            }
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

    fun setText(text: String, notify: Boolean): FileInput {
        base.setValue(text.replace('\\', '/'), notify)
        return this
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return if (action == "DragStart" && !Input.mouseHasMoved) {
            val value = value
            val title = value.nameWithoutExtension
            val stringContent = value.absolutePath
            EngineBase.dragged = Draggable(stringContent, "File", value, title, style)
            true
        } else super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_RIGHT -> {
                val files = listOf(value)
                openMenu(windowStack, listOf(
                    openInExplorer, openInStandardProgram,
                    editInStandardProgram, copyPath,
                ).map { it.toMenu(this, files) } + listOf(
                    MenuOption(pasteDesc) {
                        val newValue = getReference(getClipboardContent()?.toString() ?: "")
                        setValue(newValue, true)
                    },
                ) + extraRightClickOptions.map {
                    it.toMenu(this, files)
                })
            }
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
        Thumbs[file, size, true] ?: return null
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