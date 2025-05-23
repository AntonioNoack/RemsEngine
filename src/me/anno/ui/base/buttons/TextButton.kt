package me.anno.ui.base.buttons

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max

open class TextButton(nameDesc: NameDesc, var aspectRatio: Float, style: Style) :
    TextPanel(nameDesc, style.getChild("button")), InputPanel<Unit> {

    companion object {

        fun getColor(isHovered: Boolean, mouseDown: Boolean, base: Int, alternative: Int): Int {
            val alpha = if (isHovered && !mouseDown) 170 else 255
            return (if (mouseDown) alternative else base).withAlpha(alpha)
        }

        fun Panel.drawButtonBorder(
            leftColor: Int, topColor: Int, rightColor: Int, bottomColor: Int,
            isInputAllowed: Boolean, borderSize: Padding, mouseDown: Boolean,
        ) {
            val isHovered = isHovered && isInputAllowed
            val mouseDown1 = mouseDown && isInputAllowed
            val bi = DrawRectangles.startBatch()
            var leftColor1 = leftColor
            var rightColor1 = rightColor
            var topColor1 = topColor
            var bottomColor1 = bottomColor
            if (!isInputAllowed) {
                val avgColor = mixARGB(
                    mixARGB(leftColor1, rightColor1, 0.5f),
                    mixARGB(topColor1, bottomColor1, 0.5f), 0.5f
                )
                val f = 0.5f
                leftColor1 = mixARGB(leftColor1, avgColor, f)
                rightColor1 = mixARGB(rightColor1, avgColor, f)
                topColor1 = mixARGB(topColor1, avgColor, f)
                bottomColor1 = mixARGB(bottomColor1, avgColor, f)
            }
            // draw button border
            drawRect(
                x + width - borderSize.right, y, borderSize.right, height,
                getColor(isHovered, mouseDown1, rightColor1, leftColor1)
            ) // right
            drawRect(
                x, y + height - borderSize.bottom, width,
                borderSize.bottom, getColor(isHovered, mouseDown1, bottomColor1, topColor1)
            ) // bottom
            drawRect(x, y, borderSize.left, height, getColor(isHovered, mouseDown1, leftColor1, rightColor1)) // left
            drawRect(x, y, width, borderSize.top, getColor(isHovered, mouseDown1, topColor1, bottomColor1)) // top
            DrawRectangles.finishBatch(bi)
        }
    }

    init {
        textAlignmentX = AxisAlignment.CENTER
        textAlignmentY = AxisAlignment.CENTER
    }

    override val value: Unit get() = Unit
    override fun setValue(newValue: Unit, mask: Int, notify: Boolean): Panel {
        return this
    }

    constructor(style: Style) : this(NameDesc.EMPTY, style)
    constructor(nameDesc: NameDesc, style: Style) : this(nameDesc, 0f, style)
    constructor(nameDesc: NameDesc, isSquare: Boolean, style: Style) : this(nameDesc, if (isSquare) 1f else 0f, style)

    val leftColor = style.getColor("borderColorLeft", black or 0x999999)
    val rightColor = style.getColor("borderColorRight", black or 0x111111)
    val topColor = style.getColor("borderColorTop", black or 0x999999)
    val bottomColor = style.getColor("borderColorBottom", black or 0x111111)

    val borderSize = style.getPadding("borderSize", 2)

    val normalBackgroundColor get() = background.originalColor
    val hoveredBackgroundColor = mixARGB(bottomColor, normalBackgroundColor, 0.7f)

    init {
        padding += borderSize
    }

    override var isInputAllowed = true

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val aspectRatio = aspectRatio
        if (aspectRatio > 0f) {
            val size = max(
                max(minW - padding.width, 0).toFloat() / aspectRatio,
                max(minH - padding.height, 0).toFloat()
            )
            minW = max((size * aspectRatio).roundToIntOr() + padding.width, 0)
            minH = max(size.toInt() + padding.height, 0)
        }
    }

    var isPressed = false

    override fun onUpdate() {
        super.onUpdate()
        background.color = getCurrentBackgroundColor()
    }

    private fun getCurrentBackgroundColor(): Int {
        return if (isHovered && !isPressed && isInputAllowed) hoveredBackgroundColor
        else normalBackgroundColor
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)
        drawButtonText()
        drawButtonBorder(
            leftColor, topColor, rightColor, bottomColor,
            isInputAllowed, borderSize, isPressed
        )
        if (isInFocus) showIsInFocus(borderSize.left)
    }

    fun drawButtonText() {
        val text = text
        val widthLimit = if (breaksIntoMultiline) this.width else -1
        val alignmentX = textAlignmentX
        val alignmentY = textAlignmentY
        val textColor = textColor
        val textAlpha = if (isEnabled && isInputAllowed) textColor.a()
        else textColor.a() / 2
        DrawTexts.drawTextOrFail(
            alignmentX.getAnchor(x + padding.left, width - padding.width),
            alignmentY.getAnchor(y + padding.top, height - padding.height),
            font, text, textColor.withAlpha(textAlpha), backgroundColor, widthLimit, heightLimit,
            alignmentX, alignmentY
        )
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        super.onKeyDown(x, y, key)
        isPressed = key.isClickKey(true)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        super.onKeyUp(x, y, key)
        isPressed = false
    }

    fun click() {
        onMouseClicked(x + width * 0.5f, y + height * 0.5f, Key.BUTTON_LEFT, false)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        if (isInputAllowed && key.isClickKey(false)) click()
        else uiParent?.onKeyTyped(x, y, key)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (isInputAllowed) super.onMouseClicked(x, y, button, long)
        else uiParent?.onMouseClicked(x, y, button, long)
    }

    override fun acceptsChar(char: Int) = Key.byId(char).isClickKey(true) // not ideal...
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
        if (dst !is TextButton) return
        dst.aspectRatio = aspectRatio
    }
}