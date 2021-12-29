package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.input.ActionManager
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
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
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.maths.Maths.mixARGB
import org.apache.logging.log4j.LogManager

class SceneTab(var file: FileReference?, var scene: Transform, history: History?) : TextPanel("", DefaultConfig.style) {

    companion object {
        const val maxDisplayNameLength = 15
        private val LOGGER = LogManager.getLogger(SceneTab::class)
    }

    var history = history ?: try {
        TextReader.read(file!!).filterIsInstance<History>().first()
    } catch (e: java.lang.Exception) {
        History()
    }

    private val longName get() = file?.name ?: scene.name
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
        addLeftClickListener { open(this) }
        addRightClickListener {
            if (hasChanged) {
                openMenu(windowStack, listOf(
                    MenuOption(NameDesc("Close", "", "ui.sceneTab.closeSaved")) { save { close() } },
                    MenuOption(NameDesc("Close (Unsaved)", "", "ui.sceneTab.closeUnsaved")) { close() }
                ))
            } else {
                openMenu(windowStack, listOf(
                    MenuOption(NameDesc("Close", "", "ui.sceneTab.close")) { close() }
                ))
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
        LOGGER.info("Saving $dst, ${scene.listOfAll.joinToString { it.name }}")
        threadWithName("SaveScene") {
            try {
                synchronized(scene) {
                    dst.getParent()?.mkdirs()
                    TextWriter.save(listOf(scene, history), dst)
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
            var name = scene.name.trim()
            if (!name.endsWith(".json", true)) name = "$name.json"
            val name0 = name
            // todo replace /,\?,..
            name = name.toAllowedFilename() ?: ""
            if (name.isEmpty()) {
                val dst = getReference(project!!.scenes, name)
                if (dst.exists) {
                    // todo translate
                    ask(
                        windowStack,
                        NameDesc("Override %1?", "Replaces the old file", "ui.file.override")
                            .with("%1", dst.name)
                    ) {
                        file = dst
                        save(file!!, onSuccess)
                    }
                } else {
                    file = dst
                    save(file!!, onSuccess)
                    rootPanel.forAll { if (it is FileExplorer) it.invalidate() }
                }
            } else {
                msg(
                    windowStack,
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
