package me.anno.ui.base.text

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.iconGray
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.input.Keys.isClickKey
import me.anno.utils.maths.Maths.mixARGB
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.max

open class TextPanel(text: String, style: Style) : Panel(style), TextStyleable {

    var instantTextLoading = false
    var padding = style.getPadding("textPadding", 2)
    var font = style.getFont("text", DefaultConfig.defaultFont)

    var textColor = style.getColor("textColor", iconGray)
        set(value) {
            if (field != value) {
                invalidateDrawing()
                field = value
            }
        }

    var focusTextColor = style.getColor("textColorFocused", -1)
    val hoverColor get() = mixARGB(textColor, focusTextColor, 0.5f)

    var textAlignment = AxisAlignment.MIN

    var textCacheKey: TextCacheKey = TextCacheKey(text, font, 0, 0)

    open var text: String = text
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
                invalidateLayout()
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

    fun drawText(dx: Int, dy: Int, color: Int): Int {
        return DrawTexts.drawText(
            this.x + dx + padding.left, this.y + dy + padding.top, font,
            textCacheKey, color, backgroundColor
        )
    }

    fun drawText(color: Int = effectiveTextColor) {
        val offset = if (textAlignment == AxisAlignment.MIN) 0
        else textAlignment.getOffset(w, getMaxWidth())
        drawText(offset, 0, color)
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
        if (widthLimit != textCacheKey.widthLimit || heightLimit != textCacheKey.heightLimit ||
            text != textCacheKey.text ||
            font.isBold != textCacheKey.isBold() || font.isItalic != textCacheKey.isItalic()
        ) {
            textCacheKey = TextCacheKey(text, font, widthLimit, heightLimit)
        }
        val size = getTextSize(textCacheKey)
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
        drawText(effectiveTextColor)
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

    override fun getCursor(): Long? = if (onClickListeners.isEmpty()) super.getCursor() else Cursor.drag

    override fun getPrintSuffix(): String = "\"${text.shorten(20)}\""

    override fun isKeyInput() = onClickListeners.isNotEmpty()
    override fun acceptsChar(char: Int) = when (char.toChar()) {
        '\t', '\n' -> false
        else -> true
    }

    override fun onKeyDown(x: Float, y: Float, key: Int) {
        if (key.isClickKey()) {
            for (l in onClickListeners) {
                if (l(x, y, MouseButton.LEFT, false)) {
                    return
                }
            }
        }
    }

}