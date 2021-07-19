package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile

class ChangeAddEntity(var source: FileReference) : Change(0) {

    constructor() : this(InvalidRef)

    // doesn't need the name...

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("source", source)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "source" -> source = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun applyChange(element: Any?, name: String?) {
        element as Entity
        if (source == InvalidRef) {
            element.add(Entity())
        } else {
            TODO("create entity from $source")
        }
    }

    override val className: String = "ChangeAddEntity"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
