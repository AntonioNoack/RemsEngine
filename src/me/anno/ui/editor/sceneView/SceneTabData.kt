package me.anno.ui.editor.sceneView

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.objects.Transform
import me.anno.studio.history.History
import me.anno.ui.editor.sceneTabs.SceneTab
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.types.Lists.firstInstanceOrNull

class SceneTabData() : Saveable() {

    constructor(tab: SceneTab) : this() {
        file = tab.file
        transform = tab.scene
        history = tab.history
    }

    var file: FileReference? = null
    var transform: Transform? = null
    var history: History? = null

    fun apply(tab: SceneTab) {
        tab.file = file
        val read by lazy { TextReader.read(file!!, true) }
        tab.scene = transform ?: read.firstInstanceOrNull<Transform>() ?: Transform().run {
            // todo translate
            name = "Root"
            comment = "Error loading $file!"
            this
        }
        tab.history = history ?: read.firstInstanceOrNull<History>() ?: tab.history
    }

    override fun save(writer: BaseWriter) {
        writer.writeFile("file", file)
        if (file == null) {// otherwise there isn't really a need to save it
            writer.writeObject(this, "transform", transform)
            writer.writeObject(this, "history", history)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "file" -> file = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "file" -> file = value
            else -> super.readFile(name, value)
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