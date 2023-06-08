package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style
import java.util.*

/**
 * Panel for ComplexMenuGroup;
 * acceptably good solution, hover-ability would be better
 * */
class ComplexMenuGroupPanel(val data: ComplexMenuGroup, val close: () -> Unit, style: Style) :
    TextPanel("${data.title} →", style) {

    init {
        addLeftClickListener(::openMenu)
    }

    fun openMenu() {
        Menu.openComplexMenu(
            // todo actual w is too large... why?
            windowStack, lx1, y,
            NameDesc(""),
            data.children.map { entryWithCloseListener(it) }
        )
    }

    fun entryWithCloseListener(entry: ComplexMenuEntry): ComplexMenuEntry {
        return when (entry) {
            is ComplexMenuGroup -> ComplexMenuGroup(
                entry.title, entry.description, entry.isEnabled, entry.children.map {
                    entryWithCloseListener(it)
                })

            is ComplexMenuOption -> ComplexMenuOption(
                entry.title, entry.description, entry.isEnabled
            ) { button, long ->
                val shallClose = entry.action(button, long)
                if (shallClose) close()
                shallClose
            }
        }
    }

    override fun onUpdate() {
        if (isHovered) {
            // open menu
            // todo if hovered over other items of this very list, close the menu (?)
            val prev = openedBy.put(uiParent, this)
            if (prev !== this) {
                // and close lower levels / others
                val window = window
                if (window != null) {
                    val ws = window.windowStack
                    val idx = ws.indexOf(window)
                    if (idx >= 0) while (idx < ws.size - 1) {
                        ws.pop().destroy()
                    }
                    openMenu()
                }
            }
        }
        super.onUpdate()
    }

    companion object {
        private val openedBy = WeakHashMap<PanelGroup, ComplexMenuGroupPanel>()
    }
}