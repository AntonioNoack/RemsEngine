package me.anno.ui.base.constraints

import me.anno.io.Saveable
import me.anno.ui.Panel

abstract class Constraint(val order: Int) : Saveable() {

    abstract fun apply(panel: Panel)

    abstract fun clone(): Constraint

}