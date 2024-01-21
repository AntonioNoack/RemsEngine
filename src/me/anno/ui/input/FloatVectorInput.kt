package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase.Companion.shiftSlowdown
import me.anno.gpu.Cursor
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isLeftDown
import me.anno.input.Input.isShiftDown
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.VectorInputList
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing
import me.anno.utils.types.AnyToDouble.getDouble
import org.apache.logging.log4j.LogManager
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.max

open class FloatVectorInput(
    title: String,
    visibilityKey: String,
    val type: NumberType,
    style: Style,
    val createComponent: () -> FloatInput
) : TitledListY(title, visibilityKey, style), InputPanel<Vector4d>, TextStyleable {

    companion object {
        private val LOGGER = LogManager.getLogger(FloatVectorInput::class)
    }

    constructor(title: String, visibilityKey: String, type: NumberType, style: Style) :
            this(title, visibilityKey, type, style, { FloatInput("", visibilityKey, type, style) })

    constructor(style: Style) : this("", "", NumberType.FLOAT, style)

    constructor(
        title: String, visibilityKey: String, value: Quaternionf,
        type: NumberType = NumberType.QUATERNION, style: Style
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
        title: String, visibilityKey: String, value: Vector2f,
        type: NumberType = NumberType.VEC2, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector3f,
        type: NumberType = NumberType.VEC3, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector4f,
        type: NumberType = NumberType.VEC4, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Planef,
        type: NumberType = NumberType.PLANE4, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Planed,
        type: NumberType = NumberType.PLANE4D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector2d,
        type: NumberType = NumberType.VEC2D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector3d,
        type: NumberType = NumberType.VEC3D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, false)
    }

    constructor(
        title: String, visibilityKey: String, value: Vector4d,
        type: NumberType = NumberType.VEC4D, style: Style
    ) : this(title, visibilityKey, type, style) {
        setValue(value, -1, false)
    }

    private val components: Int = type.components
    private val valueFields = ArrayList<FloatInput>(components)

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

    override val value: Vector4d
        get() = Vector4d(
            compX.value,
            compY?.value ?: 0.0,
            compZ?.value ?: 0.0,
            compW?.value ?: 0.0
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

    private fun addComponent(title: String, index: Int): FloatInput {
        val component = createComponent()
        component.inputPanel.indexInProperty = index
        component.inputPanel.tooltip = title
        component.setChangeListener { onChange(1 shl index) }
        component.weight = 1f
        component.setResetListener {
            when (val vector = type.defaultValue) {
                // not sure about these...
                is Quaternionf -> vector.toEulerAnglesDegrees()[index]
                is Quaterniond -> vector.toEulerAnglesDegrees()[index]
                else -> getDouble(vector, index, 0.0)
            }.toString()
        }
        valueList += component
        valueFields += component
        return component
    }

    fun setResetListener(listener: (() -> Any?)?): FloatVectorInput {
        resetListener = listener
        return this
    }

    override fun onCopyRequested(x: Float, y: Float) = "[$vxd, $vyd, $vzd, $vwd]"

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
                values[0]?.apply { compX.setValue(this, -1, true) }
                values.getOrNull(2)?.apply { compY?.setValue(this, -1, true) }
                values.getOrNull(2)?.apply { compZ?.setValue(this, -1, true) }
                values.getOrNull(3)?.apply { compW?.setValue(this, -1, true) }
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
        val scalar = data.toDoubleOrNull()
        return if (scalar != null && scalar.isFinite()) {
            compX.setValue(scalar, -1, true)
            compY?.setValue(scalar, -1, true)
            compZ?.setValue(scalar, -1, true)
            compW?.setValue(scalar, -1, true)
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

    val vx get() = compX.value.toFloat()
    val vy get() = compY?.value?.toFloat() ?: 0f
    val vz get() = compZ?.value?.toFloat() ?: 0f
    val vw get() = compW?.value?.toFloat() ?: 0f

    val vxd get() = compX.value
    val vyd get() = compY?.value ?: 0.0
    val vzd get() = compZ?.value ?: 0.0
    val vwd get() = compW?.value ?: 0.0

    fun setValue(v: Vector2f, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
    }

    fun setValue(v: Vector3f, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
        compZ?.setValue(v.z, notify)
    }

    fun setValue(v: Vector4f, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
        compZ?.setValue(v.z, notify)
        compW?.setValue(v.w, notify)
    }

    fun setValue(v: Planef, notify: Boolean) {
        compX.setValue(v.dirX, notify)
        compY?.setValue(v.dirY, notify)
        compZ?.setValue(v.dirZ, notify)
        compW?.setValue(v.distance, notify)
    }

    fun setValue(v: Quaternionf, notify: Boolean) {
        compX.setValue(v.x, notify)
        compY?.setValue(v.y, notify)
        compZ?.setValue(v.z, notify)
        compW?.setValue(v.w, notify)
    }

    fun setValue(v: Vector2d, notify: Boolean) {
        compX.setValue(v.x, -1, notify)
        compY?.setValue(v.y, -1, notify)
    }

    fun setValue(v: Vector3d, notify: Boolean) {
        compX.setValue(v.x, -1, notify)
        compY?.setValue(v.y, -1, notify)
        compZ?.setValue(v.z, -1, notify)
    }

    final override fun setValue(newValue: Vector4d, mask: Int, notify: Boolean): Panel {
        compX.setValue(newValue.x, -1, notify && mask.hasFlag(1))
        compY?.setValue(newValue.y, -1, notify && mask.hasFlag(2))
        compZ?.setValue(newValue.z, -1, notify && mask.hasFlag(4))
        compW?.setValue(newValue.w, -1, notify && mask.hasFlag(8))
        return this
    }

    fun setValue(v: Planed, notify: Boolean) {
        compX.setValue(v.dirX, -1, notify)
        compY?.setValue(v.dirY, -1, notify)
        compZ?.setValue(v.dirZ, -1, notify)
        compW?.setValue(v.distance, -1, notify)
    }

    fun setValue(v: Quaterniond, notify: Boolean) {
        compX.setValue(v.x, -1, notify)
        compY?.setValue(v.y, -1, notify)
        compZ?.setValue(v.z, -1, notify)
        compW?.setValue(v.w, -1, notify)
    }

    fun setValue(vi: FloatVectorInput, notify: Boolean) {
        compX.setValue(vi.vx, notify)
        compY?.setValue(vi.vy, notify)
        compZ?.setValue(vi.vz, notify)
        compW?.setValue(vi.vw, notify)
    }

    val changeListeners = ArrayList<(x: Double, y: Double, z: Double, w: Double, mask: Int) -> Unit>()

    fun onChange(mask: Int) {
        for (changeListener in changeListeners)
            changeListener(
                compX.value,
                compY?.value ?: 0.0,
                compZ?.value ?: 0.0,
                compW?.value ?: 0.0,
                mask
            )
    }

    fun addChangeListener(listener: (x: Double, y: Double, z: Double, w: Double, mask: Int) -> Unit): FloatVectorInput {
        changeListeners.add(listener)
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): FloatVectorInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (isLeftDown && isAnyChildInFocus && InputVisibility[visibilityKey]) {
            val ws = windowStack
            val size = 20f * shiftSlowdown / max(ws.width, ws.height)
            val dx0 = dx * size
            val dy0 = dy * size
            val delta = dx0 - dy0
            when (type) {
                NumberType.POSITION -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz), true)
                }
                NumberType.VEC3 -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz), true)
                }
                NumberType.VEC3D -> {
                    val scaleFactor = 0.2
                    setValue(Vector3d(vxd + dx0 * scaleFactor, vyd - dy0 * scaleFactor, vzd), true)
                }
                NumberType.POSITION_2D -> {
                    val scaleFactor = 0.2f
                    setValue(Vector2f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor), true)
                }
                NumberType.ROT_YXZ -> {
                    val scaleFactor = 20f
                    if (isControlDown) {
                        setValue(Vector3f(vx, vy, vz + delta * scaleFactor), true)
                    } else {
                        setValue(Vector3f(vx + dy0 * scaleFactor, vy + dx0 * scaleFactor, vz), true)
                    }
                }
                NumberType.ROT_YXZ64 -> {
                    val scaleFactor = 20.0
                    if (isControlDown) {
                        setValue(Vector3d(vxd, vyd, vzd + delta * scaleFactor), true)
                    } else {
                        setValue(Vector3d(vxd + dy0 * scaleFactor, vyd + dx0 * scaleFactor, vzd), true)
                    }
                }
                NumberType.SCALE -> {
                    val scaleFactor = 1.03f
                    if (isControlDown) {
                        val scaleX = pow(scaleFactor, dx0)
                        val scaleY = pow(scaleFactor, -dy0)
                        setValue(Vector3f(vx * scaleX, vy * scaleY, vz * scaleX), true)
                    } else {
                        val scale = pow(scaleFactor, delta)
                        setValue(Vector3f(vx * scale, vy * scale, vz * scale), true)
                    }
                }
                NumberType.COLOR -> {
                    val scaleFactor = 1.10f
                    val scale = pow(scaleFactor, delta)
                    if (isControlDown) {
                        setValue(Vector4f(vx * scale, vy * scale, vz * scale, vw), true)
                    } else {
                        setValue(Vector4f(vx, vy, vz, clamp(vw + delta, 0f, 1f)), true)
                    }
                }
                NumberType.SKEW_2D -> {
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

    override fun wantsMouseTeleport(): Boolean = true

    override fun onEmpty(x: Float, y: Float) {
        val resetListener = resetListener
        if (resetListener == null) {
            onEmpty2(type.defaultValue)
        } else {
            // onChange is not required, and wrong, because we set a listener, so we need to handle this ourselves
            // also we decided the value ourselves, so we know the value
            when (val value = resetListener()) {
                is Quaternionf -> {
                    if (type.components == 3) {
                        val comp = value.toEulerAnglesDegrees()
                        valueFields[0].setValue(comp.x, false)
                        valueFields[1].setValue(comp.y, false)
                        valueFields[2].setValue(comp.z, false)
                    } else {
                        valueFields[0].setValue(value.x, false)
                        valueFields[1].setValue(value.y, false)
                        valueFields[2].setValue(value.z, false)
                        valueFields[3].setValue(value.w, false)
                    }
                }
                is Quaterniond -> {
                    if (type.components == 3) {
                        val comp = value.toEulerAnglesDegrees()
                        valueFields[0].setValue(comp.x, -1, false)
                        valueFields[1].setValue(comp.y, -1, false)
                        valueFields[2].setValue(comp.z, -1, false)
                    } else {
                        valueFields[0].setValue(value.x, -1, false)
                        valueFields[1].setValue(value.y, -1, false)
                        valueFields[2].setValue(value.z, -1, false)
                        valueFields[3].setValue(value.w, -1, false)
                    }
                }
                else -> onEmpty2(value ?: type.defaultValue)
            }
        }
    }

    fun onEmpty2(defaultValue: Any) {
        for (index in valueFields.indices) {
            valueFields[index].setValue(getDouble(defaultValue, index, 0.0), -1, false)
        }
        if (resetListener == null) {
            onChange(-1)
        }// else:
        // onChange is not required, and wrong, because we set a listener, so we need to handle this ourselves
        // also we decided the value ourselves, so we know the value
    }

    override fun getCursor(): Cursor = Cursor.drag

    override fun clone(): FloatVectorInput {
        val clone = FloatVectorInput(title, visibilityKey, type, style, createComponent)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as FloatVectorInput
        // only works if there are no hard references
        dst.changeListeners.clear()
        dst.changeListeners.addAll(changeListeners)
        dst.resetListener = resetListener
        dst.setValue(Vector4d(vxd, vyd, vzd, vwd), -1, false)
    }

    override val className: String get() = "FloatVectorInput"
}