package me.anno.ui.input.components

import me.anno.config.DefaultStyle.black
import me.anno.gpu.TextureLib
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.input.MouseButton
import me.anno.studio.StudioBase
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.dragging.Draggable
import me.anno.ui.style.Style
import me.anno.utils.Color.toARGB
import me.anno.utils.Color.toHexColor
import me.anno.utils.ColorParsing
import me.anno.utils.maths.Maths.mixARGB
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import org.joml.Vector4fc

// maximum size???...
class ColorPalette(
    private val dimX: Int,
    private val dimY: Int,
    style: Style
) : PanelGroup(style) {

    override val className get() = "ColorPalette"

    override val children: List<Panel> = Array(dimX * dimY) {
        ColorField(this, it % dimX, it / dimX, 0, style)
    }.toList()

    override fun remove(child: Panel) {}

    class ColorField(
        private val palette: ColorPalette,
        private val paletteX: Int,
        private val paletteY: Int,
        private val constSize: Int,
        style: Style
    ) : Panel(style) {

        constructor(field: ColorField) : this(
            field.palette,
            field.paletteX, field.paletteY,
            (field.w + field.h) / 2,
            field.style
        )

        override fun calculateSize(w: Int, h: Int) {
            minW = constSize
            minH = constSize
            this.w = constSize
            this.h = constSize
        }

        override val className get() = "ColorPaletteEntry"

        var color
            get() = palette.getColor(paletteX, paletteY)
            set(value) = palette.setColor(paletteX, paletteY, value)

        val focusColor = black or 0xcccccc
        val hoverColor = black or 0x777777
        val focusHoverColor = mixARGB(focusColor, hoverColor, 0.5f)

        override fun getVisualState(): Any? {
            return super.getVisualState() to Triple(isInFocus, isHovered, color)
        }

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            // draw border/background depending on hover/focus
            val backgroundColor = if (isHovered) if (isInFocus) focusHoverColor else hoverColor
            else if (isInFocus) focusColor else backgroundColor
            DrawRectangles.drawRect(x, y, w, h, backgroundColor)
            TextureLib.colorShowTexture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.REPEAT)
            val tiling = Vector4f(2f, 2f, 0f, 0f)
            DrawTextures.drawTexture(x + 1, y + 1, w - 2, h - 2, TextureLib.colorShowTexture, -1, tiling)
            DrawRectangles.drawRect(x + 1, y + 1, w - 2, h - 2, color)
        }

        override fun onCopyRequested(x: Float, y: Float): String? {
            return color.toHexColor()
        }

        fun setARGB(color: Int, notify: Boolean) {
            // todo notify & allow undo
            this.color = color
        }

        fun setRGBA(color: Vector4fc, notify: Boolean) {
            setARGB(color.toARGB(), notify)
        }

        override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
            if (button.isLeft) {
                palette.onColorSelected(color)
            }
        }

        /*override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            when {
                button.isRight -> {
                    // open context menu for clearing?
                }
                else -> super.onMouseClicked(x, y, button, long)
            }
        }*/

        override fun onEmpty(x: Float, y: Float) {
            color = 0
        }

        override fun onPaste(x: Float, y: Float, data: String, type: String) {
            when (val color = ColorParsing.parseColorComplex(data)) {
                is Int -> setARGB(color, true)
                is Vector4f -> setRGBA(color, true)
                null -> LOGGER.warn("Didn't understand color $data")
                else -> throw RuntimeException("Color type $data -> $color isn't yet supported for ColorChooser")
            }
        }

        override fun onGotAction(
            x: Float,
            y: Float,
            dx: Float,
            dy: Float,
            action: String,
            isContinuous: Boolean
        ): Boolean {
            when (action) {
                "DragStart" -> {
                    val color = color
                    val colorString = color.toHexColor()
                    StudioBase.dragged = Draggable(
                        colorString, "Color", color,
                        ColorField(this)
                        //TextPanel(colorString, style)
                    )
                }
                else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
            }
            return true
        }

    }

    var onColorSelected: (color: Int) -> Unit = { }

    override fun calculateSize(w: Int, h: Int) {
        minW = w
        minH = minW * dimY / dimX
        this.w = minW
        this.h = minH
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        for (j in 0 until dimY) {
            val y2 = y + j * h / dimY
            val y3 = y + (j + 1) * h / dimY
            for (i in 0 until dimX) {
                val x2 = x + i * w / dimX
                val x3 = x + (i + 1) * w / dimX
                val index = getIndex(i, j)
                val child = children[index]
                child.placeInParent(x2, y2)
                child.w = x3 - x2
                child.h = y3 - y2
            }
        }
    }

    fun getColor(x: Int, y: Int) = project?.config?.get("color.$x.$y", 0) ?: 0
    fun setColor(x: Int, y: Int, value: Int) {
        project?.config?.set("color.$x.$y", value)
    }

    private fun getIndex(x: Int, y: Int): Int = x + y * dimX

    companion object {
        private val LOGGER = LogManager.getLogger(ColorPalette::class)
    }

}