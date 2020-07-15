package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.io.text.TextReader
import me.anno.objects.Camera
import me.anno.utils.clamp
import me.anno.utils.pow
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style
import me.anno.utils.get
import me.anno.utils.warn
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.math.max

class VectorInput(
    style: Style, var title: String,
    val type: AnimatedProperty.Type,
    val owningProperty: AnimatedProperty<*>? = null
): PanelListY(style){

    constructor(title: String, property: AnimatedProperty<*>, time: Double, style: Style):
            this(style, title, property.type, property){
        when(val value = property[time]){
            is Float -> setValue(value)
            is Vector2f -> setValue(value)
            is Vector3f -> setValue(value)
            is Vector4f -> setValue(value)
            is Quaternionf -> setValue(value)
            else -> throw RuntimeException("Type $value not yet supported!")
        }
    }

    constructor(style: Style, title: String, value: Vector2f, type: AnimatedProperty.Type,
                owningProperty: AnimatedProperty<*>? = null): this(style, title, type, owningProperty){
        setValue(value)
    }

    constructor(style: Style, title: String, value: Vector3f, type: AnimatedProperty.Type,
                owningProperty: AnimatedProperty<*>? = null): this(style, title, type, owningProperty){
        setValue(value)
    }

    constructor(style: Style, title: String, value: Vector4f, type: AnimatedProperty.Type,
                owningProperty: AnimatedProperty<*>? = null): this(style, title, type, owningProperty){
        setValue(value)
    }

    constructor(style: Style, title: String, value: Quaternionf,
                type: AnimatedProperty.Type = AnimatedProperty.Type.QUATERNION): this(style, title, type){
        setValue(value)
    }

    val components: Int = type.components
    val valueFields = ArrayList<PureTextInput>(components)

    fun addComponent(i: Int, title: String): FloatInput {
        val pseudo = FloatInput(style, title, type, owningProperty, i)
            .setChangeListener {
                changeListener(
                    compX.lastValue.toFloat(),
                    compY.lastValue.toFloat(),
                    compZ?.lastValue?.toFloat() ?: 0f,
                    compW?.lastValue?.toFloat() ?: 0f)
            }
        // titleList += pseudo.titlePanel.setWeight(1f)
        val input = pseudo.inputPanel
        valueList += input.setWeight(1f)
        valueFields += input
        return pseudo
    }

    // val titleList = PanelListX(style)
    val valueList = PanelListX(style)
    init {
        valueList.visibility = Visibility.GONE
        valueList.disableConstantSpaceForWeightedChildren = true
    }
    val titleView = object: TextPanel(title, style){

        override fun onMouseDown(x: Float, y: Float, button: Int) { this@VectorInput.onMouseDown(x,y,button) }
        override fun onMouseUp(x: Float, y: Float, button: Int) { this@VectorInput.onMouseUp(x,y,button) }
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) { this@VectorInput.onMouseMoved(x,y,dx,dy) }
        override fun onCopyRequested(x: Float, y: Float): String? =
            owningProperty?.toString() ?:
            "[${compX.lastValue}, ${compY.lastValue}, ${compZ?.lastValue ?: 0f}, ${compW?.lastValue ?: 0f}]"

        override fun onPaste(x: Float, y: Float, data: String, type: String) {
            val allComponents = data.toDoubleOrNull()
            if(allComponents != null){
                compX.setValue(allComponents)
                compY.setValue(allComponents)
                compZ?.setValue(allComponents)
                compW?.setValue(allComponents)
            } else {
                // parse vector
                if(data.startsWith("[") && data.endsWith("]") && data.indexOf('{') < 0){
                    val values = data.substring(1, data.lastIndex).split(',').map { it.trim().toDoubleOrNull() }
                    if(values.size in 1 .. 4){
                        values[0]?.apply { compX.setValue(this) }
                        values[1]?.apply { compY.setValue(this) }
                        values.getOrNull(2)?.apply { compZ?.setValue(this) }
                        values.getOrNull(3)?.apply { compW?.setValue(this) }
                    }
                } else {
                    try {
                        val animProperty = TextReader.fromText(data).firstOrNull() as? AnimatedProperty<*>
                        if(animProperty != null){
                            if(owningProperty != null){
                                owningProperty.copyFrom(animProperty)
                                when(val value = owningProperty[editorTime]){
                                    is Float -> setValue(value)
                                    is Vector2f -> setValue(value)
                                    is Vector3f -> setValue(value)
                                    is Vector4f -> setValue(value)
                                    is Quaternionf -> setValue(value)
                                    else -> warn("Unknown pasted data type $value")
                                }
                            } else {
                                // get the default value? no, the current value? yes.
                                setValue(Vector4f(
                                    animProperty[editorTime]!![0, vx],
                                    animProperty[editorTime]!![1, vy],
                                    animProperty[editorTime]!![2, vz],
                                    animProperty[editorTime]!![3, vw])
                                )
                            }
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }.setWeight(1f)

    init {

        if(type == AnimatedProperty.Type.COLOR) warn("VectorInput should be replaced with ColorInput for type color!")

        // titleList += WrapAlign.Top
        valueList += WrapAlign.Top

        this += titleView
        // this += titleList
        this += valueList

    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val focused1 = titleView.isInFocus || valueList.children.count { it.isInFocus } > 0
        if(focused1) isSelectedListener?.invoke()
        val focused2 = focused1 || owningProperty == Studio.selectedProperty
        valueList.visibility = if(focused2) Visibility.VISIBLE else Visibility.GONE
        super.draw(x0, y0, x1, y1)
        compX.updateValueMaybe()
        compY.updateValueMaybe()
        compZ?.updateValueMaybe()
        compW?.updateValueMaybe()
    }

    val compX = addComponent(0, "x")
    val compY = addComponent(1, "y")
    val compZ = if(components > 2) addComponent(2, "z") else null
    val compW = if(components > 3) addComponent(3, "w") else null

    val vx get() = compX.lastValue.toFloat()
    val vy get() = compY.lastValue.toFloat()
    val vz get() = compZ!!.lastValue.toFloat()
    val vw get() = compW!!.lastValue.toFloat()

    fun setValue(v: Float){
        compX.setValue(v)
    }

    fun setValue(v: Vector2f){
        compX.setValue(v.x)
        compY.setValue(v.y)
    }

    fun setValue(v: Vector3f){
        compX.setValue(v.x)
        compY.setValue(v.y)
        compZ?.setValue(v.z)
    }

    fun setValue(v: Vector4f){
        compX.setValue(v.x)
        compY.setValue(v.y)
        compZ?.setValue(v.z)
        compW?.setValue(v.w)
    }

    fun setValue(v: Quaternionf){
        compX.setValue(v.x)
        compY.setValue(v.y)
        compZ?.setValue(v.z)
        compW?.setValue(v.w)
    }

    var changeListener: (x: Float, y: Float, z: Float, w: Float) -> Unit = {
        _,_,_,_ ->
    }

    fun setChangeListener(listener: (x: Float, y: Float, z: Float, w: Float) -> Unit): VectorInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): VectorInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    var mouseIsDown = false
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if(mouseIsDown){
            val size = (if(isShiftDown) 4f else 20f) * (if(Studio.selectedTransform is Camera) -1f else 1f) / max(GFX.width,GFX.height)
            val dx0 = dx*size
            val dy0 = dy*size
            val delta = dx0-dy0
            when(type){
                AnimatedProperty.Type.POSITION -> {
                    val scaleFactor = 0.2f
                    setValue(Vector3f(vx + dx0 * scaleFactor, vy - dy0 * scaleFactor, vz))
                }
                AnimatedProperty.Type.ROT_YXZ -> {
                    val scaleFactor = 20f
                    if(isControlDown){
                        setValue(Vector3f(vx, vy, vz + delta * scaleFactor))
                    } else {
                        setValue(Vector3f(vx + dy0 * scaleFactor, vy + dx0 * scaleFactor, vz))
                    }
                }
                AnimatedProperty.Type.SCALE -> {
                    val scaleFactor = 1.03f
                    if(isControlDown){
                        val scaleX = pow(scaleFactor, dx0)
                        val scaleY = pow(scaleFactor, -dy0)
                        setValue(Vector3f(vx * scaleX, vy * scaleY, vz))
                    } else {
                        val scale = pow(scaleFactor, delta)
                        setValue(Vector3f(vx * scale, vy * scale, vz * scale))
                    }
                }
                AnimatedProperty.Type.COLOR -> {
                    val scaleFactor = 1.10f
                    val scale = pow(scaleFactor, delta)
                    if(isControlDown){
                        setValue(Vector4f(vx * scale, vy * scale, vz * scale, vw))
                    } else {
                        setValue(Vector4f(vx, vy, vz, clamp(vw + delta, 0f, 1f)))
                    }
                }
                AnimatedProperty.Type.SKEW_2D -> {
                    if(isShiftDown){
                        setValue(Vector2f(vx, vy + dy0/5))
                    } else {
                        setValue(Vector2f(vx + dx0/5, vy))
                    }
                }
            }
            // setValue(lastValue * pow(1.01f, delta))
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun onEmpty(x: Float, y: Float) {
        val defaultValue = type.defaultValue
        valueFields.forEachIndexed { index, pureTextInput ->
            pureTextInput.text = defaultValue[index].toString()
        }
        changeListener(defaultValue[0], defaultValue[1], defaultValue[2], defaultValue[3])
    }

    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true
    override fun getClassName() = "VectorInput"

}