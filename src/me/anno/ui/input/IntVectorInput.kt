package me.anno.ui.input

import me.anno.animation.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.studio.StudioBase.Companion.warn
import me.anno.ui.Panel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.ColorParsing
import me.anno.utils.types.AnyToInt.getInt
import org.joml.*
import kotlin.math.roundToInt

open class IntVectorInput(
    style: Style, title: String,
    visibilityKey: String,
    val type: Type,
    val createComponent: () -> IntInput
) : TitledListY(title, visibilityKey, style), InputPanel<Vector4i> {

    constructor(style: Style, title: String, visibilityKey: String, type: Type) :
            this(style, title, visibilityKey, type, { IntInput(style, "", visibilityKey, type) })

    constructor(style: Style) : this(style, "", "", Type.INT)

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector2ic, type: Type
    ) : this(style, title, visibilityKey, type) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector3ic, type: Type
    ) : this(style, title, visibilityKey, type) {
        setValue(value, false)
    }

    constructor(
        style: Style, title: String, visibilityKey: String, value: Vector4ic, type: Type
    ) : this(style, title, visibilityKey, type) {
        setValue(value, false)
    }

    private val components: Int = type.components
    private val valueFields = ArrayList<IntInput>(components)

    private var resetListener: (() -> Any?)? = null

    val valueList = PanelListX(style)

    init {

        valueList.disableConstantSpaceForWeightedChildren = true

        if (type == Type.COLOR) warn("VectorInput should be replaced with ColorInput for type color!")

        valueList += WrapAlign.TopFill
        this += valueList
        if (titleView != null) valueList.hide()

    }

    override var isInputAllowed: Boolean
        get() = valueFields.first().isInputAllowed
        set(value) {
            titleView?.setTextAlpha(if(value) 1f else 0.5f)
            for(child in valueFields){
                child.isInputAllowed = value
            }
        }

    override val lastValue: Vector4i
        get() = Vector4i(
            compX.lastValue.toInt(),
            compY?.lastValue?.toInt() ?: 0,
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

    private fun addComponent(title: String): IntInput {
        val component = createComponent()
        component.inputPanel.tooltip = title
        component.setChangeListener {
            onChange()
        }
        valueList += component.setWeight(1f)
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
            is Vector4fc -> setValue(
                (255 * color.x()).roundToInt(),
                (255 * color.y()).roundToInt(),
                (255 * color.z()).roundToInt(),
                (255 * color.w()).roundToInt(), true
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

    val compX = addComponent("x")
    val compY = if (components > 1) addComponent("y") else null
    val compZ = if (components > 2) addComponent("z") else null
    val compW = if (components > 3) addComponent("w") else null

    val vx get() = compX.lastValue
    val vy get() = compY?.lastValue ?: 0L
    val vz get() = compZ?.lastValue ?: 0L
    val vw get() = compW?.lastValue ?: 0L

    fun setValue(v: Vector2ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
    }

    fun setValue(v: Vector3ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
    }

    fun setValue(v: Vector4ic, notify: Boolean) {
        compX.setValue(v.x(), notify)
        compY?.setValue(v.y(), notify)
        compZ?.setValue(v.z(), notify)
        compW?.setValue(v.w(), notify)
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

    val changeListeners = ArrayList<(x: Long, y: Long, z: Long, w: Long) -> Unit>()

    fun addChangeListener(listener: (x: Long, y: Long, z: Long, w: Long) -> Unit): IntVectorInput {
        changeListeners += listener
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
        if (resetListener == null) {
            onEmpty2(type.defaultValue)
        } else {
            onEmpty2(resetListener() ?: 0)
        }
    }

    fun onChange() {
        for (changeListener in changeListeners) {
            changeListener(vx, vy, vz, vw)
        }
    }

    fun onEmpty2(defaultValue: Any) {
        valueFields.forEachIndexed { index, pureTextInput ->
            pureTextInput.setValue(getInt(defaultValue, index), false)
        }
        if (resetListener == null) {
            onChange()
        }
    }

    override fun getCursor(): Long = Cursor.drag

    override fun clone(): IntVectorInput {
        val clone = IntVectorInput(style, title, visibilityKey, type, createComponent)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as IntVectorInput
        // only works if there are no hard references
        clone.changeListeners.clear()
        clone.changeListeners.addAll(changeListeners)
        clone.resetListener = resetListener
        clone.setValue(vx, vy, vz, vw, false)
    }

    override val className: String = "IntVectorInput"

}