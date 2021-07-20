package me.anno.scripting.visual

import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter

abstract class NamedVisual : NamedSaveable() {

    var color: Int = 0

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("color", color)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "color" -> color = value
            else -> super.readInt(name, value)
        }
    }

    override val approxSize get() = 100
    override fun isDefaultValue(): Boolean = false

}