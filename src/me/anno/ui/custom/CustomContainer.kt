package me.anno.ui.custom

import me.anno.config.DefaultConfig
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache
import me.anno.input.Key
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.Style
import me.anno.utils.Color.white
import org.apache.logging.log4j.LogManager
import kotlin.math.roundToInt

/**
 * Container element, where the child can be selected from a predefined list (library) of panels.
 * */
class CustomContainer(default: Panel, val library: UITypeLibrary, style: Style) :
    PanelContainer(default, Padding(0), style) {

    // todo when dragging on the cross, or maybe left corners as well, split UI there like Blender
    //  but also allow merging

    init {
        if (default is CustomContainer) LOGGER.warn(
            "You added a custom container to a custom container." +
                    " You probably made a mistake!"
        )
    }

    override fun calculateSize(w: Int, h: Int) {
        child.calculateSize(w, h)
        minW = child.minW
        minH = child.minH
        this.width = child.width
        this.height = child.height
    }

    override fun setPosition(x: Int, y: Int) {
        child.setPosition(x, y)
        this.x = x
        this.y = y
    }

    override fun invalidateLayout() {
        window?.addNeedsLayout(this)
    }

    override fun capturesChildEvents(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        val crossSize = getCrossSize(style)
        return this.lx1 - lx1 < crossSize && ly0 - this.ly0 < crossSize
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val icon = ImageGPUCache[crossPath, 360_000L, false] ?: whiteTexture
        val crossSize = getCrossSize(style).roundToInt()
        drawTexture(x + width - (crossSize + 2), y + 2, crossSize, crossSize, icon, white, null)
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
        for (it in library.typeList) {
            options.add(MenuOption(NameDesc(it.displayName, "", "")) { changeTo(it.constructor()) })
        }
        val parent = parent
        val warningForLast = "Cannot remove root of custom UI hierarchy"
        options += MenuOption(NameDesc("Remove This Element", "", "ui.customize.remove")) {
            if (parent is CustomList) {
                parent.remove(indexInParent)
            } else LOGGER.warn(warningForLast)
        }.setEnabled(parent is CustomList && (siblings.size > 1 || parent.parent is CustomList), warningForLast)
        options += MenuOption(NameDesc("Add Panel Before", "", "ui.customize.addBefore")) {
            addPanel(false, firstThis = false)
        }
        options += MenuOption(NameDesc("Add Panel After", "", "ui.customize.addAfter")) {
            addPanel(false, firstThis = true)
        }
        options += MenuOption(NameDesc("Add Panel Above", "", "ui.customize.addAbove")) {
            addPanel(true, firstThis = false)
        }
        options += MenuOption(NameDesc("Add Panel Below", "", "ui.customize.addBelow")) {
            addPanel(true, firstThis = false)
        }
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
                changeTo(type.constructor())
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
        return if (isCross(x, y)) {
            changeType()
            true
        } else false
    }

    override val className: String get() = "CustomContainer"

    companion object {

        val crossPath = getReference("res://textures/Cross.png")

        private val LOGGER = LogManager.getLogger(CustomContainer::class)

        fun getCrossSize(style: Style): Float {
            val fontSize = style.getSize("text.fontSize", DefaultConfig.defaultFont.size.toInt())
            return style.getSize("customizable.crossSize", fontSize).toFloat()
        }

        fun Panel.isCross(x: Float, y: Float): Boolean {
            val crossSize = getCrossSize(style) + 4f // +4f for 2*padding
            return x - (this.x + width - crossSize) in 0f..crossSize && y - this.y in 0f..crossSize
        }
    }
}