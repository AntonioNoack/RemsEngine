package me.anno.ui.editor.treeView

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.midGray
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.select
import me.anno.input.Input
import me.anno.input.Input.mouseX
import me.anno.input.Input.mouseY
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import me.anno.objects.Camera
import me.anno.objects.Rectangle
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.effects.MaskLayer
import me.anno.studio.RemsStudio.onLargeChange
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.RemsStudio.selectedTransform
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.TextPanel
import me.anno.ui.dragging.Draggable
import me.anno.ui.editor.files.addChildFromFile
import me.anno.ui.style.Style
import me.anno.utils.clamp
import org.joml.Vector4f
import java.io.File

class TreeViewPanel(val getElement: () -> Transform, style: Style) : TextPanel("", style) {

    // todo the symbols should have equal size...

    // todo text shadow, if text color and background color are close

    private val accentColor = style.getColor("accentColor", black or 0xff0000)

    init {
        enableHoverColor = true
    }

    var showAddIndex: Int? = null

    override fun getVisualState(): Any? = Pair(super.getVisualState(), showAddIndex)

    override fun tickUpdate() {
        super.tickUpdate()
        val transform = getElement()
        val dragged = dragged
        textColor = black or (transform.getLocalColor().toRGB(180))
        showAddIndex = if (
            mouseX.toInt() in lx0..lx1 &&
            mouseY.toInt() in ly0..ly1 &&
            dragged is Draggable && dragged.getOriginal() is Transform
        ) {
            clamp(((mouseY - this.y) / this.h * 3).toInt(), 0, 2)
        } else null
        val isInFocus = isInFocus || selectedTransform == transform
        if (isHovered) textColor = hoverColor
        if (isInFocus) textColor = accentColor
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val showAddIndex = showAddIndex
        if (showAddIndex != null) {
            val x = x + padding.left
            val indent = textSize
            val lineWidth = textSize * 7
            val lineColor = midGray
            when (showAddIndex) {
                0 -> GFX.drawRect(x, y, lineWidth, 1, lineColor)
                1 -> GFX.drawRect(x + indent, y + h - 1, lineWidth, 1, lineColor)
                2 -> GFX.drawRect(x, y + h - 1, lineWidth, 1, lineColor)
            }
        }
    }

    override val effectiveTextColor: Int get() = textColor

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {

        val transform = getElement()
        when {
            button.isLeft -> {
                if (Input.isShiftDown) {
                    transform.isCollapsed = !transform.isCollapsed
                    onSmallChange("collapse")
                } else {
                    select(transform)
                }
            }
            button.isRight -> {

                fun add(action: (Transform) -> Transform): () -> Unit = { transform.apply { select(action(this)) } }
                val options = DefaultConfig["createNewInstancesList"] as? StringMap
                if (options != null) {
                    val extras = ArrayList<Pair<String, () -> Unit>>()
                    if (transform.parent != null) {
                        extras += "Add Mask" to {
                            val parent = transform.parent!!
                            val i = parent.children.indexOf(transform)
                            if (i < 0) throw RuntimeException()
                            val mask = MaskLayer.create(listOf(Rectangle.create()), listOf(transform))
                            mask.isFullscreen = true
                            parent.setChildAt(mask, i)
                        }
                    }
                    GFX.openMenu(
                        mouseX, mouseY, "Add Child",
                        options.entries
                            .sortedBy { (key, _) -> key.toLowerCase() }
                            .map { (key, value) ->
                                key to add {
                                    val newT = if (value is Transform) value.clone() else value.toString().toTransform()
                                    newT!!
                                    it.addChild(newT)
                                    newT
                                }
                            } + extras
                    )
                } else println("Reset the config, to enable this menu!")
            }
        }
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return getElement().stringify()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (!data.startsWith("[")) return super.onPaste(x, y, data, type)
        try {
            val child = TextReader.fromText(data).firstOrNull { it is Transform } as? Transform ?: return super.onPaste(
                x,
                y,
                data,
                type
            )
            val original = (dragged as? Draggable)?.getOriginal() as? Transform
            val relativeY = (y - this.y) / this.h
            val e = getElement()
            if (relativeY < 0.33f) {
                // paste on top
                if (e.parent != null) {
                    e.addBefore(child)
                } else {
                    e.addChild(child)
                }
                // we can't remove the element, if it's the parent
                if (original !in child.listOfAll) {
                    original?.removeFromParent()
                }
            } else if (relativeY < 0.67f) {
                // paste as child
                e.addChild(child)
                if (e != original) {
                    // we can't remove the element, if it's the parent
                    if (original !in child.listOfAll) {
                        original?.removeFromParent()
                    }
                }
            } else {
                // paste below
                if (e.parent != null) {
                    e.addAfter(child)
                } else {
                    e.addChild(child)
                }
                // we can't remove the element, if it's the parent
                if (original !in child.listOfAll) {
                    original?.removeFromParent()
                }
            }
            select(child)
            onLargeChange()
        } catch (e: Exception) {
            e.printStackTrace()
            super.onPaste(x, y, data, type)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val transform = getElement()
        files.forEach {
            addChildFromFile(transform, it, {})
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DragStart" -> {
                val transform = getElement()
                if (dragged?.getOriginal() != transform) {
                    dragged = Draggable(transform.stringify(), "Transform", transform, TextPanel(transform.name, style))
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
        getElement().destroy()
    }

    override fun onBackSpaceKey(x: Float, y: Float) = onDeleteKey(x, y)
    override fun getCursor() = Cursor.drag

    override fun getTooltipText(x: Float, y: Float): String? {
        val transform = getElement()
        return if (transform is Camera) transform.getDefaultDisplayName() + ", drag onto scene to view" else transform.getDefaultDisplayName()
    }

    fun Vector4f.toRGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

    // multiple values can be selected
    override fun getMultiSelectablePanel() = this

}