package me.anno.ui.impl

import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style

class OptionBar(style: Style): PanelListX(null, style.getChild("options")) {

    init {
        spacing = style.getSize("textSize", 12)/2
    }

    class Major(val name: String, style: Style): TextPanel(name, style){
        init {
            this += WrapAlign.LeftTop
            if(name == "Edit") weight = 1f
        }
    }

    fun addMajor(name: String){
        if(!majors.containsKey(name)){
            val major = Major(name, style)
            majors[name] = major
            this += major
        }
    }

    val majors = HashMap<String, Major>()


}