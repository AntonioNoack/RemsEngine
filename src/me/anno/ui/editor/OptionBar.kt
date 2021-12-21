package me.anno.ui.editor

import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style

class OptionBar(style: Style) : PanelListX(null, style.getChild("options")) {

    init {
        spacing = style.getSize("fontSize", 12) / 2
    }

    class Major(name: String, val action: (() -> Unit)?, style: Style) : TextPanel(name, style) {

        init {
            this.name = name
        }

        val actions = HashMap<String, Minor>()
        val actionList = ArrayList<Pair<String, Minor>>()

        init {
            this += WrapAlign.LeftTop
            if (name == "Edit") weight = 1f
        }

        fun addMinor(minor: Minor, id: String) {
            if (actions.containsKey(id)) {
                actions[id] = minor
                actionList[actionList.indexOfFirst { it.first == id }] = id to minor
            } else {
                actions[id] = minor
                actionList += id to minor
            }
        }

        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            action?.invoke()
            openMenu(windowStack, this.x, this.y + this.h, NameDesc(), actionList.map { (_, minor) ->
                MenuOption(NameDesc(minor.name, "", "")) { minor.action() }
            })
        }

    }

    class Minor(val name: String, val action: () -> Unit)

    fun addMajor(name: String, action: (() -> Unit)?): Major {
        if (!majors.containsKey(name)) {
            val major = Major(name, action, style)
            majors[name] = major
            this += major
        }
        return majors[name]!!
    }

    fun addAction(major: String, minor: String, action: () -> Unit) = addAction(major, minor, minor, action)
    fun addAction(major: String, minor: String, name: String, action: () -> Unit) {
        addMajor(major, null).addMinor(Minor(name, action), minor)
    }

    val majors = HashMap<String, Major>()

}