package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.VectorInputList
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.ColorParsing
import me.anno.utils.types.AnyToInt.getInt
import org.apache.logging.log4j.LogManager
import org.joml.Vector2i
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.roundToInt

open class IntVectorInput(
    title: String,
    visibilityKey: String,
    val type: NumberType,
    style: Style,
    val createComponent: () -> IntInput
) : TitledListY(title, visibilityKey, style), InputPanel<Vector4i>, TextStyleable {

    companion object {
        private val LOGGER = LogManager.getLogger(IntVectorInput::class)
    }

    constructor(title: String, visibilityKey: String, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style, { IntInput("", visibilityKey, type, style) })

    constructor(style: Style) : this("", "", NumberType.LONG, style)

    constructor(title: String, visibilityKey: String, value: Vector2i, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(title: String, visibilityKey: String, value: Vector3i, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(title: String, visibilityKey: String, value: Vector4i, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style) {
        setValue(value, -1, false)
    }

    private val components: Int get() = type.components
    private val valueFields = ArrayList<IntInput>(components)

    private var resetListener: (() -> Any?)? = null

    val valueList = VectorInputList(visibilityKey, style)

    init {
        if (type == NumberType.COLOR) LOGGER.warn("VectorInput should be replaced with ColorInput for type color!")
        this += valueList
        if (titleView != null) valueList.hide()
    }

    override var isInputAllowed: Boolean
        get() = valueFields.first().isInputAllowed
        set(value) {
            titleView?.setTextAlpha(if (value) 1f else 0.5f)
            for (child in valueFields) {
                child.isInputAllowed = value
            }
        }

    override val value: Vector4i
        get() = Vector4i(vx.toInt(), vy.toInt(), vz.toInt(), vw.toInt())

    override var textColor: Int
        get() = titleView?.textColor ?: 0
        set(value) {
            titleView?.textColor = value
        }

    override var textSize: Float
        get() = titleView?.textSize ?: 0f
        set(value) {
            titleView?.textSize = value
        }

    override var isBold: Boolean
        get() = titleView?.isBold == true
        set(value) {
            titleView?.isBold = value
        }

    override var isItalic: Boolean
        get() = titleView?.isItalic == true
        set(value) {
            titleView?.isItalic = value
        }

    fun setResetListener(listener: (() -> Any?)?): IntVectorInput {
        resetListener = listener
        return this
    }

    private fun addComponent(title: String, index: Int): IntInput {
        val component = createComponent()
        component.inputPanel.indexInProperty = index
        component.inputPanel.tooltip = title
        component.setChangeListener { onChange(1 shl index) }
        component.weight = 1f
        component.setResetListener {
            getInt(type.defaultValue, index, 0).toString()
        }
        valueList += component
        valueFields += component
        return component
    }

    override fun onCopyRequested(x: Float, y: Float): String? = "[$vx, $vy, $vz, $vw]"

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        pasteVector(data)
            ?: pasteScalar(data)
            ?: pasteColor(data)
            ?: super.onPaste(x, y, data, type)
    }

    fun pasteVector(data: String): Unit? {
        return if (data.startsWith("[") && data.endsWith("]") && data.indexOf('{') < 0) {
            val values = data.substring(1, data.lastIndex).split(',').map { it.trim().toIntOrNull() }
            if (values.size in 1..4) {
                values[0]?.apply { compX.setValue(this, true) }
                values.getOrNull(1)?.apply { compY?.setValue(this, true) }
                values.getOrNull(2)?.apply { compZ?.setValue(this, true) }
                values.getOrNull(3)?.apply { compW?.setValue(this, true) }
                Unit
            } else null
        } else null
    }

    fun pasteColor(data: String): Unit? {
        return when (val color = ColorParsing.parseColorComplex(data)) {
            is Int -> setValue(color.r(), color.g(), color.b(), color.a(), true)
            is Vector4f -> setValue(
                (255 * color.x).roundToInt(),
                (255 * color.y).roundToInt(),
                (255 * color.z).roundToInt(),
                (255 * color.w).roundToInt(), true
            )
            else -> null
        }
    }

    fun pasteScalar(data: String): Unit? {
        val allComponents = data.toIntOrNull()
        return if (allComponents != null) {
            compX.setValue(allComponents, true)
            compY?.setValue(allComponents, true)
            compZ?.setValue(allComponents, true)
            compW?.setValue(allComponents, true)
            Unit
        } else null
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        var focused1 = titleView?.isInFocus == true
        if (!focused1) {// removing the need for an iterator
            val children = valueList.children
            for (i in children.indices) {
                if (children[i].isInFocus) {
                    focused1 = true
                    break
                }
            }
        }
        if (focused1) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
        compX.updateValueMaybe()
        compY?.updateValueMaybe()
        compZ?.updateValueMaybe()
        compW?.updateValueMaybe()
    }

    val compX = addComponent("x", 0)
    val compY = if (components > 1) addComponent("y", 1) else null
    val compZ = if (components > 2) addComponent("z", 2) else null
    val compW = if (components > 3) addComponent("w", 3) else null

    val vx get() = compX.value
    val vy get() = compY?.value ?: 0L
    val vz get() = compZ?.value ?: 0L
    val vw get() = compW?.value ?: 0L

    fun setValue(v: Vector2i, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
    }

    fun setValue(v: Vector3i, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
        compZ?.setValue(v.z, notify)
    }

    final override fun setValue(newValue: Vector4i, mask: Int, notify: Boolean): Panel {
        compX.setValue(newValue.x, notify)
        compY?.setValue(newValue.y, notify)
        compZ?.setValue(newValue.z, notify)
        compW?.setValue(newValue.w, notify)
        return this
    }

    fun setValue(vi: IntVectorInput, notify: Boolean) {
        compX.setValue(vi.vx, notify)
        compY?.setValue(vi.vy, notify)
        compZ?.setValue(vi.vz, notify)
        compW?.setValue(vi.vw, notify)
    }

    fun setValue(vx: Int, vy: Int, vz: Int, vw: Int, notify: Boolean) {
        compX.setValue(vx, notify)
        compY?.setValue(vy, notify)
        compZ?.setValue(vz, notify)
        compW?.setValue(vw, notify)
    }

    fun setValue(vx: Long, vy: Long, vz: Long, vw: Long, notify: Boolean) {
        compX.setValue(vx, notify)
        compY?.setValue(vy, notify)
        compZ?.setValue(vz, notify)
        compW?.setValue(vw, notify)
    }

    val changeListeners = ArrayList<(x: Long, y: Long, z: Long, w: Long, mask: Int) -> Unit>()

    fun addChangeListener(listener: (x: Long, y: Long, z: Long, w: Long, mask: Int) -> Unit): IntVectorInput {
        changeListeners += listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): IntVectorInput {
        isSelectedListener = listener
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        val resetListener = resetListener
        if (resetListener == null) {
            onEmpty2(type.defaultValue)
        } else {
            onEmpty2(resetListener() ?: 0)
        }
    }

    fun onChange(mask: Int) {
        for (changeListener in changeListeners) {
            changeListener(vx, vy, vz, vw, mask)
        }
    }

    fun onEmpty2(defaultValue: Any) {
        for (index in valueFields.indices) {
            valueFields[index].setValue(getInt(defaultValue, index), false)
        }
        if (resetListener == null) {
            onChange(-1)
        }
    }

    override fun getCursor() = Cursor.drag

    override fun clone(): IntVectorInput {
        val clone = IntVectorInput(title, visibilityKey, type, style, createComponent)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as IntVectorInput
        // only works if there are no hard references
        dst.changeListeners.clear()
        dst.changeListeners.addAll(changeListeners)
        dst.resetListener = resetListener
        dst.setValue(vx, vy, vz, vw, false)
    }
}