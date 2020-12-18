package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.Transform
import me.anno.studio.RemsStudio.project
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.history.History
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.sceneTabs.SceneTabs.currentTab
import me.anno.ui.editor.sceneTabs.SceneTabs.open
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.Maths.mixARGB
import java.io.File
import kotlin.concurrent.thread

class SceneTab(var file: File?, var root: Transform, history: History?) : TextPanel("", DefaultConfig.style) {

    companion object {
        val maxNameLength = 15
    }

    var history = history ?: try {
        TextReader.fromText(file!!.readText()).filterIsInstance<History>().first()
    } catch (e: java.lang.Exception) {
        History()
    }

    private val longName get() = file?.name ?: root.name
    private val shortName
        get() = longName.run {
            if (length > maxNameLength) {
                substring(0, maxNameLength - 3) + "..."
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
                        GFX.openMenu(listOf(
                            "Close" to { save { close() } },
                            "Close (Unsaved)" to { close() }
                        ))
                    } else {
                        GFX.openMenu(listOf(
                            "Close" to { close() }
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

    fun save(onSuccess: () -> Unit) {
        fun save(dst: File) {
            if (dst.isDirectory) dst.deleteRecursively()
            thread {
                try {
                    synchronized(root) {
                        dst.parentFile.mkdirs()
                        dst.writeText(TextWriter.toText(root, false))
                        file = dst
                        hasChanged = false
                        onSuccess()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (file == null) {
            var name = root.name.trim()
            if (!name.endsWith(".json", true)) name = "$name.json"
            val name0 = name
            // todo replace /,\?,..
            name = name.toAllowedFilename() ?: ""
            if (name.isEmpty()) {
                val dst = File(project!!.scenes, name)
                if (dst.exists()) {
                    GFX.ask("Override ${dst.name}?") {
                        file = dst
                        save(dst)
                    }
                } else {
                    file = dst
                    save(dst)
                    rootPanel.listOfAll
                        .filterIsInstance<FileExplorer>()
                        .forEach { it.invalidate() }
                }
            } else {
                GFX.msg("'$name0' is no valid file name, rename it!")
            }
        } else {
            save(file!!)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                if (dragged?.getOriginal() != this) {
                    dragged = Draggable(SceneTabData(this).toString(), "SceneTab", this, TextPanel(shortName, style))
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
