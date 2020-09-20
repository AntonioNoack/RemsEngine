package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.objects.Transform
import me.anno.studio.Studio
import me.anno.ui.base.TextPanel
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.ui.editor.sceneTabs.SceneTabs.currentTab
import me.anno.ui.editor.sceneTabs.SceneTabs.open
import me.anno.utils.mixARGB
import java.io.File
import kotlin.concurrent.thread

class SceneTab(var file: File?, var root: Transform) : TextPanel(file?.name ?: root.name, DefaultConfig.style) {

    var hasChanged = false
        set(value) {
            val baseName = file?.name ?: root.name
            text = if (field) "$baseName*" else baseName
            field = value
        }

    init {
        padding.top --
        padding.bottom --
        setOnClickListener { _, _, button, _ ->
            when {
                button.isLeft -> {
                    open(this)
                }
                button.isRight -> {
                    if(hasChanged){
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
                    synchronized(root){
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
                val dst = File(Studio.project!!.scenes, name)
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

}
