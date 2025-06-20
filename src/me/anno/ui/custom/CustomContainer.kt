package me.anno.ui.custom

import me.anno.fonts.FontStats
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.Color.black
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager

/**
 * Container element, where the child can be selected from a predefined list (library) of panels.
 * */
class CustomContainer(default: Panel, val library: UITypeLibrary, style: Style) :
    PanelContainer(default, Padding(0), style) {

    private val crossSize = getCrossSize(style)

    // todo when dragging on the cross, or maybe left corners as well, split UI there like Blender
    //  but also allow merging

    override fun calculateSize(w: Int, h: Int) {
        child.calculateSize(w, h)
        minW = child.minW
        minH = child.minH
    }

    override fun capturesChildEvents(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return lx1 - x1 < crossSize && y0 - ly0 < crossSize
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val icon0 = TextureCache[crossPath, 360_000L].value
        val icon = icon0 ?: whiteTexture
        val x2 = x + width - (crossSize + 2)
        val y2 = y + 2
        val tint = 0x8f8f8f or black
        drawTexture(x2, y2, crossSize, crossSize, icon, tint, null)
    }

    private fun addBefore(index: Int, parent: CustomList) {
        parent.add(index, CustomContainer(library.createDefault(), library, style))
    }

    private fun addAfter(index: Int, parent: CustomList) {
        parent.add(index + 1, CustomContainer(library.createDefault(), library, style))
    }

    private fun replace(index: Int, parent: CustomList, isY: Boolean, firstThis: Boolean) {
        val children = parent.children
        val replaced = CustomList(isY, style)
        replaced.parent = parent
        children[index] = replaced
        replaced.weight = this.weight
        val newInstance = CustomContainer(library.createDefault(), library, style)
        if (firstThis) {
            replaced.add(this)
            replaced.add(newInstance)
        } else {
            replaced.add(newInstance)
            replaced.add(this)
        }
    }

    fun addPanel(isYAction: Boolean, firstThis: Boolean) {
        val parent = parent!!
        val index = indexInParent
        parent as CustomList
        if (isYAction == parent.isY) {
            if (firstThis) {
                addAfter(index, parent)
            } else {
                addBefore(index, parent)
            }
        } else replace(index, parent, isYAction, firstThis)
    }

    private fun changeType() {
        val options = ArrayList<MenuOption>(library.typeList.size + 5)
        for ((nameDesc, generator) in library.typeList) {
            options.add(MenuOption(nameDesc) { changeTo(generator()) })
        }
        options.add(Menu.menuSeparator1)
        val parent = parent
        val warningForLast = "Cannot remove root of custom UI hierarchy"
        options.add(
            MenuOption(NameDesc("Remove This Element", "", "ui.customize.remove")) {
                if (parent is CustomList) {
                    parent.remove(indexInParent)
                } else LOGGER.warn(warningForLast)
            }.setEnabled(parent is CustomList && (siblings.size > 1 || parent.parent is CustomList), warningForLast)
        )
        options.add(Menu.menuSeparator1)
        options.add(
            MenuOption(
                NameDesc(
                    "Add Panel Left",
                    "Adds a new panel to the left of this one",
                    "ui.customize.addBefore"
                )
            ) { addPanel(false, firstThis = false) })
        options.add(
            MenuOption(
                NameDesc(
                    "Add Panel Right",
                    "Adds a new panel to the right of this one",
                    "ui.customize.addAfter"
                )
            ) { addPanel(false, firstThis = true) })
        options.add(
            MenuOption(
                NameDesc(
                    "Add Panel Above",
                    "Adds a new panel to the top of this one",
                    "ui.customize.addAbove"
                )
            ) { addPanel(true, firstThis = false) })
        options.add(
            MenuOption(
                NameDesc(
                    "Add Panel Below",
                    "Adds a new panel to the bottom of this one",
                    "ui.customize.addBelow"
                )
            ) { addPanel(true, firstThis = true) })
        openMenu(windowStack, x + width - 16, y, NameDesc("Customize UI", "", "ui.customize.title"), options)
    }

    private fun changeTo(panel: Panel) {
        child = panel
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        val prefix = "ChangeType("
        if (action.startsWith(prefix)) {
            val typeName = action.substring(prefix.length, action.lastIndex)
            val type = library.getType(typeName)
            if (type != null) {
                changeTo(type.generator())
            } else {
                changeType()
            }
        } else when (action) {
            "ChangeType" -> changeType()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        clicked(x, y)
    }

    fun clicked(x: Float, y: Float): Boolean {
        return if (isCursorOnCross(x, y, crossSize)) {
            changeType()
            true
        } else false
    }

    fun getCrossSize(style: Style): Int {
        val fontSize = style.getSize("text.fontSize", FontStats.getDefaultFontSize())
        return style.getSize("customizable.crossSize", fontSize)
    }

    fun isCursorOnCross(x: Float, y: Float, crossSize: Int): Boolean {
        val crossSize1 = crossSize + 4f // +4f for 2*padding
        return x - (this.x + width - crossSize1) in 0f..crossSize1 && y - this.y in 0f..crossSize1
    }

    companion object {
        private val crossPath = res.getChild("textures/Cross.png")
        private val LOGGER = LogManager.getLogger(CustomContainer::class)
    }
}