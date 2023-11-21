package me.anno.io.config

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

abstract class ConfigEntry : Saveable() {

    var version = 0
    var id = ""
    var group = ""
    var name = ""
    var description = ""
    var comment = ""

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("version", version)
        writer.writeString("id", id)
        writer.writeString("name", name)
        writer.writeString("description", description)
        writer.writeString("group", group)
        writer.writeString("comment", comment)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "version" -> version = value
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "id" -> id = value
            "name" -> this.name = value
            "description" -> description = value
            "group" -> group = value
            "comment" -> comment = value
            else -> super.readString(name, value)
        }
    }


}