package me.anno.ui.base.buttons

import me.anno.config.DefaultStyle.black
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style
import me.anno.utils.input.Keys.isClickKey
import me.anno.maths.Maths.mixARGB
import kotlin.math.max

open class TextButton(
    title: String,
    var isSquare: Boolean,
    style: Style
) : TextPanel(title, style.getChild("button")) {

    init {
        tooltip = title
    }

    constructor(style: Style) : this("", false, style)

    constructor(title: String, description: String, isSquare: Boolean, style: Style) : this(title, isSquare, style) {
        setTooltip(description)
    }

    constructor(title: String, description: String, dictPath: String, isSquare: Boolean, style: Style) :
            this(Dict[title, dictPath], Dict[description, "$dictPath.desc"], isSquare, style)

    val leftColor = style.getColor("borderColorLeft", black or 0x999999)
    val rightColor = style.getColor("borderColorRight", black or 0x111111)
    val topColor = style.getColor("borderColorTop", black or 0x999999)
    val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

    val borderSize = style.getPadding("borderSize", 2)

    val normalBackground = backgroundColor
    val hoveredBackground = mixARGB(bottomColor, normalBackground, 0.7f)

    init {
        padding += borderSize
        this += WrapAlign.LeftTop
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        if (isSquare) {
            val size = max(minW, minH)
            minW = size
            minH = size
        }
    }

    var mouseDown = false
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override fun tickUpdate() {
        super.tickUpdate()
        mouseDown = (isHovered && 0 in Input.mouseKeysDown) ||
                (isInFocus && keysDown.any { it.key.isClickKey() })
        backgroundColor = if (isHovered && !mouseDown) hoveredBackground else normalBackground
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        draw(isHovered, mouseDown)
    }

    fun draw(isHovered: Boolean, mouseDown: Boolean) {

        drawBackground()

        val widthLimit = if (breaksIntoMultiline) this.w else -1
        val size = getTextSize(font, text, widthLimit, heightLimit)
        DrawTexts.drawText(
            x + (w - getSizeX(size)) / 2,
            y + (h - getSizeY(size)) / 2,
            font, text, textColor, backgroundColor, widthLimit, heightLimit
        )

        drawRect(
            x + w - borderSize.right, y, borderSize.right, h,
            getColor(isHovered, mouseDown, rightColor, leftColor)
        ) // right
        drawRect(
            x, y + h - borderSize.bottom, w,
            borderSize.bottom, getColor(isHovered, mouseDown, bottomColor, topColor)
        ) // bottom
        drawRect(x, y, borderSize.left, h, getColor(isHovered, mouseDown, leftColor, rightColor)) // left
        drawRect(x, y, w, borderSize.top, getColor(isHovered, mouseDown, topColor, bottomColor)) // top

    }

    fun getColor(isHovered: Boolean, mouseDown: Boolean, base: Int, alternative: Int): Int {
        val alpha = if (isHovered && !mouseDown) 0xaa000000.toInt() else black
        return alpha or ((if (mouseDown) alternative else base) and 0xffffff)
    }

    fun click() {
        onMouseClicked(x + w * 0.5f, y + h * 0.5f, MouseButton.LEFT, false)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (key.isClickKey()) click()
    }

    override fun acceptsChar(char: Int) = char.isClickKey()
    override fun isKeyInput() = true

    override fun clone(): TextButton {
        val clone = TextButton(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TextButton
        clone.isSquare = isSquare
    }

    override val className: String = "TextButton"

}