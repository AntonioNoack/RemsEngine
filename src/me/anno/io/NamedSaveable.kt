package me.anno.io

import me.anno.io.base.BaseWriter

abstract class NamedSaveable : Saveable() {

    open var name = ""
    open var description = ""

    override fun isDefaultValue(): Boolean = false
    override val approxSize: Int = 10

    override fun save(writer: BaseWriter) {
        super.save(writer)
        if (name.isNotEmpty())
            writer.writeString("name", name)
        if (description.isNotEmpty())
            writer.writeString("desc", description)
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "name" -> this.name = value ?: ""
            "desc", "description" -> this.description = value ?: ""
            else -> super.readString(name, value)
        }
    }

}