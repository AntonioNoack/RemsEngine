package me.anno.ui.editor.treeView

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.midGray
import me.anno.config.DefaultStyle.white
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
import me.anno.io.ISaveable
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.language.translation.Dict
import me.anno.objects.Camera
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.style.Style
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toARGB
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.sq
import me.anno.utils.structures.Hierarchical
import org.joml.Vector4f

class AbstractTreeViewPanel<V : Hierarchical<V>>(
    val getElement: () -> V,
    val openAddMenu: (parent: V) -> Unit,
    val fileContentImporter: FileContentImporter<V>,
    val showSymbol: Boolean,
    val castedParent: AbstractTreeView<V>, style: Style
) : PanelListX(style) {

    private val accentColor = style.getColor("accentColor", black or 0xff0000)

    val symbol: TextPanel? = if(showSymbol){
        object : TextPanel("", style) {
            init {
                textAlignment = AxisAlignment.CENTER
            }

            override fun calculateSize(w: Int, h: Int) {
                calculateSize(w, h, "xx")
            }

            override fun onCopyRequested(x: Float, y: Float): String? = parent?.onCopyRequested(x, y)
        }
    } else null

    val text = object : TextPanel("", style) {
        override fun onCopyRequested(x: Float, y: Float): String? = parent?.onCopyRequested(x, y)
    }

    init {
        if(symbol != null){
            symbol.enableHoverColor = true
            this += symbol
        }
        text.enableHoverColor = true
        this += text
    }

    fun setText(symbol: String, name: String) {
        this.symbol?.text = symbol
        this.text.text = name
    }

    var showAddIndex: Int? = null

    var textColor
        get() = (symbol ?: text).textColor
        set(value) {
            symbol?.textColor = value
            text.textColor = value
        }

    // override val effectiveTextColor: Int get() = textColor
    val hoverColor get() = (symbol ?: text).hoverColor
    val font get() = (symbol ?: text).font

    private val tmp0 = Vector4f()
    override fun tickUpdate() {
        super.tickUpdate()
        val transform = getElement()
        val dragged = dragged
        var backgroundColor = originalBGColor
        val textColor0 = castedParent.getLocalColor(transform, tmp0)
        var textColor = black or (textColor0.toARGB(180))
        val showAddIndex = if (
            mouseX.toInt() in lx0..lx1 &&
            mouseY.toInt() in ly0..ly1 &&
            dragged is Draggable && dragged.getOriginal() is Transform
        ) {
            clamp(((mouseY - this.y) / this.h * 3).toInt(), 0, 2)
        } else null
        if (this.showAddIndex != showAddIndex) invalidateDrawing()
        this.showAddIndex = showAddIndex
        val isInFocus = isInFocus || selectedTransform == transform
        if (isHovered) textColor = hoverColor
        if (isInFocus) textColor = accentColor
        val colorDifference = sq(textColor.r() - backgroundColor.r()) +
                sq(textColor.g() - backgroundColor.g()) +
                sq(textColor.b() - backgroundColor.b())
        backgroundColor = if (colorDifference < 512) {// too similar colors
            if (textColor.g() > 127) black
            else white
        } else originalBGColor
        this.textColor = textColor
        text.backgroundColor = backgroundColor
        symbol?.backgroundColor = backgroundColor
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

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {

        val transform = getElement()
        when {
            button.isLeft -> {
                if (Input.isShiftDown && inFocus.size < 2) {
                    RemsStudio.largeChange(if (transform.isCollapsed) "Expanded ${transform.name}" else "Collapsed ${transform.name}") {
                        val target = !transform.isCollapsed
                        // remove children from the selection???...
                        inFocus.filterIsInstance<AbstractTreeViewPanel<*>>().forEach {
                            val transform2 = it.getElement()
                            transform2.isCollapsed = target
                        }
                        transform.isCollapsed = target
                    }
                } else {
                    castedParent.selectElementMaybe(transform)
                }
            }
            button.isRight -> openAddMenu(transform)
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        when {
            button.isLeft -> {
                castedParent.focusOnElement(getElement())
            }
            else -> super.onDoubleClick(x, y, button)
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String {
        return stringify(getElement())
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        try {
            val child0 = TextReader.read(data).first()
            val child = child0 as? V ?: return super.onPaste(x, y, data, type)
            val original = (dragged as? Draggable)?.getOriginal() as? V
            val relativeY = (y - this.y) / this.h
            val element = getElement()
            RemsStudio.largeChange("Moved Component") {
                if (relativeY < 0.33f) {
                    // paste on top
                    if (element.parent != null) {
                        element.addBefore(child)
                    } else {
                        element.addChild(child)
                    }
                    // we can't remove the element, if it's the parent
                    if (original !in child.listOfAll) {
                        original?.removeFromParent()
                    }
                } else if (relativeY < 0.67f) {
                    // paste as child
                    element.addChild(child)
                    if (element != original) {
                        // we can't remove the element, if it's the parent
                        if (original !in child.listOfAll) {
                            original?.removeFromParent()
                        }
                    }
                } else {
                    // paste below
                    if (element.parent != null) {
                        element.addAfter(child)
                    } else {
                        element.addChild(child)
                    }
                    // we can't remove the element, if it's the parent
                    if (original !in child.listOfAll) {
                        original?.removeFromParent()
                    }
                }
                castedParent.selectElement(child)
                // selectTransform(child)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
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
                            // todo content type...
                            stringify(element), "Transform", element,
                            TextPanel(element.name, style)
                        )
                    }
                }
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onEmpty(x: Float, y: Float) {
        onDeleteKey(x, y)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        RemsStudio.largeChange("Deleted Component ${getElement().name}") {
            val element = getElement()
            element.removeFromParent()
            for (it in element.listOfAll.toList()) it.destroy()
        }
    }

    override fun onBackSpaceKey(x: Float, y: Float) = onDeleteKey(x, y)
    override fun getCursor() = Cursor.drag

    override fun getTooltipText(x: Float, y: Float): String? {
        val element = getElement()
        return if (element is Camera) {
            element.defaultDisplayName + Dict[", drag onto scene to view", "ui.treeView.dragCameraToView"]
        } else element.defaultDisplayName
    }

    // multiple values can be selected
    override fun getMultiSelectablePanel() = this

    override val className get() = "TreeViewPanel"

    companion object {

        fun stringify(element: Any?): String {
            return if (element is ISaveable) {
                TextWriter.toText(element, false)
            } else element.toString()
        }

    }

}