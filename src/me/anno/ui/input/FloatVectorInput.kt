package me.anno.ui.input

import me.anno.animation.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.MouseButton
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.studio.StudioBase.Companion.warn
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.style.Style
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.Quaternions.toEulerAnglesDegrees
import org.joml.*
import kotlin.math.max

open class FloatVectorInput(
    title: String,
    visibilityKey: String,
    val type: Type,
    style: Style,
    val createComponent: () -> FloatInput
) : TitledListY(title, visibilityKey, style), InputPanel<Vector4d>, TextStyleable {

    constructor(title: String, visibilityKey: String, type: Type, style: Style) :
            this(title, visibilityKey, type, style, { FloatInput(style, "", visibilityKey, type) })

    constructor(style: Style) : this("", "", Type.FLOAT, style)

    constructor(
        title: String, visibilityKey: String, value: Quaternionf,
        type: Type = Type.QUATERNION, style: Style
    ) : this(title, visibilityKey, type, style) {
        if (type.components == 3) {
            // if type is Type.ROT_YXZ, we need to transform the value to angles, and to degrees
            val value2 = value.toEulerAnglesDegrees()
            setValue(value2, false)
        } else {
            setValue(value, false)
        }
    }

    constructor(
        title: String, visibilityKey: String, value: Vector2fc,
        type: Type = Type.VEC2, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector3fc,
        type: Type = Type.VEC3, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector4fc,
        type: Type = Type.VEC4, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector2dc,
        type: Type = Type.VEC2D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector3dc,
        type: Type = Type.VEC3D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector4dc,
        type: Type = Type.VEC4D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    private val components: Int = type.components
    private val valueFields = ArrayList<FloatInput>(components)

    private var resetListener: (() -> Any?)? = null

    val valueList = object : PanelListX(style) {
        override var visibility: Visibility
            get() = InputVisibility[visibilityKey]
            set(_) {}

        override fun onEnterKey(x: Float, y: Float) {
            this@FloatVectorInput.onEnterKey(x, y)
        }

        override fun onEmpty(x: Float, y: Float) {
            this@FloatVectorInput.onEmpty(x, y)
        }
    }

    init {
        valueList.disableConstantSpaceForWeightedChildren = true
    }

    init {

        if (type == Type.COLOR) warn("VectorInput should be replaced with ColorInput for type color!")

        valueList += WrapAlign.TopFill

        this += valueList
        if (titleView != null) valueList.hide()

    }

    override val lastValue: Vector4d
        get() = Vector4d(
            compX.lastValue,
            compY?.lastValue ?: 0.0,
            compZ?.lastValue ?: 0.0,
            compW?.lastValue ?: 0.0
        )

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

    override fun setValue(value: Vector4d, notify: Boolean): FloatVectorInput {
        setValue(value as Vector4dc, notify)
        return this
    }

    private fun addComponent(title: String): FloatInput {
        val component = createComponent()
        component.inputPanel.tooltip = title
        component.setChangeListener { onChange() }
        valueList += component.setWeight(1f)
        valueFields += component
        return component
    }

    fun setResetListener(listener: (() -> Any?)?) {
        resetListener = listener
    }

    override fun onCopyRequested(x: Float, y: Float) =
        "[${compX.lastValue}, ${compY?.lastValue ?: 0f}, ${compZ?.lastValue ?: 0f}, ${compW?.lastValue ?: 0f}]"

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        pasteVector(data)
            ?: pasteScalar(data)
            ?: pasteColor(data)
            ?: super.onPaste(x, y, data, type)
    }

    fun pasteVector(data: String): Unit? {
        return if (data.startsWith("[") && data.endsWith("]") && data.indexOf('{') < 0) {
            val values = data.substring(1, data.lastIndex).split(',').map { it.trim().toDoubleOrNull() }
            if (values.size in 1..4) {
                values[0]?.apply { compX.setValue(this, true) }
                values.getOrNull(2)?.apply { compY?.setValue(this, true) }
                values.getOrNull(2)?.apply { compZ?.setValue(this, true) }
                values.getOrNull(3)?.apply { compW?.setValue(this, true) }
                Unit
            } else null
        } else null
    }

    fun pasteColor(data: String): Unit? {
        return when (val color = ColorParsing.parseColorComplex(data)) {
            is Int -> setValue(color.toVecRGBA(), true)
            is Vector4f -> setValue(color, true)
            else -> null
        }
    }

    fun pasteScalar(data: String): Unit? {
        val allComponents = data.toDoubleOrNull()
        return if (allComponents != null && allComponents.isFinite()) {
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

    val compX: FloatInput = addComponent("x")
    val compY: FloatInput? = if (components > 1) addComponent("y") else null
    val compZ: FloatInput? = if (components > 2) addComponent("z") else null
    val compW: FloatInput? = if (components > 3) addComponent("w") else null

    val vx get() = compX.lastValue.toFloat()
    val vy get() = compY?.lastValue?.toFloat() ?: 0f
    val vz get() = compZ?.lastValue?.toFloat() ?: 0f
    val vw get() = compW?.lastValue?.toFloat() ?: 0f

    val vxd get() = compX.lastValue
    val vyd get() = compY?.lastValue ?: 0.0
    val vzd get() = compZ?.lastValue ?: 0.0
    val vwd get() = compW?.lastValue ?: 0.0

    fun setValue(v: Vector2fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4fc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Quaternionfc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Vector2dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4dc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(v: Quaterniondc, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(vi: FloatVectorInput, notify: Boolean) {
        compX.setValue(vi.vx, notify)
        compY?.setValue(vi.vy, notify)
        compZ?.setValue(vi.vz, notify)
        compW?.setValue(vi.vw, notify)
    }

    var changeListener: (x: Double, y: Double, z: Double, w: Double) -> Unit = { _, _, _, _ ->
    }

    fun onChange() {
        changeListener(
            compX.lastValue,
            compY?.lastValue ?: 0.0,
            compZ?.lastValue ?: 0.0,
            compW?.lastValue ?: 0.0
        )
    }

    fun setChangeListener(listener: (x: Double, y: Double, z: Double, w: Double) -> Unit): FloatVectorInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): FloatVectorInput {
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
            val size = 20f * shiftSlowdown / max(GFX.width, GFX.height)
            val dx0 = dx * size
            val dy0 = dy * size
            val delta = dx0 - dy0
            when (type) {
                Type.POSITION -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz), true)
                }
                Type.VEC3 -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz), true)
                }
                Type.VEC3D -> {
                    val scaleFactor = 0.2
                    setValue(Vector3d(vxd + dx0 * scaleFactor, vyd - dy0 * scaleFactor, vzd), true)
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
                Type.ROT_YXZ64 -> {
                    val scaleFactor = 20.0
                    if (isControlDown) {
                        setValue(Vector3d(vxd, vyd, vzd + delta * scaleFactor), true)
                    } else {
                        setValue(Vector3d(vxd + dy0 * scaleFactor, vyd + dx0 * scaleFactor, vzd), true)
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
        val resetListener = resetListener
        if (resetListener == null) {
            onEmpty2(type.defaultValue)
        } else {
            // onChange is not required, and wrong, because we set a listener, so we need to handle this ourselves
            // also we decided the value ourselves, so we know the value
            when (val value = resetListener()) {
                is Quaternionfc -> {
                    if (type.components == 3) {
                        val comp = value.toEulerAnglesDegrees()
                        valueFields[0].setValue(comp.x, false)
                        valueFields[1].setValue(comp.y, false)
                        valueFields[2].setValue(comp.z, false)
                    } else {
                        valueFields[0].setValue(value.x(), false)
                        valueFields[1].setValue(value.y(), false)
                        valueFields[2].setValue(value.z(), false)
                        valueFields[3].setValue(value.w(), false)
                    }
                }
                is Quaterniondc -> {
                    if (type.components == 3) {
                        val comp = value.toEulerAnglesDegrees()
                        valueFields[0].setValue(comp.x, false)
                        valueFields[1].setValue(comp.y, false)
                        valueFields[2].setValue(comp.z, false)
                    } else {
                        valueFields[0].setValue(value.x(), false)
                        valueFields[1].setValue(value.y(), false)
                        valueFields[2].setValue(value.z(), false)
                        valueFields[3].setValue(value.w(), false)
                    }
                }
                else -> onEmpty2(value ?: type.defaultValue)
            }
        }

    }

    fun onEmpty2(defaultValue: Any) {
        valueFields.forEachIndexed { index, pureTextInput ->
            val double = getDouble(defaultValue, index)
            pureTextInput.setValue(double, false)// = double.toString()
        }
        if (resetListener == null) {
            onChange()
        }// else:
        // onChange is not required, and wrong, because we set a listener, so we need to handle this ourselves
        // also we decided the value ourselves, so we know the value
    }

    override fun getCursor(): Long = Cursor.drag

    override fun clone(): FloatVectorInput {
        val clone = FloatVectorInput(title, visibilityKey, type, style, createComponent)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as FloatVectorInput
        // only works if there are no hard references
        clone.changeListener = changeListener
        clone.resetListener = resetListener
        clone.setValue(Vector4d(vxd, vyd, vzd, vwd), false)
    }

    override val className: String = "FloatVectorInput"

}