package me.anno.ui.input

import me.anno.input.Input
import me.anno.input.Input.setClipboardContent
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.studio.StudioBase
import me.anno.ui.Style
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorer.Companion.copyPathDesc
import me.anno.ui.editor.files.FileExplorer.Companion.openInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorer.Companion.pasteDesc
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager

open class URLInput(
    title: String, style: Style,
    f0: FileReference,
    var extraRightClickOptions: List<FileExplorerOption>,
    var isDirectory: Boolean = false
) : PanelListX(style), InputPanel<FileReference> {

    val base = TextInput(title, "", false, f0.absolutePath, style)

    init {
        base.apply {
            this += WrapAlign.LeftCenter
            addChangeListener {
                val gf = it.toGlobalFile()
                this@URLInput.changeListener(gf)
            }
        }
        // for a symmetric border
        val border = style.getPadding("borderSize", 2).left
        if (border > 0) this += SpacerPanel(border, 0, style).apply { backgroundColor = 0 }
        this += base
    }

    override var isInputAllowed: Boolean
        get() = base.isInputAllowed
        set(value) {
            base.isInputAllowed = value
        }

    override fun setValue(newValue: FileReference, notify: Boolean): URLInput {
        base.setValue(newValue.absolutePath, false)
        if (notify) changeListener(newValue)
        return this
    }

    override val value: FileReference get() = getReference(base.value)

    var changeListener = { _: FileReference -> }
    fun setChangeListener(listener: (FileReference) -> Unit): URLInput {
        this.changeListener = listener
        return this
    }

    fun setResetListener(listener: () -> FileReference): URLInput {
        base.setResetListener { listener().toLocalPath() }
        return this
    }

    fun setText(text: String, notify: Boolean) {
        base.setValue(text.replace('\\', '/'), notify)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return if (action == "DragStart") {
            val lastValue = value
            val title = lastValue.nameWithoutExtension
            val stringContent = lastValue.absolutePath
            StudioBase.dragged = Draggable(stringContent, "File", lastValue, title, style)
            true
        } else super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_RIGHT -> openMenu(windowStack, listOf(
                MenuOption(openInStandardProgramDesc) { value.openInStandardProgram() },
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

    fun setIsSelectedListener(listener: () -> Unit): URLInput {
        base.setIsSelectedListener(listener)
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        if (isInputAllowed && files.isNotEmpty()) {
            if (files.size == 1) {
                setValue(files[0], true)
            } else {
                setValue(files.first(), true)
                LOGGER.warn("Can only paste a single file!, got $files")
            }
        } else super.onPasteFiles(x, y, files)
    }

    override val className: String get() = "URLInput"

    companion object {
        private val LOGGER = LogManager.getLogger(URLInput::class)
    }
}