package me.anno.ui.base.constraints

import me.anno.ui.base.Constraint
import me.anno.ui.base.Panel

class Margin(var left: Int, var top: Int, var right: Int, var bottom: Int): Constraint(20){
    override fun apply(panel: Panel) {
        panel.x += left
        panel.y += top
        panel.w -= left + right
        panel.h -= top + bottom
    }
}