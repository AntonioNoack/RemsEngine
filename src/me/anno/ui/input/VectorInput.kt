package me.anno.ui.input

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.MouseButton
import me.anno.io.text.TextReader
import me.anno.objects.Camera
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.studio.StudioBase.Companion.warn
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.Selection.selectedProperty
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.input.components.VectorInputComponent
import me.anno.ui.style.Style
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.pow
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import org.joml.*
import kotlin.math.max

class VectorInput(
    style: Style, var title: String,
    val type: Type,
    private val owningProperty: AnimatedProperty<*>? = null
) : PanelListY(style) {

    constructor(title: String, property: AnimatedProperty<*>, time: Double, style: Style) :
            this(style, title, property.type, property) {
        when (val value = property[time]) {
            is Vector2f -> setValue(value, false)
            is Vector3f -> setValue(value, false)
            is Vector4f -> setValue(value, false)
            is Quaternionf -> setValue(value, false)
            else -> throw RuntimeException("Type $value not yet supported!")
        }
    }

    constructor(
        style: Style, title: String, value: Vector2fc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Vector3fc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Vector4fc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Quaternionf,
        type: Type = Type.QUATERNION
    ) : this(style, title, type) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Vector2dc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Vector3dc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Vector4dc, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, value: Quaterniond,
        type: Type = Type.QUATERNION
    ) : this(style, title, type) {
        setValue(value, false)
    }

    private val components: Int = type.components
    private val valueFields = ArrayList<PureTextInput>(components)

    private fun addComponent(i: Int, title: String): FloatInput {
        val pseudo = VectorInputComponent(this.title, type, owningProperty, i, this, style)
        val input = pseudo.inputPanel
        pseudo.inputPanel.tooltip = title
        valueList += input.setWeight(1f)
        valueFields += input
        return pseudo
    }

    // val titleList = PanelListX(style)
    private val valueList = object : PanelListX(style) {
        override var visibility: Visibility
            get() = InputVisibility[title]
            set(_) {}
        override fun onEnterKey(x: Float, y: Float) {
            this@VectorInput.onEnterKey(x, y)
        }
    }

    init {
        valueList.disableConstantSpaceForWeightedChildren = true
    }

    private val titleView = TitlePanel(title, this, style)

    init {

        if (type == Type.COLOR) warn("VectorInput should be replaced with ColorInput for type color!")

        // titleList += WrapAlign.Top
        valueList += WrapAlign.TopFill

        this += titleView
        titleView.enableHoverColor = true
        titleView.setWeight(1f)
        titleView.focusTextColor = titleView.textColor
        titleView.setSimpleClickListener {
            InputVisibility.toggle(title, this)
            valueList.children.forEach { it.show() }
        }

        this += valueList
        valueList.hide()

    }

    override fun onCopyRequested(x: Float, y: Float): String? =
        owningProperty?.toString()
            ?: "[${compX.lastValue}, ${compY.lastValue}, ${compZ?.lastValue ?: 0f}, ${compW?.lastValue ?: 0f}]"

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        pasteVector(data)
            ?: pasteScalar(data)
            ?: pasteColor(data)
            ?: pasteAnimated(data)
            ?: super.onPaste(x, y, data, type)
    }

    private fun pasteVector(data: String): Unit? {
        return if (data.startsWith("[") && data.endsWith("]") && data.indexOf('{') < 0) {
            val values = data.substring(1, data.lastIndex).split(',').map { it.trim().toDoubleOrNull() }
            if (values.size in 1..4) {
                values[0]?.apply { compX.setValue(this, true) }
                values[1]?.apply { compY.setValue(this, true) }
                values.getOrNull(2)?.apply { compZ?.setValue(this, true) }
                values.getOrNull(3)?.apply { compW?.setValue(this, true) }
                Unit
            } else null
        } else null
    }

    private fun pasteColor(data: String): Unit? {
        return when (val color = ColorParsing.parseColorComplex(data)) {
            is Int -> setValue(color.toVecRGBA(), true)
            is Vector4f -> setValue(color, true)
            else -> null
        }
    }

    private fun pasteScalar(data: String): Unit? {
        val allComponents = data.toDoubleOrNull()
        return if (allComponents != null && allComponents.isFinite()) {
            compX.setValue(allComponents, true)
            compY.setValue(allComponents, true)
            compZ?.setValue(allComponents, true)
            compW?.setValue(allComponents, true)
        } else null
    }

    private fun pasteAnimated(data: String): Unit? {
        return try {
            val editorTime = editorTime
            val animProperty = TextReader.fromText(data).firstOrNull() as? AnimatedProperty<*>
            if (animProperty != null) {
                if (owningProperty != null) {
                    owningProperty.copyFrom(animProperty)
                    when (val value = owningProperty[editorTime]) {
                        is Vector2f -> setValue(value, true)
                        is Vector3f -> setValue(value, true)
                        is Vector4f -> setValue(value, true)
                        is Quaternionf -> setValue(value, true)
                        else -> warn("Unknown pasted data type $value")
                    }
                } else {
                    // get the default value? no, the current value? yes.
                    val atTime = animProperty[editorTime]!!
                    setValue(
                        Vector4f(
                            getFloat(atTime, 0, vx),
                            getFloat(atTime, 1, vy),
                            getFloat(atTime, 2, vz),
                            getFloat(atTime, 3, vw)
                        ), true
                    )
                }
            }
            Unit
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val focused1 = titleView.isInFocus || valueList.children.any { it.isInFocus }
        if (focused1) isSelectedListener?.invoke()
        if (RemsStudio.hideUnusedProperties) {
            val focused2 = focused1 || owningProperty == selectedProperty
            valueList.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }
        super.onDraw(x0, y0, x1, y1)
        compX.updateValueMaybe()
        compY.updateValueMaybe()
        compZ?.updateValueMaybe()
        compW?.updateValueMaybe()
    }

    val compX = addComponent(0, "x")
    val compY = addComponent(1, "y")
    val compZ = if (components > 2) addComponent(2, "z") else null
    val compW = if (components > 3) addComponent(3, "w") else null

    val vx get() = compX.lastValue.toFloat()
    val vy get() = compY.lastValue.toFloat()
    val vz get() = compZ?.lastValue?.toFloat() ?: 0f
    val vw get() = compW?.lastValue?.toFloat() ?: 0f

    fun setValue(v: Vector2fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Quaternionfc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Vector2dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Quaterniondc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(vi: VectorInput, notify: Boolean) {
        compX.setValue(vi.vx, notify)
        compY.setValue(vi.vy, notify)
        compZ?.setValue(vi.vz, notify)
        compW?.setValue(vi.vw, notify)
    }

    var changeListener: (x: Double, y: Double, z: Double, w: Double) -> Unit = { _, _, _, _ ->
    }

    fun setChangeListener(listener: (x: Double, y: Double, z: Double, w: Double) -> Unit): VectorInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): VectorInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    var mouseIsDown = false
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown) {
            val size = 20f * shiftSlowdown * (if (selectedTransform is Camera) -1f else 1f) / max(GFX.width, GFX.height)
            val dx0 = dx * size
            val dy0 = dy * size
            val delta = dx0 - dy0
            when (type) {
                Type.POSITION -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz), true)
                }
                Type.POSITION_2D -> {
                    val scaleFactor = 0.2f
                    setValue(Vector2f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor), true)
                }
                Type.ROT_YXZ -> {
                    val scaleFactor = 20f
                    if (isControlDown) {
                        setValue(Vector3f(vx, vy, vz + delta * scaleFactor), true)
                    } else {
                        setValue(Vector3f(vx + dy0 * scaleFactor, vy + dx0 * scaleFactor, vz), true)
                    }
                }
                /*Type.SCALE -> {
                    val scaleFactor = 1.03f
                    if (isControlDown) {
                        val scaleX = pow(scaleFactor, dx0)
                        val scaleY = pow(scaleFactor, -dy0)
                        setValue(Vector3f(vx * scaleX, vy * scaleY, vz), true)
                    } else {
                        val scale = pow(scaleFactor, delta)
                        setValue(Vector3f(vx * scale, vy * scale, vz * scale), true)
                    }
                }*/
                Type.COLOR -> {
                    val scaleFactor = 1.10f
                    val scale = pow(scaleFactor, delta)
                    if (isControlDown) {
                        setValue(Vector4f(vx * scale, vy * scale, vz * scale, vw), true)
                    } else {
                        setValue(Vector4f(vx, vy, vz, clamp(vw + delta, 0f, 1f)), true)
                    }
                }
                Type.SKEW_2D -> {
                    if (isShiftDown) {
                        setValue(Vector2f(vx, vy + dy0 / 5), true)
                    } else {
                        setValue(Vector2f(vx + dx0 / 5, vy), true)
                    }
                }
                else -> {// universal version, just scaling
                    val scale = pow(1.1f, delta)
                    setValue(Vector4f(vx * scale, vy * scale, vz * scale, vw * scale), true)
                }
            }
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun onEmpty(x: Float, y: Float) {
        val defaultValue = owningProperty?.defaultValue ?: type.defaultValue
        valueFields.forEachIndexed { index, pureTextInput ->
            pureTextInput.text = getDouble(defaultValue, index).toString()
        }
        changeListener(
            getDouble(defaultValue, 0),
            getDouble(defaultValue, 1),
            getDouble(defaultValue, 2),
            getDouble(defaultValue, 3)
        )
    }

    override fun getCursor(): Long = Cursor.drag

}