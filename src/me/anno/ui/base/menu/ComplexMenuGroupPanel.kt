package me.anno.ui.base.menu

import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSizeCharByChar
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.text.TextPanel
import me.anno.utils.types.Booleans.toInt
import java.util.WeakHashMap

/**
 * Panel for ComplexMenuGroup;
 * acceptably good solution, hover-ability would be better
 * */
class ComplexMenuGroupPanel(val data: ComplexMenuGroup, val magicIndex: Int, val close: () -> Unit, style: Style) :
    TextPanel(data.nameDesc, style) {

    init {
        addLeftClickListener {
            openMenu()
        }
    }

    fun openMenu() {
        // choose centered y
        val shownEntries = data.children.size + Menu.needsSearch(data.children.size).toInt()
        val estimatedChildSize = shownEntries * font.sizeInt
        val self = uiParent ?: this
        val ownSize = self.height
        val centeredY = clamp(
            self.y + (ownSize - estimatedChildSize).shr(1),
            y - estimatedChildSize + height, y
        )
        Menu.openComplexMenu(
            windowStack, lx1, centeredY,
            NameDesc(""),
            data.children.map { entryWithCloseListener(it) }
        )
    }

    fun entryWithCloseListener(entry: ComplexMenuEntry): ComplexMenuEntry {
        return when (entry) {
            is ComplexMenuGroup -> ComplexMenuGroup(
                entry.nameDesc, entry.isEnabled, entry.children.map {
                    entryWithCloseListener(it)
                })
            is ComplexMenuOption -> ComplexMenuOption(
                entry.nameDesc, entry.isEnabled
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

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minW += getSizeX(getTextSizeCharByChar(font, " →", true))
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