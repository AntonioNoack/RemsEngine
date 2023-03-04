package me.anno.ui.base.text

import me.anno.config.DefaultStyle.deepDark
import me.anno.config.DefaultStyle.iconGray
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawTextCharByChar
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.MouseButton
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Keys.isClickKey
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.withAlpha
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.types.Strings.isBlank2
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

    var textAlignment = AxisAlignment.MIN
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
        val textAlignment = textAlignment
        val offset = if (textAlignment == AxisAlignment.MIN) 0
        else textAlignment.getOffset(w, getMaxWidth())
        drawText(offset, 0, color)
    }

    fun getXOffset(charIndex: Int): Int {
        if (charIndex <= 0) return -1
        val text = text
        val font = font
        var xOffsets = xOffsets
        if (charIndex >= xOffsets.size) {
            val newOffsets = IntArray(text.length + 1) { Int.MIN_VALUE }
            System.arraycopy(xOffsets, 0, newOffsets, 0, xOffsets.size)
            this.xOffsets = newOffsets
            xOffsets = newOffsets
        }
        val index = min(charIndex, text.length)
        var answer = xOffsets[index]
        if (answer == Int.MIN_VALUE) {
            answer = getTextSizeX(font, text.substring(0, index), -1, -1) - 1
            xOffsets[charIndex] = answer
        }
        return answer
    }

    open val widthLimit get() = if (breaksIntoMultiline) w - padding.width else -1
    open val heightLimit get() = -1

    override fun isOpaqueAt(x: Int, y: Int): Boolean {
        // todo this could be more pixel accurate...
        return super.isOpaqueAt(x, y) || textColor.a() >= minOpaqueAlpha
    }

    fun calculateSize(w: Int, text: String) {
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
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
            textCacheKey = TextCacheKey(text, font, widthLimit, heightLimit)
        }
        // todo if useMonospaceCharacters, calculate size based on them
        val size = getTextSize(textCacheKey)
        minW = max(1, getSizeX(size) + padding.width)
        minH = max(1, getSizeY(size) + padding.height)
        // todo remove this, when it is no longer needed
        this.w = minW
        this.h = minH
        if (inst) loadTexturesSync.pop()
    }

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isBlank2()) "." else text
        calculateSize(w, text)
    }

    fun getMaxWidth() = getTextSizeX(font, text, -1, -1) + padding.width

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        val bg = backgroundColor
        backgroundColor = if (isInFocus) focusBackground else backgroundColor
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

    fun disableFocusColors(): TextPanel {
        focusTextColor = textColor
        focusBackground = backgroundColor
        return this
    }

    override fun clone(): TextPanel {
        val clone = TextPanel(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TextPanel
        clone.instantTextLoading = instantTextLoading
        clone.padding = padding
        clone.text = text
        clone.font = font
        clone.textColor = textColor
        clone.focusTextColor = focusTextColor
        clone.focusBackground = focusBackground
        clone.textAlignment = textAlignment
        // clone.textCacheKey = textCacheKey
        clone.breaksIntoMultiline = breaksIntoMultiline
        clone.disableCopy = disableCopy
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
        writer.writeObject(null, "font", font)
        writer.writeBoolean("disableCopy", disableCopy)
        writer.writeEnum("textAlignment", textAlignment)
        writer.writeColor("textColor", textColor)
        writer.writeColor("focusTextColor", focusTextColor)
        writer.writeColor("focusBackground", focusBackground)
        writer.writeObject(null, "padding", padding)
        writer.writeBoolean("breaksIntoMultiline", breaksIntoMultiline)
        writer.writeBoolean("instantTextLoading", instantTextLoading)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "disableCopy" -> disableCopy = value
            "breaksIntoMultiline" -> breaksIntoMultiline = value
            "instantTextLoading" -> instantTextLoading = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "textColor" -> textColor = value
            "focusTextColor" -> focusTextColor = value
            "focusBackground" -> focusBackground = value
            else -> super.readInt(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        if (name == "text") text = value ?: ""
        else super.readString(name, value)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "padding" -> padding = value as? Padding ?: return
            "font" -> font = value as? Font ?: return
            else -> super.readObject(name, value)
        }
    }

    override val className get() = "TextPanel"

    companion object {
        @JvmField
        val i0 = IntArray(0)
    }

}