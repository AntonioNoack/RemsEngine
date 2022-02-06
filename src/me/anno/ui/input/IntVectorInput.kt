package me.anno.ui.input

import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.io.text.TextReader
import me.anno.studio.StudioBase.Companion.warn
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.RemsStudio.editorTime
import me.anno.remsstudio.Selection.selectedProperty
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.input.components.VectorInputIntComponent
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.ColorParsing
import me.anno.utils.types.AnyToInt.getInt
import org.joml.*
import kotlin.math.roundToInt

class IntVectorInput(
    style: Style, title: String,
    visibilityKey: String,
    val type: Type,
    private val owningProperty: AnimatedProperty<*>? = null
) : TitledListY(title, visibilityKey, style), InputPanel<Vector4i> {

    constructor(style: Style) : this(style, "", "", Type.INT, null)

    constructor(title: String, visibilityKey: String, property: AnimatedProperty<*>, time: Double, style: Style) :
            this(style, title, visibilityKey, property.type, property) {
        when (val value = property[time]) {
            is Vector2i -> setValue(value, false)
            is Vector3i -> setValue(value, false)
            is Vector4i -> setValue(value, false)
            else -> throw RuntimeException("Type $value not yet supported!")
        }
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector2ic, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector3ic, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector4ic, type: Type,
        owningProperty: AnimatedProperty<*>? = null
    ) : this(style, title, visibilityKey, type, owningProperty) {
        setValue(value, false)
    }

    private val components: Int = type.components
    private val valueFields = ArrayList<IntInput>(components)

    private var resetListener: (() -> Any?)? = null

    private val valueList = PanelListX(style)

    init {

        if (type == Type.COLOR) warn("VectorInput should be replaced with ColorInput for type color!")

        valueList += WrapAlign.TopFill
        this += valueList

    }

    override val lastValue: Vector4i
        get() = Vector4i(
            compX.lastValue.toInt(),
            compY.lastValue.toInt(),
            compZ?.lastValue?.toInt() ?: 0,
            compW?.lastValue?.toInt() ?: 0
        )

    override fun setValue(value: Vector4i, notify: Boolean): Panel {
        setValue(value as Vector4ic, notify)
        return this
    }

    fun setResetListener(listener: (() -> Any?)?) {
        resetListener = listener
    }

    private fun addComponent(i: Int, title: String): IntInput {
        val pseudo = VectorInputIntComponent(this.title, visibilityKey, type, owningProperty, i, this, style)
        pseudo.inputPanel.tooltip = title
        valueList += pseudo.setWeight(1f)
        valueFields += pseudo
        return pseudo
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
            val values = data.substring(1, data.lastIndex).split(',').map { it.trim().toIntOrNull() }
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
            is Int -> setValue(color.r(), color.g(), color.b(), color.a(), true)
            is Vector4fc -> setValue(
                (255 * color.x()).roundToInt(),
                (255 * color.y()).roundToInt(),
                (255 * color.z()).roundToInt(),
                (255 * color.w()).roundToInt(), true
            )
            else -> null
        }
    }

    private fun pasteScalar(data: String): Unit? {
        val allComponents = data.toIntOrNull()
        return if (allComponents != null) {
            compX.setValue(allComponents, true)
            compY.setValue(allComponents, true)
            compZ?.setValue(allComponents, true)
            compW?.setValue(allComponents, true)
            Unit
        } else null
    }

    private fun pasteAnimated(data: String): Unit? {
        return try {
            val editorTime = editorTime
            val animProperty = TextReader.read(data, true)
                .firstOrNull() as? AnimatedProperty<*>
            if (animProperty != null) {
                if (owningProperty != null) {
                    owningProperty.copyFrom(animProperty)
                    when (val value = owningProperty[editorTime]) {
                        is Vector2i -> setValue(value, true)
                        is Vector3i -> setValue(value, true)
                        is Vector4i -> setValue(value, true)
                        else -> warn("Unknown pasted data type $value")
                    }
                } else {
                    // get the default value? no, the current value? yes.
                    val atTime = animProperty[editorTime]!!
                    setValue(
                        getInt(atTime, 0, vx),
                        getInt(atTime, 1, vy),
                        getInt(atTime, 2, vz),
                        getInt(atTime, 3, vw),
                        true
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

    val vx get() = compX.lastValue.toInt()
    val vy get() = compY.lastValue.toInt()
    val vz get() = compZ?.lastValue?.toInt() ?: 0
    val vw get() = compW?.lastValue?.toInt() ?: 0

    fun setValue(v: Vector2ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
    }

    fun setValue(vi: IntVectorInput, notify: Boolean) {
        compX.setValue(vi.vx, notify)
        compY.setValue(vi.vy, notify)
        compZ?.setValue(vi.vz, notify)
        compW?.setValue(vi.vw, notify)
    }

    fun setValue(vx: Int, vy: Int, vz: Int, vw: Int, notify: Boolean) {
        compX.setValue(vx, notify)
        compY.setValue(vy, notify)
        compZ?.setValue(vz, notify)
        compW?.setValue(vw, notify)
    }

    var changeListener: (x: Int, y: Int, z: Int, w: Int) -> Unit = { _, _, _, _ ->
    }

    fun setChangeListener(listener: (x: Int, y: Int, z: Int, w: Int) -> Unit): IntVectorInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): IntVectorInput {
        isSelectedListener = listener
        return this
    }

    /*override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
       super.onMouseDown(x, y, button)
       mouseIsDown = true
   }

   var mouseIsDown = false
   override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
       super.onMouseMoved(x, y, dx, dy)
       if (mouseIsDown) {
          // todo scale like the int input does it
         val size = 20f * shiftSlowdown * (if (selectedTransform is Camera) -1f else 1f) / max(GFX.width, GFX.height)
          val dx0 = dx * size
          val dy0 = dy * size
          val delta = dx0 - dy0
          when (type) {
              else -> {// universal version, just scaling
                  val scale = pow(1.1f, delta)
                  setValue(Vector4i(vx * scale, vy * scale, vz * scale, vw * scale), true)
              }
          }
       }
   }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }*/

    override fun onEmpty(x: Float, y: Float) {
        val resetListener = resetListener
        if (owningProperty != null || resetListener == null) {
            onEmpty2(owningProperty?.defaultValue ?: type.defaultValue)
        } else {
            onEmpty2(resetListener() ?: 0)
        }
    }

    fun onChange() {
        changeListener(
            compX.lastValue.toInt(),
            compY.lastValue.toInt(),
            compZ?.lastValue?.toInt() ?: 0,
            compW?.lastValue?.toInt() ?: 0
        )
    }

    private fun onEmpty2(defaultValue: Any) {
        valueFields.forEachIndexed { index, pureTextInput ->
            pureTextInput.setValue(getInt(defaultValue, index), false)
        }
        if (resetListener == null) {
            onChange()
        }
    }

    override fun getCursor(): Long = Cursor.drag

    override fun clone(): IntVectorInput {
        val clone = IntVectorInput(style, title, visibilityKey, type, owningProperty)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as IntVectorInput
        // only works if there are no hard references
        clone.changeListener = changeListener
        clone.resetListener = resetListener
        clone.setValue(vx, vy, vz, vw, false)
    }

    override val className: String = "IntVectorInput"

}