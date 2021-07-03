package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.input.ActionManager
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.history.History
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.menu.Menu.ask
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.sceneTabs.SceneTabs.currentTab
import me.anno.ui.editor.sceneTabs.SceneTabs.open
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.Maths.mixARGB
import me.anno.utils.Threads.threadWithName
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference

class SceneTab(var file: FileReference?, var root: Transform, history: History?) : TextPanel("", DefaultConfig.style) {

    companion object {
        const val maxDisplayNameLength = 15
    }

    var history = history ?: try {
        TextReader.fromText(file!!.readText()).filterIsInstance<History>().first()
    } catch (e: java.lang.Exception) {
        History()
    }

    private val longName get() = file?.name ?: root.name
    private val shortName
        get() = longName.run {
            if (length > maxDisplayNameLength) {
                substring(0, maxDisplayNameLength - 3) + "..."
            } else this
        }

    init {
        text = shortName
        tooltip = longName
    }

    var hasChanged = false
        set(value) {
            val baseName = shortName
            val newText = if (value) "$baseName*" else baseName
            text = newText
            tooltip = longName
            field = value
        }

    init {
        padding.top--
        padding.bottom--
        setOnClickListener { _, _, button, _ ->
            when {
                button.isLeft -> {
                    open(this)
                }
                button.isRight -> {
                    if (hasChanged) {
                        openMenu(listOf(
                            MenuOption(NameDesc("Close", "", "ui.sceneTab.closeSaved")) { save { close() } },
                            MenuOption(NameDesc("Close (Unsaved)", "", "ui.sceneTab.closeUnsaved")) { close() }
                        ))
                    } else {
                        openMenu(listOf(
                            MenuOption(NameDesc("Close", "", "ui.sceneTab.close")) { close() }
                        ))
                    }
                }
            }
        }
    }

    fun close() = SceneTabs.close(this)

    private val bg = backgroundColor
    private val bgLight = mixARGB(bg, 0xff777777.toInt(), 0.5f)
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        backgroundColor = if (this === currentTab) bgLight else bg
        super.onDraw(x0, y0, x1, y1)
    }

    fun save(dst: FileReference, onSuccess: () -> Unit) {
        if (dst.isDirectory) dst.deleteRecursively()
        threadWithName("SaveScene") {
            try {
                synchronized(root) {
                    dst.getParent()?.mkdirs()
                    val writer = TextWriter(false)
                    writer.add(root)
                    writer.add(history)
                    writer.writeAllInList()
                    dst.writeText(writer.toString())
                    file = dst
                    hasChanged = false
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        if (file == null) {
            var name = root.name.trim()
            if (!name.endsWith(".json", true)) name = "$name.json"
            val name0 = name
            // todo replace /,\?,..
            name = name.toAllowedFilename() ?: ""
            if (name.isEmpty()) {
                val dst = getReference(project!!.scenes, name)
                if (dst.exists) {
                    // todo translate
                    ask(
                        NameDesc("Override %1?", "Replaces the old file", "ui.file.override")
                            .with("%1", dst.name)
                    ) {
                        file = dst
                        save(file!!, onSuccess)
                    }
                } else {
                    file = dst
                    save(file!!, onSuccess)
                    rootPanel.listOfAll { if(it is FileExplorer) it.invalidate() }
                }
            } else {
                msg(
                    NameDesc("'%1' is no valid file name, rename it!", "", "ui.file.invalidName")
                        .with("%1", name0)
                )
            }
        } else {
            save(file!!, onSuccess)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                if (dragged?.getOriginal() != this) {
                    dragged = Draggable(
                        SceneTabData(this).toString(), "SceneTab", this,
                        TextPanel(shortName, style)
                    )
                }
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        ActionManager.executeGlobally(0f, 0f, false, listOf("DragEnd"))
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        SceneTabs.onPaste(x, y, data, type)
    }

}
