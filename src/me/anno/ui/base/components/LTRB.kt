package me.anno.ui.base.components

import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter

open class LTRB(var left: Int, var top: Int, var right: Int, var bottom: Int) : Saveable() {

    val width: Int get() = left + right
    val height: Int get() = top + bottom

    fun set(all: Int) {
        left = all
        top = all
        right = all
        bottom = all
    }

    fun set(other: LTRB) {
        left = other.left
        top = other.top
        right = other.right
        bottom = other.bottom
    }

    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun add(all: Int) = add(all, all, all, all)

    fun add(left: Int, top: Int, right: Int, bottom: Int) {
        this.left += left
        this.top += top
        this.right += right
        this.bottom += bottom
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("left", left)
        writer.writeInt("right", right)
        writer.writeInt("top", top)
        writer.writeInt("bottom", bottom)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "left" -> left = value as? Int ?: return
            "right" -> right = value as? Int ?: return
            "top" -> top = value as? Int ?: return
            "bottom" -> bottom = value as? Int ?: return
            else -> super.setProperty(name, value)
        }
    }
}