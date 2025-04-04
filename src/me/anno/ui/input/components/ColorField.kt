package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.input.Key
import me.anno.utils.Color.mixARGB
import me.anno.engine.EngineBase
import me.anno.gpu.Cursor
import me.anno.ui.Panel
import me.anno.ui.dragging.Draggable
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.toARGB
import me.anno.utils.Color.toHexColor
import me.anno.utils.ColorParsing
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f

class ColorField(
    private val palette: ColorPalette,
    private val paletteX: Int,
    private val paletteY: Int,
    private val constSize: Int,
    style: Style
) : Panel(style) {

    constructor(base: ColorField) : this(
        base.palette,
        base.paletteX, base.paletteY,
        base.constSize,
        base.style
    ) {
        base.copyInto(this)
    }

    override fun calculateSize(w: Int, h: Int) {
        minW = constSize
        minH = constSize
        this.width = constSize
        this.height = constSize
    }

    var color: Int
        get() = palette.getColor(paletteX, paletteY)
        set(value) = palette.setColor(paletteX, paletteY, value)

    val focusColor: Int = black or 0xcccccc
    val hoverColor: Int = black or 0x777777
    val focusHoverColor: Int = mixARGB(focusColor, hoverColor, 0.5f)

    var changeListener: (ColorField, Int) -> Unit = { _, _ -> }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        // draw border/background depending on hover/focus
        val backgroundColor = if (isHovered) if (isInFocus) focusHoverColor else hoverColor
        else if (isInFocus) focusColor else backgroundColor
        DrawRectangles.drawRect(x, y, width, height, backgroundColor)
        TextureLib.colorShowTexture.bind(0, Filtering.TRULY_NEAREST, Clamping.REPEAT)
        DrawTextures.drawTexture(x + 1, y + 1, width - 2, height - 2, TextureLib.colorShowTexture, -1, tiling)
        DrawRectangles.drawRect(x + 1, y + 1, width - 2, height - 2, color)
    }

    override fun onCopyRequested(x: Float, y: Float) = color.toHexColor()

    fun setARGB(color: Int, notify: Boolean) {
        this.color = color
        if (notify) changeListener(this, color)
    }

    fun setRGBA(color: Vector4f, notify: Boolean) {
        setARGB(color.toARGB(), notify)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        if (button == Key.BUTTON_LEFT) {
            palette.onColorSelected(color)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        setARGB(0, true)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (val color = ColorParsing.parseColorComplex(data)) {
            is Int -> setARGB(color, true)
            is Vector4f -> setRGBA(color, true)
            null -> LOGGER.warn("Didn't understand color $data")
            else -> LOGGER.warn("Color type $data -> $color isn't yet supported for ColorChooser")
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
                EngineBase.dragged = Draggable(
                    colorString, "Color", color,
                    ColorField(this)
                )
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun clone(): ColorField = ColorField(this)
    override fun getCursor() = Cursor.hand

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ColorField) return
        dst.color = color
        // only works if there are no references inside
        dst.changeListener = changeListener
    }

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(ColorField::class)

        @JvmField
        val tiling = Vector4f(2f, 2f, 0f, 0f)
    }

}