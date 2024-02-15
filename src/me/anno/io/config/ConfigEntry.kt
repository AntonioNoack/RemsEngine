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

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "version" -> version = value as? Int ?: return
            "id" -> id = value as? String ?: return
            "name" -> this.name = value as? String ?: return
            "description" -> description = value as? String ?: return
            "group" -> group = value as? String ?: return
            "comment" -> comment = value as? String ?: return
            else -> super.setProperty(name, value)
        }
    }
}