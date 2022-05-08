package me.anno.ui.base.constraints

import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.ui.Panel

abstract class Constraint(val order: Int) : Saveable() {

    abstract fun apply(panel: Panel)

    open fun clone() = TextReader.readFirst<Constraint>(TextWriter.toText(this, InvalidRef), InvalidRef, false)

}