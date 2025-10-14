package me.anno.ui.editor.files

import me.anno.engine.EngineBase
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.OS

class Favourite(var name: String, var file: FileReference) : Saveable() {
    @Suppress("unused")
    constructor() : this("", InvalidRef)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        if (file == EngineBase.workspace && file != OS.documents) {
            writer.writeBoolean("isWorkspace", true)
        } else {
            writer.writeFile("file", file)
        }
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value.toString()
            "file" -> this.file = value as? FileReference ?: return
            "isWorkspace" -> if (value == true) {
                this.file = EngineBase.workspace
            }
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize: Int get() = 1
}