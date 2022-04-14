package me.anno.ui.input

import me.anno.config.DefaultConfig
import me.anno.input.MouseButton
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.files.FileExplorer.Companion.editInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorer.Companion.openInExplorerDesc
import me.anno.ui.editor.files.FileExplorer.Companion.openInStandardProgramDesc
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.style.Style
import me.anno.utils.files.FileExplorerSelectWrapper
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager

class FileInput(
    title: String, style: Style,
    f0: FileReference,
    var extraRightClickOptions: List<FileExplorerOption>,
    var isDirectory: Boolean = false
) : PanelListX(style), InputPanel<FileReference> {

    // todo file preview, if available
    // todo property inspector, if is mutable prefab
    // (e.g. would be really nice for quick changes to materials)

    val button = TextButton(DefaultConfig["ui.symbol.folder", "\uD83D\uDCC1"], true, style)
    val base = TextInput(title, "", false, f0.toString2(), style)
    val base2 = base.base

    // val text get() = base.text

    init {
        base.setTooltip(f0.absolutePath)
        base.apply {
            this += WrapAlign.LeftCenter
            addChangeListener {
                val gf = it.toGlobalFile()
                this@FileInput.changeListener(gf)
                base.setTooltip(gf.absolutePath)
            }
        }
        button.apply {
            addLeftClickListener {
                var file2 = file
                while (file2 != InvalidRef && !file2.exists) {
                    val file3 = file2.getParent() ?: InvalidRef
                    if (file3 == InvalidRef || file3 == file2 || file3.exists) {
                        file2 = file3
                        break
                    } else {
                        file2 = file3
                    }
                }
                val file3 = if (file2 == InvalidRef) null else (file2 as? FileFileRef)?.file
                // todo select the file using our own explorer (?), because ours may be better
                FileExplorerSelectWrapper.selectFileOrFolder(file3, isDirectory) { file ->
                    if (file != null) {
                        setValue(getReference(file), true)
                    }
                }
            }
            setTooltip("Select the file in your default file explorer")
            textColor = textColor and 0x7fffffff
            focusTextColor = textColor
        }
        this += button
        // for a symmetric border
        val border = style.getPadding("borderSize", 2).left
        if (border > 0) this += SpacerPanel(border, 0, style).apply { backgroundColor = 0 }
        this += base//ScrollPanelX(base, Padding(), style, AxisAlignment.MIN)
    }

    /*fun setValue(file: FileReference, notify: Boolean): FileInput {
        base.setValue(file.toString2(), false)
        if (notify) changeListener(getReference(file))
        return this
    }*/

    override fun setValue(value: FileReference, notify: Boolean): FileInput {
        base.setValue(value.toString2(), false)
        if (notify) changeListener(value)
        return this
    }

    // private fun File.toString2() = toLocalPath()
    private fun FileReference.toString2() = toLocalPath()
    // toString().replace('\\', '/') // / is easier to type

    val file get(): FileReference = base.lastValue.toGlobalFile()

    override val lastValue: FileReference get() = file

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

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight -> openMenu(windowStack, listOf(
                MenuOption(openInExplorerDesc) { file.openInExplorer() },
                MenuOption(openInStandardProgramDesc) { file.openInStandardProgram() },
                MenuOption(editInStandardProgramDesc) { file.editInStandardProgram() },
            ) + extraRightClickOptions.map {
                MenuOption(it.nameDesc) { it.onClick(this, file) }
            })
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun setIsSelectedListener(listener: () -> Unit): FileInput {
        base.setIsSelectedListener(listener)
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        if (files.size == 1) {
            setValue(files[0], true)
        } else {
            LOGGER.warn("Can only paste a single file!, got $files")
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(FileInput::class)
    }

}