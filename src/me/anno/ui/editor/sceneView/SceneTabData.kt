package me.anno.ui.editor.sceneView

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextReader
import me.anno.objects.Transform
import me.anno.ui.editor.sceneTabs.SceneTab
import java.io.File

class SceneTabData() : Saveable() {

    constructor(tab: SceneTab): this(){
        file = tab.file
        transform = tab.root
    }

    var file: File? = null
    var transform: Transform? = null

    fun apply(tab: SceneTab) {
        tab.file = file
        tab.root = transform ?: TextReader.fromText(file!!.readText())
            .filterIsInstance<Transform>()
            .first() ?: Transform().run {
            name = "Root"
            comment = "Error loading $file!"
            this
        }
    }

    override fun save(writer: BaseWriter) {
        writer.writeFile("file", file)
        if (file == null) {// otherwise there isn't really a need to save it
            writer.writeObject(this, "transform", transform)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "transform" -> {
                transform = value as? Transform
            }
            else -> super.readObject(name, value)
        }
    }

    override fun isDefaultValue() = false
    override fun getClassName() = "SceneTabData"
    override fun getApproxSize() = 1_000_000

}