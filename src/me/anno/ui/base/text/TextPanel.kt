package me.anno.ui.base.text

import me.anno.config.DefaultStyle.deepDark
import me.anno.config.DefaultStyle.iconGray
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.fonts.Font
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.utils.Color.a
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.AnyToBool.getBool
import me.anno.utils.types.AnyToInt.getInt
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
    }

    var padding = style.getPadding("textPadding", 2)

    open var font = style.getFont("text")
        set(value) {
            if (field != value) {
                field = value
                xOffsets = i0
            }
        }

    override var textColor = style.getColor("textColor", iconGray)

    var focusTextColor = style.getColor("textColorFocused", -1)
    var focusBackgroundColor = style.getColor("textBackgroundFocused", deepDark)

    private var xOffsets = i0
    val hoverColor get() = mixARGB(textColor, focusTextColor, 0.5f)

    // make this center??, no, center is sth for titles
    var textAlignmentX = AxisAlignment.MIN
    var textAlignmentY = AxisAlignment.CENTER

    // only really, if it might...
    override val canDrawOverBorders get() = true

    open var text: String = text
        set(value) {
            if (field != value) {
                field = value
                xOffsets = i0
            }
        }

    var breaksIntoMultiline = false

    // can be disabled for parents to copy ALL lines, e.g., for a bug report :)
    var disableCopy = false

    open var enableHoverColor = false
    open var enableFocusColor = true

    override var textSize: Float
        get() = font.size
        set(value) {
            font = font.withSize(value)
        }

    override var isBold: Boolean
        get() = font.isBold
        set(value) {
            font = font.withBold(value)
        }

    override var isItalic: Boolean
        get() = font.isItalic
        set(value) {
            font = font.withItalic(value)
        }

    open fun drawText(color: Int) {
        val textAlignmentX = textAlignmentX
        val textAlignmentY = textAlignmentY
        val padding = padding
        val ax = textAlignmentX.getAnchor(x + padding.left, width - padding.width)
        val ay = textAlignmentY.getAnchor(y + padding.top, height - padding.height)
        drawText(
            ax, ay, 0,
            font, text,
            color, backgroundColor,
            widthLimit, heightLimit,
            textAlignmentX, textAlignmentY
        )
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
            answer = getTextSizeX(font, text.substring(0, index)) - 1
            xOffsets[charIndex] = answer
        }
        return answer
    }

    open val widthLimit get() = if (breaksIntoMultiline) width - padding.width else -1
    open val heightLimit get() = -1

    override fun isOpaqueAt(x: Int, y: Int): Boolean {
        // to do this could be more pixel accurate...
        return super.isOpaqueAt(x, y) || textColor.a() >= minOpaqueAlpha
    }

    fun calculateSize(w: Int, text: String) {
        val widthLimit = max(1, if (breaksIntoMultiline) w - padding.width else GFX.maxTextureSize)
        val size = getTextSize(font, text, widthLimit, heightLimit)
        minW = max(1, getSizeX(size) + padding.width)
        minH = max(1, getSizeY(size) + padding.height)
    }

    fun underline(i0: Int, i1: Int) {
        underline(i0, i1, effectiveTextColor, 1)
    }

    fun underline(i0: Int, i1: Int, color: Int, thickness: Int) {
        val textSize = getTextSize(font, text, widthLimit, heightLimit)
        val dx = textAlignmentX.getOffset(width, getSizeX(textSize) + padding.width)
        val dy = textAlignmentY.getOffset(height, getSizeY(textSize) + padding.height)
        underline(i0, i1, color, thickness, dx, dy)
    }

    fun underline(i0: Int, i1: Int, color: Int, thickness: Int, dx: Int, dy: Int) {
        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top + font.sizeInt * 10 / 8
        val x0 = x + getTextSizeX(font, text.subSequence(0, i0))
        val x1 = x + getTextSizeX(font, text.subSequence(0, i1))
        DrawRectangles.drawRect(x0, y, x1 - x0, thickness, color)
    }

    override fun calculateSize(w: Int, h: Int) {
        calculateSize(w, text)
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val bg = background.color
        background.color = if (isInFocus && enableFocusColor) focusBackgroundColor else background.color
        drawBackground(x0, y0, x1, y1)
        drawText(effectiveTextColor)
        background.color = bg
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
        if (dst !is TextPanel) return
        dst.padding = padding
        dst.text = text
        dst.font = font
        dst.textColor = textColor
        dst.focusTextColor = focusTextColor
        dst.focusBackgroundColor = focusBackgroundColor
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
        writer.writeColor("focusBackground", focusBackgroundColor)
        writer.writeObject(null, "padding", padding)
        writer.writeBoolean("breaksIntoMultiline", breaksIntoMultiline)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "text" -> text = value.toString()
            "font" -> font = value as? Font ?: return
            "disableCopy" -> disableCopy = getBool(value)
            "textAlignmentX" -> textAlignmentX = AxisAlignment.find(value as? Int ?: return) ?: return
            "textAlignmentY" -> textAlignmentY = AxisAlignment.find(value as? Int ?: return) ?: return
            "textColor" -> textColor = getInt(value)
            "focusTextColor" -> focusTextColor = getInt(value)
            "focusBackground" -> focusBackgroundColor = getInt(value)
            "padding" -> padding = value as? Padding ?: return
            "breaksIntoMultiline" -> breaksIntoMultiline = getBool(value)
            else -> super.setProperty(name, value)
        }
    }

    companion object {
        @JvmField
        val i0 = IntArray(0)
    }
}