package me.anno.ui.base.buttons

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.Input
import me.anno.input.Input.keysDown
import me.anno.input.Key
import me.anno.language.translation.Dict
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import kotlin.math.max
import kotlin.math.roundToInt

open class TextButton(
    title: String,
    var aspectRatio: Float,
    style: Style
) : TextPanel(title, style.getChild("button")), InputPanel<Unit> {

    init {
        tooltip = title
    }

    override val value: Unit get() = Unit
    override fun setValue(newValue: Unit, mask: Int, notify: Boolean): Panel {
        return this
    }

    constructor(style: Style) : this("", style)
    constructor(title: String, style: Style) : this(title, 0f, style)
    constructor(title: String, isSquare: Boolean, style: Style) : this(title, if (isSquare) 1f else 0f, style)
    constructor(title: String, description: String, isSquare: Boolean, style: Style) : this(title, isSquare, style) {
        tooltip = description
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
    }

    override var isInputAllowed = true

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val aspectRatio = aspectRatio
        if (aspectRatio > 0f) {
            val size = max(minW.toFloat() / aspectRatio, minH.toFloat())
            minW = (size * aspectRatio).roundToInt()
            minH = size.toInt()
        }
    }

    var isPressed = false
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override fun onUpdate() {
        super.onUpdate()
        isPressed = isInputAllowed && ((isHovered && Input.isLeftDown) ||
                (isInFocus && keysDown.any { it.key.isClickKey() }))
        backgroundColor = if (isHovered && !isPressed && isInputAllowed) hoveredBackground else normalBackground
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        draw(x0, y0, x1, y1, isHovered && isInputAllowed, isPressed && isInputAllowed)
    }

    fun draw(x0: Int, y0: Int, x1: Int, y1: Int, isHovered: Boolean, mouseDown: Boolean) {

        drawBackground(x0, y0, x1, y1)

        val text = text
        val widthLimit = if (breaksIntoMultiline) this.width else -1
        val size = getTextSize(font, text, widthLimit, heightLimit)
        val textColor = textColor
        val tx = x + (width - getSizeX(size)) / 2
        val ty = y + (height - getSizeY(size)) / 2
        DrawTexts.drawText(
            tx, ty, font, text, textColor.withAlpha(
                if (isEnabled && isInputAllowed) textColor.a()
                else textColor.a() / 2
            ), backgroundColor, widthLimit, heightLimit
        )

        val bi = DrawRectangles.startBatch()
        var leftColor = leftColor
        var rightColor = rightColor
        var topColor = topColor
        var bottomColor = bottomColor
        if (!isInputAllowed) {
            val avgColor = mixARGB(
                mixARGB(leftColor, rightColor, 0.5f),
                mixARGB(topColor, bottomColor, 0.5f), 0.5f
            )
            val f = 0.5f
            leftColor = mixARGB(leftColor, avgColor, f)
            rightColor = mixARGB(rightColor, avgColor, f)
            topColor = mixARGB(topColor, avgColor, f)
            bottomColor = mixARGB(bottomColor, avgColor, f)
        }
        // draw button border
        drawRect(
            x + width - borderSize.right, y, borderSize.right, height,
            getColor(isHovered, mouseDown, rightColor, leftColor)
        ) // right
        drawRect(
            x, y + height - borderSize.bottom, width,
            borderSize.bottom, getColor(isHovered, mouseDown, bottomColor, topColor)
        ) // bottom
        drawRect(x, y, borderSize.left, height, getColor(isHovered, mouseDown, leftColor, rightColor)) // left
        drawRect(x, y, width, borderSize.top, getColor(isHovered, mouseDown, topColor, bottomColor)) // top
        DrawRectangles.finishBatch(bi)
    }

    fun getColor(isHovered: Boolean, mouseDown: Boolean, base: Int, alternative: Int): Int {
        val alpha = if (isHovered && !mouseDown) 0xaa000000.toInt() else black
        return alpha or ((if (mouseDown) alternative else base) and 0xffffff)
    }

    fun click() {
        onMouseClicked(x + width * 0.5f, y + height * 0.5f, Key.BUTTON_LEFT, false)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        if (isInputAllowed && key.isClickKey()) click()
        else super.onKeyTyped(x, y, key)
    }

    override fun acceptsChar(char: Int) = Key.byId(char).isClickKey() // not ideal...
    override fun isKeyInput() = true

    override fun getCursor(): Cursor? {
        return if (isInputAllowed) super.getCursor()
        else null
    }

    override fun clone(): TextButton {
        val clone = TextButton(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextButton
        dst.aspectRatio = aspectRatio
    }

    override val className: String get() = "TextButton"
}