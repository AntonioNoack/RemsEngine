package me.anno.ui.base.text

import me.anno.config.DefaultStyle.deepDark
import me.anno.config.DefaultStyle.iconGray
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.fonts.Font
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawTextCharByChar
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeCharByChar
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.Key
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.utils.Color.a
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.shorten
import kotlin.math.max
import kotlin.math.min

open class TextPanel(text: String, style: Style) : Panel(style), TextStyleable {

    constructor(style: Style) : this("", style)

    constructor(nameDesc: NameDesc, style: Style) : this(nameDesc.name, style) {
        tooltip = nameDesc.desc
    }

    fun setTextAlpha(alpha: Float) {
        textColor = textColor.withAlpha(alpha)
        invalidateDrawing()
    }

    var instantTextLoading = false
    var useMonospaceCharacters = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var padding = style.getPadding("textPadding", 2)
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    open var font = style.getFont("text")
        set(value) {
            if (field != value) {
                field = value
                xOffsets = i0
                invalidateLayout()
            }
        }

    override var textColor = style.getColor("textColor", iconGray)
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    var focusTextColor = style.getColor("textColorFocused", -1)
        set(value) {
            if (field != value) {
                if (isInFocus) invalidateDrawing()
                field = value
            }
        }

    var focusBackground = style.getColor("textBackgroundFocused", deepDark)
        set(value) {
            if (field != value) {
                if (isInFocus) invalidateDrawing()
                field = value
            }
        }

    private var xOffsets = i0
    val hoverColor get() = mixARGB(textColor, focusTextColor, 0.5f)

    var textAlignmentX = AxisAlignment.MIN // make this center??
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    var textAlignmentY = AxisAlignment.MIN
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    @NotSerializedProperty
    var textCacheKey: TextCacheKey = TextCacheKey(text, font)

    // only really, if it might...
    override val canDrawOverBorders get() = true

    open var text: String = text
        set(value) {
            if (field != value) {
                field = value
                xOffsets = i0
                invalidateLayout()
            }
        }

    var breaksIntoMultiline = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // can be disabled for parents to copy ALL lines, e.g., for a bug report :)
    var disableCopy = false

    open var enableHoverColor = false
    open var enableFocusColor = true

    override var textSize: Float
        get() = font.size
        set(value) {
            if (font.size != value) {
                font = font.withSize(value)
                invalidateLayout()
            }
        }

    override var isBold: Boolean
        get() = font.isBold
        set(value) {
            if (font.isBold != value) {
                font = font.withBold(isBold)
                invalidateLayout()
            }
        }

    override var isItalic: Boolean
        get() = font.isItalic
        set(value) {
            if (font.isItalic != value) {
                font = font.withItalic(isItalic)
                invalidateLayout()
            }
        }

    open fun drawText(dx: Int, dy: Int, text: String, color: Int): Int {
        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top
        return if (useMonospaceCharacters) {
            drawTextCharByChar(
                x, y, font, text, color,
                backgroundColor, widthLimit, heightLimit,
                AxisAlignment.MIN, AxisAlignment.MIN, true
            )
        } else {
            DrawTexts.drawText(x, y, font, text, color, backgroundColor, widthLimit, heightLimit)
        }
    }

    open fun drawText(dx: Int, dy: Int, color: Int): Int {
        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top
        return if (useMonospaceCharacters) {
            drawText(dx, dy, text, color)
        } else {
            DrawTexts.drawText(x, y, font, textCacheKey, color, backgroundColor)
        }
    }

    open fun drawText(color: Int = effectiveTextColor) {
        val textSize = getTextSize(font, text, -1, -1, false)
        val dx = textAlignmentX.getOffset(width, getSizeX(textSize) + padding.width)
        val dy = textAlignmentY.getOffset(height, getSizeY(textSize) + padding.height)
        drawText(dx, dy, color)
    }

    fun getXOffset(charIndex: Int): Int {
        if (charIndex <= 0) return -1
        val text = text
        val font = font
        var xOffsets = xOffsets
        if (charIndex >= xOffsets.size) {
            val newOffsets = xOffsets.copyOf(text.length + 1)
            newOffsets.fill(Int.MIN_VALUE, xOffsets.size)
            this.xOffsets = newOffsets
            xOffsets = newOffsets
        }
        val index = min(charIndex, text.length)
        var answer = xOffsets[index]
        if (answer == Int.MIN_VALUE) {
            answer = getTextSizeX(font, text.substring(0, index), -1, -1, false) - 1
            xOffsets[charIndex] = answer
        }
        return answer
    }

    open val widthLimit get() = if (breaksIntoMultiline) width - padding.width else -1
    open val heightLimit get() = -1

    override fun isOpaqueAt(x: Int, y: Int): Boolean {
        // todo this could be more pixel accurate...
        return super.isOpaqueAt(x, y) || textColor.a() >= minOpaqueAlpha
    }

    fun calculateSize(w: Int, text: String) {
        val widthLimit = max(1, if (breaksIntoMultiline) w - padding.width else GFX.maxTextureSize)
        val heightLimit = max(1, GFX.maxTextureSize)
        if (widthLimit != textCacheKey.widthLimit ||
            heightLimit != textCacheKey.heightLimit ||
            text != textCacheKey.text ||
            font.name != textCacheKey.fontName ||
            font.isBold != textCacheKey.isBold() ||
            font.isItalic != textCacheKey.isItalic() ||
            font.sizeIndex != textCacheKey.fontSizeIndex()
        ) {
            textCacheKey = TextCacheKey(text, font, widthLimit, heightLimit, false)
        }
        if (useMonospaceCharacters) {
            calculateSizeMono()
        } else {
            val inst = instantTextLoading
            if (inst) loadTexturesSync.push(true)
            val size = getTextSize(textCacheKey, !inst)
            if (size == -1) {
                calculateSizeMono()
                invalidateLayout() // mark as not final yet
            } else {
                minW = max(1, getSizeX(size) + padding.width)
                minH = max(1, getSizeY(size) + padding.height)
            }
            if (inst) loadTexturesSync.pop()
        }
    }

    private fun calculateSizeMono() {
        val size = getTextSizeCharByChar(font, text, true)
        minW = max(1, getSizeX(size) + padding.width)
        minH = max(1, getSizeY(size) + padding.height)
    }

    fun underline(i0: Int, i1: Int) {
        underline(i0, i1, effectiveTextColor, 1)
    }

    fun underline(i0: Int, i1: Int, color: Int, thickness: Int) {
        val textSize = getTextSize(font, text, -1, -1, false)
        val dx = textAlignmentX.getOffset(width, getSizeX(textSize) + padding.width)
        val dy = textAlignmentY.getOffset(height, getSizeY(textSize) + padding.height)
        underline(i0, i1, color, thickness, dx, dy)
    }

    fun underline(i0: Int, i1: Int, color: Int, thickness: Int, dx: Int, dy: Int) {
        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top + font.sizeInt * 10 / 8
        val x0 = x + getTextSizeX(font, text.subSequence(0, i0), -1, -1, false)
        val x1 = x + getTextSizeX(font, text.subSequence(0, i1), -1, -1, false)
        DrawRectangles.drawRect(x0, y, x1 - x0, thickness, color)
    }

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isBlank2()) "." else text
        calculateSize(w, text)
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        val bg = backgroundColor
        backgroundColor = if (isInFocus && enableFocusColor) focusBackground else backgroundColor
        drawBackground(x0, y0, x1, y1)
        drawText(effectiveTextColor)
        backgroundColor = bg
        if (inst) loadTexturesSync.pop()
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return if (disableCopy) super.onCopyRequested(x, y) else text
    }

    open val effectiveTextColor
        get() =
            if (isHovered && enableHoverColor) hoverColor
            else if (isInFocus && enableFocusColor) focusTextColor
            else textColor

    override fun getCursor(): Cursor? = if (onClickListeners.isEmpty()) super.getCursor() else Cursor.drag

    override fun getPrintSuffix(): String = "\"${text.shorten(20)}\""

    override fun isKeyInput() = onClickListeners.isNotEmpty()
    override fun acceptsChar(char: Int) = when (char.toChar()) {
        '\t', '\n' -> false
        else -> true
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key.isClickKey(false)) {
            for (listener in onClickListeners) {
                if (listener(this, x, y, Key.BUTTON_LEFT, false)) {
                    return
                }
            }
        }
    }

    fun disableFocusColors(): TextPanel {
        enableFocusColor = false
        return this
    }

    override fun clone(): TextPanel {
        val clone = TextPanel(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextPanel
        dst.instantTextLoading = instantTextLoading
        dst.padding = padding
        dst.text = text
        dst.font = font
        dst.textColor = textColor
        dst.focusTextColor = focusTextColor
        dst.focusBackground = focusBackground
        dst.textAlignmentX = textAlignmentX
        dst.textAlignmentY = textAlignmentY
        // clone.textCacheKey = textCacheKey
        dst.breaksIntoMultiline = breaksIntoMultiline
        dst.disableCopy = disableCopy
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
        writer.writeObject(null, "font", font)
        writer.writeBoolean("disableCopy", disableCopy)
        writer.writeEnum("textAlignmentX", textAlignmentX)
        writer.writeEnum("textAlignmentY", textAlignmentY)
        writer.writeColor("textColor", textColor)
        writer.writeColor("focusTextColor", focusTextColor)
        writer.writeColor("focusBackground", focusBackground)
        writer.writeObject(null, "padding", padding)
        writer.writeBoolean("breaksIntoMultiline", breaksIntoMultiline)
        writer.writeBoolean("instantTextLoading", instantTextLoading)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "text" -> text = value as? String ?: return
            "font" -> font = value as? Font ?: return
            "disableCopy" -> disableCopy = value == true
            "textAlignmentX" -> textAlignmentX = AxisAlignment.find(value as? Int ?: return) ?: return
            "textAlignmentY" -> textAlignmentY = AxisAlignment.find(value as? Int ?: return) ?: return
            "textColor" -> textColor = value as? Int ?: return
            "focusTextColor" -> focusTextColor = value as? Int ?: return
            "focusBackground" -> focusBackground = value as? Int ?: return
            "padding" -> padding = value as? Padding ?: return
            "breaksIntoMultiline" -> breaksIntoMultiline = value == true
            "instantTextLoading" -> instantTextLoading = value == true
            else -> super.setProperty(name, value)
        }
    }

    override val className: String get() = "TextPanel"

    companion object {
        @JvmField
        val i0 = IntArray(0)
    }
}