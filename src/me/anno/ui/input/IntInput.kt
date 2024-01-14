package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.pow
import me.anno.parser.SimpleExpressionParser
import me.anno.engine.EngineBase.Companion.shiftSlowdown
import me.anno.ui.Style
import me.anno.ui.input.components.NumberInputComponent
import me.anno.utils.types.AnyToLong
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToLong

// must be open for Rem's Studio
open class IntInput(
    title: String,
    visibilityKey: String,
    type: NumberType = NumberType.LONG,
    style: Style,
    inputPanel0: NumberInputComponent? = null
) : NumberInput<Long>(style, title, visibilityKey, type, inputPanel0) {

    final override var value: Long = getValue(type.defaultValue)
    var changeListener: (value: Long) -> Unit = { }

    @NotSerializedProperty
    private var savedDelta = 0f

    init {
        setText(value.toString(), false)
        inputPanel.addChangeListener {
            val newValue = parseValue(it)
            if (newValue != null) {
                value = newValue
                changeListener(newValue)
            }
        }
    }

    constructor(style: Style) : this("", "", NumberType.LONG, style)

    @Suppress("unused")
    constructor(title: String, visibilityKey: String, value0: Int, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style) {
        setValue(value0, false)
    }

    @Suppress("unused")
    constructor(title: String, visibilityKey: String, value0: Long, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style) {
        setValue(value0, false)
    }

    constructor(title: String, visibilityKey: String, value0: Int, style: Style) :
            this(title, visibilityKey, NumberType.LONG, style) {
        setValue(value0, false)
    }

    constructor(title: String, visibilityKey: String, value0: Long, style: Style) :
            this(title, visibilityKey, NumberType.LONG, style) {
        setValue(value0, false)
    }

    fun parseValue(text: String): Long? {
        return try {
            val trimmed = text.trim()
            val parsed = if (trimmed.isEmpty()) 0L
            else (if (trimmed.startsWith("0x")) trimmed.substring(2).toLongOrNull(16)
            else if (trimmed.startsWith("0b")) trimmed.substring(2).toLongOrNull(2)
            else trimmed.toLongOrNull()) ?: SimpleExpressionParser.parseDouble(trimmed)?.roundToLong()
            if (parsed == null) null else AnyToLong.getLong(type.clamp(parsed), 0L)
        } catch (ignored: Exception) {
            null
        }
    }

    fun setValue(v: Int, notify: Boolean): IntInput {
        setValue(v.toLong(), notify)
        return this
    }

    fun stringify(v: Long): String = v.toString()

    override fun changeValue(dx: Float, dy: Float) {
        val scale = 20f
        val ws = windowStack
        val size = scale * shiftSlowdown / max(ws.width, ws.height)
        val dx0 = dx * size
        val dy0 = dy * size
        val delta = dx0 - dy0
        // chose between exponential and linear curve, depending on the use-case
        savedDelta += delta * 0.5f * type.unitScale
        val actualDelta = round(savedDelta)
        savedDelta -= actualDelta
        var value = value
        if (type.hasLinear) value += actualDelta.toLong()
        if (type.hasExponential) value = (value * pow(
            if (value < 0) 1f / 1.03f else 1.03f,
            delta * if (type.hasLinear) 1f else 3f
        )).roundToLong()
        setValueClamped(value, true)
    }

    fun setValueClamped(value: Long, notify: Boolean) {
        setValue(AnyToLong.getLong(type.clamp(value), 0L), notify)
    }

    // must be open for Rem's Studio
    open fun getValue(value: Any): Long {
        return AnyToLong.getLong(value, 0)
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        when (codepoint) {
            '+'.code -> setValueClamped(value + 1, true)
            '-'.code -> setValueClamped(value - 1, true)
            else -> super.onCharTyped(x, y, codepoint)
        }
    }

    fun setChangeListener(listener: (value: Long) -> Unit): IntInput {
        changeListener = listener
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(type.defaultValue)
        if (newValue != value) {
            setValue(newValue, true)
        }
    }

    override fun setValue(newValue: Long, mask: Int, notify: Boolean): IntInput {
        if (newValue != value || !hasValue) {
            hasValue = true
            value = newValue
            setText(stringify(newValue), notify)
            if (notify) changeListener(newValue)
        }
        return this
    }

    fun updateValueMaybe() {
        if (inputPanel.isInFocus) {
            wasInFocus = true
        } else if (wasInFocus) {
            // apply the value, or reset if invalid
            val value = parseValue(inputPanel.value) ?: value
            setValue(value, true)
            wasInFocus = false
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        // evaluate the value, and write it back into the text field, e.g., for calculations
        hasValue = false
        setValue(value, true)
    }

    override fun clone(): IntInput {
        val clone = IntInput(title, visibilityKey, type, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as IntInput
        // only works if there are no hard references
        dst.changeListener = changeListener
        dst.savedDelta = savedDelta // ^^
    }

    override val className: String get() = "IntInput"
}