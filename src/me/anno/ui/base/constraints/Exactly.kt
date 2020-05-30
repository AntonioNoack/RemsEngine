package me.anno.ui.base.constraints

import me.anno.ui.base.Constraint
import me.anno.ui.base.Panel

class Exactly(val w: Int?, val h: Int?): Constraint(1){
    override fun apply(panel: Panel) {
        if(w != null){
            panel.minW = w
            panel.w = w
        }
        if(h != null){
            panel.minH = h
            panel.h = h
        }
    }
}