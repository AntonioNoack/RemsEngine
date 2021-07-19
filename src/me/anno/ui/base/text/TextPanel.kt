package me.anno.ui.base.text

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.iconGray
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.input.MouseButton
import me.anno.ui.base.Font
import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import me.anno.utils.Tabs
import me.anno.utils.input.Keys.isClickKey
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.max
import kotlin.math.min

open class TextPanel(text: String, style: Style) : Panel(style), TextStyleable {

    var instantTextLoading = false
    var padding = style.getPadding("textPadding", 2)
    var font = style.getFont("text", DefaultConfig.defaultFont)

    var textColor = style.getColor("textColor", iconGray)
        set(value) {
            if(field != value){
                invalidateDrawing()
                field = value
            }
        }

    var focusTextColor = style.getColor("textColorFocused", -1)
    val hoverColor get() = mixARGB(textColor, focusTextColor, 0.5f)

    var textAlignment = AxisAlignment.MIN

    open var text: String = text
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override fun setBold(bold: Boolean) {
        font = font.withBold(bold)
        invalidateDrawing()
        invalidateLayout()
    }

    override fun setItalic(italic: Boolean) {
        font = font.withItalic(italic)
        invalidateDrawing()
        invalidateLayout()
    }

    // make this panel work without states, as states accumulate to 13% of the idle-allocations at runtime
    // it seems to work...
    /*override fun getLayoutState(): Any? {
        val texture = if(canBeSeen){
            // keep the texture loaded, in case we need it
            val widthLimit = if(breaksIntoMultiline) w else -1
            FontManager.getString(font, text, widthLimit)
        } else null
        val texWidth = texture?.w
        return Pair(super.getLayoutState(), texWidth)
    }

    override fun getVisualState(): Any? {
        val texture = if(canBeSeen){
            // keep the texture loaded, in case we need it
            val widthLimit = if(breaksIntoMultiline) w else -1
            FontManager.getString(font, text, widthLimit)
        } else null
        return Triple(super.getVisualState(), (texture as? Texture2D)?.state, effectiveTextColor)
    }*/

    var breaksIntoMultiline = false

    // can be disabled for parents to copy ALL lines, e.g. for a bug report :)
    var disableCopy = false

    fun drawText(dx: Int, dy: Int, text: String, color: Int): Int {
        return DrawTexts.drawText(
            this.x + dx + padding.left, this.y + dy + padding.top, font,
            text, color, backgroundColor, widthLimit, heightLimit
        )
    }

    var minW2 = 0
    var minH2 = 0

    open val widthLimit get() = if (breaksIntoMultiline) w - padding.width else -1
    open val heightLimit get() = -1

    fun calculateSize(w: Int, h: Int, text: String) {
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        super.calculateSize(w, h)
        val widthLimit = if (breaksIntoMultiline) w - padding.width else -1
        val heightLimit = -1
        val size = getTextSize(font, text, widthLimit, heightLimit)
        minW = max(1, getSizeX(size) + padding.width)
        minH = max(1, getSizeY(size) + padding.height)
        minW2 = minW
        minH2 = minH
        if (inst) loadTexturesSync.pop()
    }

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isBlank2()) "." else text
        calculateSize(w, h, text)
    }

    fun getMaxWidth() = getTextSizeX(font, text, -1, -1) + padding.width

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        super.onDraw(x0, y0, x1, y1)
        val offset = if (textAlignment == AxisAlignment.MIN) 0
        else textAlignment.getOffset(w, getMaxWidth())
        drawText(offset, 0, text, effectiveTextColor)
        if (inst) loadTexturesSync.pop()
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return if (disableCopy) super.onCopyRequested(x, y) else text
    }

    open var enableHoverColor = false

    open val effectiveTextColor
        get() =
            if (isHovered && enableHoverColor) hoverColor
            else if (isInFocus) focusTextColor
            else textColor

    override fun getCursor(): Long? = if (onClickListener == null) super.getCursor() else Cursor.drag

    override fun printLayout(tabDepth: Int) {
        println(
            "${Tabs.spaces(tabDepth * 2)}${javaClass.simpleName}($weight, ${if (visibility == Visibility.VISIBLE) "v" else "_"}) " +
                    "$x $y += $w $h ($minW $minH) \"${text.substring(0, min(text.length, 20))}\""
        )
    }

    override fun isKeyInput() = onClickListener != null
    override fun acceptsChar(char: Int) = when (char.toChar()) {
        '\t', '\n' -> false
        else -> true
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        if (key.isClickKey()) onClickListener?.invoke(x, y, MouseButton.LEFT, false)
    }

}