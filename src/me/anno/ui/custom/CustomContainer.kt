package me.anno.ui.custom

import me.anno.config.DefaultStyle.white
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.sceneView.SceneView
import me.anno.ui.style.Style

class CustomContainer(default: Panel, val library: UITypeLibrary, style: Style) :
    PanelContainer(default, Padding(0), style) {

    init {
        if (default is CustomContainer) me.anno.utils.LOGGER.warn("You added a custom container to a custom container." +
                " You probably made a mistake!")
    }

    override fun calculateSize(w: Int, h: Int) {
        child.calculateSize(w, h)
        minW = child.minW
        minH = child.minH
    }

    override fun placeInParent(x: Int, y: Int) {
        child.placeInParent(x, y)
        this.x = x
        this.y = y
    }

    override fun invalidateLayout() {
        window!!.needsLayout += this
    }

    override fun drawsOverlaysOverChildren(lx0: Int, ly0: Int, lx1: Int, ly1: Int): Boolean {
        return this.lx1 - lx1 < customContainerCrossSize && ly0 - this.ly0 < customContainerCrossSize
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val icon = getInternalTexture("cross.png", true) ?: whiteTexture
        drawTexture(x + w - 14, y + 2, 12, 12, icon, white, null)
    }

    private fun addBefore(index: Int, parent: CustomList) {
        val children = parent.children
        val view = CustomContainer(SceneView(style), library, style)
        val bar = CustomizingBar(0, 3, 0, style)
        bar.parent = parent
        view.parent = parent
        children.add(index + 0, view)
        children.add(index + 1, bar)
        view.weight = 1f
        parent.update()
    }

    private fun addAfter(index: Int, parent: CustomList) {
        val children = parent.children
        val view = CustomContainer(SceneView(style), library, style)
        val bar = CustomizingBar(0, 3, 0, style)
        bar.parent = parent
        view.parent = parent
        children.add(index + 1, bar)
        children.add(index + 2, view)
        view.weight = 1f
        parent.update()
    }

    private fun replace(index: Int, parent: CustomList, isY: Boolean, firstThis: Boolean) {
        val children = parent.children
        val replaced = CustomList(isY, style)
        replaced.parent = parent
        children[index] = replaced
        replaced.weight = this.weight
        if (firstThis) {
            replaced.add(this)
            replaced.add(CustomContainer(SceneView(style), library, style))
        } else {
            replaced.add(CustomContainer(SceneView(style), library, style))
            replaced.add(this)
        }
        parent.update()
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
        fun action(action: () -> Panel): () -> Unit = { changeTo(action()) }
        val options = library.typeList
            .map { MenuOption(NameDesc(it.displayName, "", ""), action { it.constructor() }) }
            .toMutableList()
        options += MenuOption(NameDesc("Remove This Element", "", "ui.customize.remove")) {
            (parent as? CustomList)?.apply {
                remove(indexInParent)
            }
            Unit
        }
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
        openMenu(x + w - 16, y, NameDesc("Customize UI", "", "ui.customize.title"), options)
    }

    private fun changeTo(panel: Panel) {
        child = panel
        child.parent = this
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "ChangeType" -> changeType()
            "ChangeType(SceneView)" -> changeTo(SceneView(style))
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        clicked(x, y)
    }

    fun clicked(x: Float, y: Float): Boolean {
        return if (isCross(x, y)) {
            changeType()
            true
        } else false
    }

    companion object {
        val customContainerCrossSize = 16f
        fun Panel.isCross(x: Float, y: Float) =
            x - (this.x + w - 16f) in 0f..customContainerCrossSize && y - this.y in 0f..customContainerCrossSize
    }

}