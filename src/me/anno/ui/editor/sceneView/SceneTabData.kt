package me.anno.ui.editor.sceneView

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextReader
import me.anno.objects.Transform
import me.anno.studio.history.History
import me.anno.ui.editor.sceneTabs.SceneTab
import me.anno.io.files.FileReference
import me.anno.utils.files.LocalFile.toGlobalFile

class SceneTabData() : Saveable() {

    constructor(tab: SceneTab) : this() {
        file = tab.file
        transform = tab.root
        history = tab.history
    }

    var file: FileReference? = null
    var transform: Transform? = null
    var history: History? = null

    fun apply(tab: SceneTab) {
        tab.file = file
        val read by lazy { TextReader.read(file!!) }
        tab.root = transform ?: read.filterIsInstance<Transform>().firstOrNull() ?: Transform().run {
            // todo translate
            name = "Root"
            comment = "Error loading $file!"
            this
        }
        tab.history = history ?: read.filterIsInstance<History>().firstOrNull() ?: tab.history
    }

    override fun save(writer: BaseWriter) {
        writer.writeFile("file", file)
        if (file == null) {// otherwise there isn't really a need to save it
            writer.writeObject(this, "transform", transform)
            writer.writeObject(this, "history", history)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "transform" -> transform = value as? Transform
            "history" -> history = value as? History
            else -> super.readObject(name, value)
        }
    }

    override fun isDefaultValue() = false
    override val className get() = "SceneTabData"
    override val approxSize = 1_000_000

}