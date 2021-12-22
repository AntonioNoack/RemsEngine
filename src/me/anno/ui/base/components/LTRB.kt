package me.anno.ui.base.components

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

open class LTRB(var left: Int, var top: Int, var right: Int, var bottom: Int) : Saveable() {

    val width: Int get() = left + right
    val height: Int get() = top + bottom

    override fun readInt(name: String, value: Int) {
        when (name) {
            "left" -> left = value
            "right" -> right = value
            "top" -> top = value
            "bottom" -> bottom = value
            else -> super.readInt(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("left", left)
        writer.writeInt("right", right)
        writer.writeInt("top", top)
        writer.writeInt("bottom", bottom)
    }

    override val className: String = "LTRB"

}