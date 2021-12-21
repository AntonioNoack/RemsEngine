package me.anno.ui.editor.treeView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.midGray
import me.anno.config.DefaultStyle.white
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.input.Input.mouseDownX
import me.anno.input.Input.mouseDownY
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.sq
import kotlin.math.roundToInt

class TreeViewPanel<V>(
    val getElement: () -> V,
    val getName: (V) -> String,
    val setName: (V, String) -> Unit,
    val openAddMenu: (parent: V) -> Unit,
    val fileContentImporter: FileContentImporter<V>,
    showSymbol: Boolean,
    val treeView: TreeView<V>, style: Style
) : PanelListX(style) {

    val accentColor = style.getColor("accentColor", black or 0xff0000)

    val uiSymbol: TextPanel? = if (showSymbol) {
        object : TextPanel("", style) {

            init {
                textAlignment = AxisAlignment.CENTER
            }

            override fun calculateSize(w: Int, h: Int) {
                val font = font
                minW = font.sampleWidth * 2 + padding.width
                minH = font.sampleHeight + padding.height
                if (text != textCacheKey.text || font.isBold != textCacheKey.isBold() || font.isItalic != textCacheKey.isItalic()) {
                    textCacheKey = TextCacheKey(text, font, -1, -1)
                }
            }

            override fun onCopyRequested(x: Float, y: Float) =
                this@TreeViewPanel.onCopyRequested(x, y)
        }
    } else null

    val text = object : TextPanel("", style) {
        override fun onCopyRequested(x: Float, y: Float) =
            this@TreeViewPanel.onCopyRequested(x, y)
    }

    init {
        if (uiSymbol != null) {
            uiSymbol.enableHoverColor = true
            this += uiSymbol
        }
        text.enableHoverColor = true
        this += text
    }

    fun setText(symbol: String, name: String) {
        this.uiSymbol?.text = symbol
        this.text.text = name
    }

    var showAddIndex: Int? = null

    var textColor
        get() = (uiSymbol ?: text).textColor
        set(value) {
            uiSymbol?.textColor = value
            text.textColor = value
            text.focusTextColor = value
        }

    // override val effectiveTextColor: Int get() = textColor
    val hoverColor get() = (uiSymbol ?: text).hoverColor
    val font get() = (uiSymbol ?: text).font

    fun Int.scaleRGB(f: Float): Int {
        return rgba((r() * f).roundToInt(), (g() * f).roundToInt(), (b() * f).roundToInt(), a())
    }

    override fun tickUpdate() {
        super.tickUpdate()
        val transform = getElement()
        val dragged = dragged
        var backgroundColor = originalBGColor
        val showAddIndex = if (
            mouseX.toInt() in lx0..lx1 &&
            mouseY.toInt() in ly0..ly1 &&
            dragged is Draggable && dragged.getOriginal() is Transform
        ) {
            clamp(((mouseY - this.y) / this.h * 3).toInt(), 0, 2)
        } else null
        if (this.showAddIndex != showAddIndex) invalidateDrawing()
        this.showAddIndex = showAddIndex
        val isInFocus = isInFocus || text.isInFocus || (uiSymbol?.isInFocus == true) || selectedTransform == transform
        val textColor = treeView.getLocalColor(transform, isHovered, isInFocus).scaleRGB(180 / 255f)
        val colorDifference = sq(textColor.r() - backgroundColor.r()) +
                sq(textColor.g() - backgroundColor.g()) +
                sq(textColor.b() - backgroundColor.b())
        backgroundColor = if (colorDifference < 512) {// too similar colors
            if (textColor.g() > 127) black
            else white
        } else originalBGColor
        this.textColor = textColor
        text.backgroundColor = backgroundColor
        uiSymbol?.backgroundColor = backgroundColor
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // draw the paste-preview
        val showAddIndex = showAddIndex
        if (showAddIndex != null) {
            val x = x + padding.left
            val textSize = font.sizeInt
            val indent = textSize + 0
            val lineWidth = textSize * 7
            val lineColor = midGray
            when (showAddIndex) {
                0 -> drawRect(x, y, lineWidth, 1, lineColor)
                1 -> drawRect(x + indent, y + h - 1, lineWidth, 1, lineColor)
                2 -> drawRect(x, y + h - 1, lineWidth, 1, lineColor)
            }
        }
    }

    private fun toggleCollapsed() {
        val element = getElement()
        val name = treeView.getName(element)
        val isCollapsed = treeView.isCollapsed(element)
        // todo obviously, this RemsStudio must be removed
        // todo and instead we need to use the general history/...
        RemsStudio.largeChange(if (isCollapsed) "Expanded $name" else "Collapsed $name") {
            val target = !isCollapsed
            // remove children from the selection???...
            val targets = inFocus.filterIsInstance<TreeViewPanel<*>>()
            for (it in targets) {
                val element2 = it.getElement() as V
                treeView.setCollapsed(element2, target)
            }
            if (targets.isEmpty()) {
                treeView.setCollapsed(element, target)
            }
        }
    }

    private fun isMouseOnSymbol(x: Float): Boolean {
        return uiSymbol != null && x <= uiSymbol.lx1
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {

        val element = getElement()
        when {
            button.isLeft -> {

                // todo collapse, if you click on the symbol

                // todo collapse / expand multiple elements at the same time
                // todo edit multiple elements at the same time
                if ((Input.isShiftDown && inFocus.size < 2) || isMouseOnSymbol(x)) {
                    toggleCollapsed()
                } else {
                    treeView.selectElementMaybe(element)
                }
            }
            button.isRight -> openAddMenu(element)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        when {
            button.isLeft -> treeView.focusOnElement(getElement())
            // button.isRight -> toggleCollapsed()
            else -> super.onDoubleClick(x, y, button)
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String {
        return treeView.stringifyForCopy(getElement())
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        try {
            val child0 = TextReader.read(data).firstOrNull()
            val child = child0 as? V ?: return super.onPaste(x, y, data, type)
            val original = (dragged as? Draggable)?.getOriginal() as? V
            val relativeY = (y - this.y) / this.h
            val element = getElement()
            RemsStudio.largeChange("Moved Component") {
                if (relativeY < 0.33f) {
                    // paste on top
                    if (element.parent != null) {
                        treeView.addBefore(element, child)
                    } else {
                        treeView.addChild(element, child)
                        // element.addChild(child)
                    }
                    // we can't remove the element, if it's the parent
                    if (original !in child.listOfAll) {
                        original?.removeFromParent()
                    }
                } else if (relativeY < 0.67f) {
                    // paste as child
                    treeView.addChild(element, child)
                    // element.addChild(child)
                    if (element != original) {
                        // we can't remove the element, if it's the parent
                        if (original !in child.listOfAll) {
                            original?.removeFromParent()
                        }
                    }
                } else {
                    // paste below
                    if (element.parent != null) {
                        treeView.addAfter(element, child)
                    } else {
                        treeView.addChild(element, child)
                        // element.addChild(child)
                    }
                    // we can't remove the element, if it's the parent
                    if (original !in child.listOfAll) {
                        original?.removeFromParent()
                    }
                }
                treeView.selectElement(child)
                // selectTransform(child)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    val V.parent: V? get() = treeView.getParent(this)

    val V.listOfAll: Sequence<V>
        get() {
            return sequence {
                yield(this@listOfAll)
                treeView.getChildren(this@listOfAll).forEach { yieldAll(it.listOfAll) }
            }
        }

    fun V.removeFromParent() {
        treeView.removeChild(treeView.getParent(this) ?: return, this)
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val transform = getElement()
        for (it in files) {
            fileContentImporter.addChildFromFile(transform, it, FileContentImporter.SoftLinkMode.ASK, true) {}
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                if (contains(mouseDownX, mouseDownY)) {
                    val element = getElement()
                    if (dragged?.getOriginal() != element) {
                        dragged = Draggable(
                            treeView.stringifyForCopy(element), treeView.getDragType(element), element,
                            TextPanel(treeView.getName(element), style)
                        )
                    }
                }
            }
            "Rename" -> {
                val e = getElement()
                askName(windowStack, x.toInt(), y.toInt(), NameDesc("Name"), getName(e), getColor = { -1 }, callback = { newName ->
                    setName(e, newName)
                }, actionName = NameDesc("Change Name"))
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onEmpty(x: Float, y: Float) {
        onDeleteKey(x, y)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        val element = getElement()
        val parent = treeView.getParent(element)
        if (parent != null) {
            // todo only in Rem's Studio...
            RemsStudio.largeChange("Deleted Component ${treeView.getName(getElement())}") {
                treeView.removeChild(parent, element)
                for (it in element.listOfAll.toList()) treeView.destroy(it)
            }
        }
    }

    override fun onBackSpaceKey(x: Float, y: Float) = onDeleteKey(x, y)
    override fun getCursor() = Cursor.drag

    override fun getTooltipText(x: Float, y: Float): String? {
        return if (visibility == Visibility.VISIBLE) treeView.getTooltipText(getElement())
        else null
    }

    // multiple values can be selected
    override fun getMultiSelectablePanel() = this

    override val className get() = "TreeViewPanel"

}