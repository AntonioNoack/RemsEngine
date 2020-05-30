package me.anno.ui.impl

import me.anno.gpu.GFX
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style

class OptionBar(style: Style): PanelListX(null, style.getChild("options")) {

    init {
        spacing = style.getSize("textSize", 12)/2
    }

    class Major(val name: String, style: Style): TextPanel(name, style){

        val actions = HashMap<String, Minor>()

        init {
            this += WrapAlign.LeftTop
            if(name == "Edit") weight = 1f
        }

        fun addMinor(minor: Minor, id: String){
            actions[id] = minor
        }

        override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
            GFX.openMenu(this.x, this.y + this.h, "", actions.values.map {  minor ->
                minor.name to { b: Int, l: Boolean ->
                    if(b == 0) minor.action()
                    true
                }
            })
        }

    }

    class Minor(val name: String, val action: () -> Unit)

    fun addMajor(name: String): Major {
        if(!majors.containsKey(name)){
            val major = Major(name, style)
            majors[name] = major
            this += major
        }
        return majors[name]!!
    }

    fun addAction(major: String, minor: String, action: () -> Unit) = addAction(major, minor, minor, action)
    fun addAction(major: String, minor: String, name: String, action: () -> Unit){
        addMajor(major).addMinor(Minor(name, action), minor)
    }

    val majors = HashMap<String, Major>()


}