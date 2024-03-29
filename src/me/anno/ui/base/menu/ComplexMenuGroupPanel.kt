package me.anno.ui.base.menu

import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.DrawTexts
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.text.TextPanel
import java.util.WeakHashMap

/**
 * Panel for ComplexMenuGroup;
 * acceptably good solution, hover-ability would be better
 * */
class ComplexMenuGroupPanel(val data: ComplexMenuGroup, val magicIndex: Int, val close: () -> Unit, style: Style) :
    TextPanel(data.title, style) {

    init {
        addLeftClickListener {
            openMenu()
        }
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
            ) {
                entry.action()
                close()
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        DrawTexts.drawText( // draw arrow right-aligned
            x + width - padding.right, y, font, TextCacheKey("→", font), textColor, backgroundColor,
            AxisAlignment.MAX, AxisAlignment.MIN
        )
        if (magicIndex in text.indices) {
            underline(magicIndex, magicIndex + 1)
        }
    }

    companion object {
        private val openedBy = WeakHashMap<PanelGroup, ComplexMenuGroupPanel>()
    }
}