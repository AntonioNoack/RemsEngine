package me.anno.ui.base.text

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.iconGray
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.input.Keys.isClickKey
import me.anno.maths.Maths.mixARGB
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.max

open class TextPanel(text: String, style: Style) : Panel(style), TextStyleable {

    constructor(style: Style): this("", style)

    constructor(base: TextPanel) : this(base.text, base.style){
        base.copy(this)
    }

    constructor(nameDesc: NameDesc, style: Style): this(nameDesc.name, style){
        tooltip = nameDesc.desc
    }

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

    @NotSerializedProperty
    var textCacheKey: TextCacheKey = TextCacheKey(text, font, 0, 0)

    // only really, if it might...
    override val canDrawOverBorders: Boolean = true

    open var text: String = text
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
                invalidateLayout()
            }
        }

    var breaksIntoMultiline = false

    // can be disabled for parents to copy ALL lines, e.g. for a bug report :)
    var disableCopy = false

    @NotSerializedProperty
    protected var minW2 = 0

    @NotSerializedProperty
    protected var minH2 = 0

    open var enableHoverColor = false

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
            font.name != textCacheKey.fontName ||
            font.isBold != textCacheKey.isBold() ||
            font.isItalic != textCacheKey.isItalic()
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

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return if (disableCopy) super.onCopyRequested(x, y) else text
    }

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

    override fun clone() = TextPanel(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TextPanel
        clone.instantTextLoading = instantTextLoading
        clone.padding = padding
        clone.font = font
        clone.textColor = textColor
        clone.focusTextColor = focusTextColor
        clone.textAlignment = textAlignment
        clone.textCacheKey = textCacheKey
        clone.breaksIntoMultiline = breaksIntoMultiline
        clone.disableCopy = disableCopy
    }

    override val className: String = "TextPanel"

}