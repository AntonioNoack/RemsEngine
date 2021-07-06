package me.anno.scripting.visual

import me.anno.io.Saveable

abstract class NamedVisual : Saveable() {

    var name: String = ""
    var description: String = ""
    var color: Int = 0

    override val approxSize get() = 100
    override fun isDefaultValue(): Boolean = false

}