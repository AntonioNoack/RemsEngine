package me.anno.ui.editor

import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.hasFlag
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

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.onDraw(x0, y0, x1, y1)
            if (magicIndex in text.indices) {
                underline(magicIndex, magicIndex + 1)
            }
        }

        val actions = HashMap<String, Minor>()
        val actionList = ArrayList<Pair<String, Minor>>()
        var magicIndex = -1

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

        fun open() {
            action?.invoke()
            openMenu(windowStack, this.x, this.y + this.height, NameDesc(), actionList.map { (_, minor) ->
                MenuOption(NameDesc(minor.name, "", "")) { minor.action() }
            })
        }

        override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
            open()
        }
    }

    class Minor(val name: String, val action: () -> Unit)

    fun addMajor(name: String, action: (() -> Unit)?): Major {
        if (!majors.containsKey(name)) {
            val major = Major(name, action, style)
            majors[name] = major
            val magicIndex = addFastActionLetters(name)
            major.magicIndex = magicIndex
            if (magicIndex >= 0) {
                val char = name[magicIndex].lowercaseChar()
                eventHandlers[char] = major::open
            }
            this += major
        }
        return majors[name]!!
    }

    fun addAction(major: String, minor: String, action: () -> Unit) = addAction(major, minor, minor, action)
    fun addAction(major: String, minor: String, name: String, action: () -> Unit) {
        addMajor(major, null).addMinor(Minor(name, action), minor)
    }

    // like Menu
    val eventHandlers = HashMap<Char, () -> Unit>()
    var usedLetters = 0
    fun addFastActionLetters(name: String): Int {
        for (i in name.indices) {
            val letter = name[i]
            val lcLetter = letter.lowercaseChar()
            if (lcLetter in 'a'..'z') {
                val mask = 1 shl (lcLetter.code - 'a'.code)
                if (!usedLetters.hasFlag(mask)) {
                    usedLetters = usedLetters or mask
                    return i
                }
            }
        }
        return -1
    }

    override fun onUpdate() {
        super.onUpdate()
        val window = window
        // if this window is in focus, and alt is pressed,
        // look for all keys that just went down, and based on them, decide, which menu to open
        if (window != null && window.windowStack.peek() == window &&
            GFX.activeWindow?.windowStack == windowStack &&
            Input.isKeyDown(Key.KEY_LEFT_ALT)
        ) {
            for (key in Input.keysWentDown) {
                eventHandlers['a' + (key.id - Key.KEY_A.id)]?.invoke()
            }
        }
    }

    val majors = HashMap<String, Major>()
}