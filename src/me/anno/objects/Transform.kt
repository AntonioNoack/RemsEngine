package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.blending.BlendMode
import me.anno.objects.blending.blendModes
import me.anno.objects.particles.ParticleSystem
import me.anno.studio.Studio
import me.anno.studio.Studio.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.*
import me.anno.ui.style.Style
import org.joml.*
import org.lwjgl.opengl.GL11.*
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger

// pivot? nah, always use the center to make things easy;
// or should we do it?... idk for sure...
// just make the tree work perfectly <3

// todo load 3D meshes :D
// gradients? -> can be done using the mask layer
// done select by clicking

// todo lock camera rotation?

open class Transform(var parent: Transform? = null): Saveable(), Inspectable {

    // todo generally "play" the animation of a single transform for testing purposes?
    // useful for audio, video, particle systems, generally animations
    // only available if the rest is stopped? yes.

    init {
        parent?.addChild(this)
    }

    val clickId = nextClickId.incrementAndGet()
    var isVisibleInTimeline = false

    var position = AnimatedProperty.pos()
    var scale = AnimatedProperty.scale()
    var rotationYXZ = AnimatedProperty.rotYXZ()
    var rotationQuaternion: AnimatedProperty<Quaternionf>? = null
    var skew = AnimatedProperty.skew()
    var color = AnimatedProperty.color()
    var colorMultiplier = AnimatedProperty.floatPlus().set(1f)

    var blendMode = BlendMode.UNSPECIFIED

    var timeOffset = 0.0
    var timeDilation = 1.0

    // todo make this animatable, calculate the integral to get a mapping
    var timeAnimated = AnimatedProperty.double()

    var name = getDefaultDisplayName()
    var comment = ""

    open fun getDefaultDisplayName() = if(getClassName() == "Transform") "Folder" else getClassName()

    val rightPointingTriangle = "▶"
    val bottomPointingTriangle = "▼"
    val folder = "\uD83D\uDCC1"

    val children = ArrayList<Transform>()
    var isCollapsed = false

    var lastLocalTime = 0.0

    var weight = 1f

    fun putValue(list: AnimatedProperty<*>, value: Any){
        list.addKeyframe(if(list.isAnimated) lastLocalTime else 0.0, value, 0.1)
    }

    val usesEuler get() = rotationQuaternion == null

    fun show(anim: AnimatedProperty<*>?){
        Studio.selectedProperty = anim
    }

    override fun createInspector(list: PanelListY, style: Style){

        // todo update by time :)

        list += TextInput("Name (${getClassName()})", style, name)
            .setChangeListener { name = if(it.isEmpty()) "-" else it }
            .setIsSelectedListener { show(null) }
        list += TextInputML("Comment", style, comment)
            .setChangeListener { comment = it }
            .setIsSelectedListener { show(null) }

        list += VI("Position", "Location of this object", position, style)
        list += VI("Scale", "Makes it bigger/smaller", scale, style)

        if(usesEuler){
            list += VI("Rotation (YXZ)", "", rotationYXZ, style)
        } else {
            list += VectorInput(style, "Rotation (Quaternion)", rotationQuaternion?.get(lastLocalTime) ?: Quaternionf())
                .setChangeListener { x, y, z, w ->
                    if(rotationQuaternion == null) rotationQuaternion = AnimatedProperty.quat()
                    putValue(rotationQuaternion!!, Quaternionf(x,y,z,w+1e-9f).normalize()) }
                .setIsSelectedListener { show(rotationQuaternion) }
        }

        list += VI("Skew", "Transform it similar to a shear", skew, style)
        list += VI("Color", "Tint, applied to this & children", color, style)
        list += VI("Color Multiplier", "To make things brighter than usually possible", colorMultiplier, style)
        list += VI("Start Time", "Delay the animation", null, timeOffset, style){ timeOffset = it }
        list += VI("Time Multiplier", "Speed up the animation", null, timeDilation, style){ timeDilation = it }
        list += VI("Advanced Time", "Add acceleration/deceleration to your elements", timeAnimated, style)
        list += EnumInput("Blend Mode", true, blendMode.id, blendModes.keys.toList().sorted(), style)
            .setChangeListener { blendMode = BlendMode[it] }
            .setIsSelectedListener { show(null) }

        if(parent?.acceptsWeight() == true){
            list += FloatInput("Weight", weight, AnimatedProperty.Type.FLOAT_PLUS, style)
                .setChangeListener {
                    weight = it.toFloat()
                    (parent as? ParticleSystem)?.apply {
                        if(children.size > 1) clearCache()
                    }
                }
                .setIsSelectedListener { show(null) }
        }
        list += BooleanInput("Visible In Timeline?", isVisibleInTimeline, style)
            .setChangeListener { isVisibleInTimeline = it }
            .setIsSelectedListener { show(null) }


    }

    fun getLocalTime(parentTime: Double): Double {
        var localTime0 = (parentTime - timeOffset) * timeDilation
        localTime0 += timeAnimated[localTime0]
        return localTime0
    }

    fun getLocalColor(): Vector4f = getLocalColor(parent?.getLocalColor() ?: Vector4f(1f,1f,1f,1f), lastLocalTime)
    fun getLocalColor(parentColor: Vector4f, time: Double): Vector4f {
        val col = color.getValueAt(time)
        val mul = colorMultiplier[time]
        return Vector4f(col).mul(parentColor).mul(mul, mul, mul, 1f)
    }

    fun applyTransformLT(transform: Matrix4f, time: Double){

        val position = position[time]
        val scale = scale[time]
        val euler = rotationYXZ[time]
        val rotationQuat = rotationQuaternion
        val usesEuler = usesEuler
        val skew = skew[time]

        if(position.x != 0f || position.y != 0f || position.z != 0f){
            transform.translate(position)
        }

        if(usesEuler){// y x z
            if(euler.y != 0f) transform.rotate(toRadians(euler.y), yAxis)
            if(euler.x != 0f) transform.rotate(toRadians(euler.x), xAxis)
            if(euler.z != 0f) transform.rotate(toRadians(euler.z), zAxis)
        } else {
            if(rotationQuat != null) transform.rotate(rotationQuat[time])
        }

        if(scale.x != 1f || scale.y != 1f || scale.z != 1f) transform.scale(scale)

        if(skew.x != 0f || skew.y != 0f) transform.mul3x3(// works
            1f, skew.y, 0f,
            skew.x, 1f, 0f,
            0f, 0f, 1f
        )

    }

    fun applyTransformPT(transform: Matrix4f, parentTime: Double) = applyTransformLT(transform, getLocalTime(parentTime))

    /**
     * stack with camera already included
     * */
    fun draw(stack: Matrix4fArrayList, parentTime: Double, parentColor: Vector4f){

        val time = getLocalTime(parentTime)
        val color = getLocalColor(parentColor, time)

        if(color.w > 0.00025f){ // 12 bit = 4k
            applyTransformLT(stack, time)
            GFX.drawnTransform = this
            onDraw(stack, time, color)
            if(drawChildrenAutomatically()){
                drawChildren(stack, time, color)
            }
        }

    }

    open fun drawChildrenAutomatically() = true

    fun drawChildren(stack: Matrix4fArrayList, time: Double, color: Vector4f){
        children.forEach { child ->
            drawChild(stack, time, color, child)
        }
    }

    fun drawChild(stack: Matrix4fArrayList, time: Double, color: Vector4f, child: Transform?){
        if(child != null){
            child.getParentBlendMode(BlendMode.DEFAULT).apply()
            stack.pushMatrix()
            child.draw(stack, time, color)
            stack.popMatrix()
        }
    }

    fun drawUICircle(stack: Matrix4fArrayList, scale: Float, inner: Float, color: Vector4f){
        // draw a small symbol to indicate pivot
        if(!GFX.isFinalRendering){
            if(scale != 1f){
                stack.pushMatrix()
                stack.scale(scale)
            }
            GFX.draw3DCircle(stack, inner, 0f, 360f, color, 1f)
            if(scale != 1f){
                stack.popMatrix()
            }
        }
    }

    open fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f){

        drawUICircle(stack, 0.02f, 0.7f, color)

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "parent", parent)
        writer.writeString("name", name)
        writer.writeObject(this, "position", position)
        writer.writeObject(this, "scale", scale)
        writer.writeObject(this, "rotationYXZ", rotationYXZ)
        writer.writeObject(this, "rotationQuat", rotationQuaternion)
        writer.writeObject(this, "skew", skew)
        writer.writeDouble("timeOffset", timeOffset)
        writer.writeDouble("timeDilation", timeDilation)
        writer.writeObject(this, "timeAnimated", timeAnimated)
        writer.writeObject(this, "color", color)
        writer.writeString("blendMode", blendMode.id)
        writer.writeList(this, "children", children)
        writer.writeBool("isVisibleInTimeline", isVisibleInTimeline, true)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "isVisibleInTimeline" -> isVisibleInTimeline = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "parent" -> {
                if(value is Transform){
                    value.addChild(this)
                }
            }
            "children" -> {
                if(value is Transform){
                    addChild(value)
                }
            }
            "position" -> position.copyFrom(value)
            "scale" -> scale.copyFrom(value)
            "rotationYXZ" -> rotationYXZ.copyFrom(value)
            "rotationQuat" -> {
                rotationQuaternion?.copyFrom(value) ?: {
                    if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.QUATERNION){
                        rotationQuaternion = value as AnimatedProperty<Quaternionf>
                    }
                }()
            }
            "skew" -> skew.copyFrom(value)
            "timeAnimated" -> timeAnimated.copyFrom(value)
            "color" -> color.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when(name){
            "timeDilation" -> timeDilation = value
            "timeOffset" -> timeOffset = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "name" -> this.name = value
            "blendMode" -> this.blendMode = BlendMode[value]
            else -> super.readString(name, value)
        }
    }

    fun contains(t: Transform): Boolean {
        if(t === this) return true
        if(children != null){// can be null on init
            for(child in children){
                if(child === t || child.contains(t)) return true
            }
        }
        return false
    }

    override fun getClassName(): String = "Transform"
    override fun getApproxSize(): Int = 50

    fun addChild(child: Transform){
        if(child.contains(this)) throw RuntimeException("this cannot contain its parent!")
        child.parent?.removeChild(child)
        child.parent = this
        children += child
    }

    fun removeChild(child: Transform){
        child.parent = null
        children.remove(child)
    }

    fun stringify(): String {
        val myParent = parent
        parent = null
        val data = TextWriter.toText(this, false)
        parent = myParent
        return data
    }

    fun setName(name: String): Transform {
        this.name = name
        return this
    }

    fun getGlobalTransform(time: Double): Pair<Matrix4f, Double> {
        val (parentTransform, parentTime) = parent?.getGlobalTransform(time) ?: Matrix4f() to time
        val localTime = getLocalTime(parentTime)
        applyTransformLT(parentTransform, localTime)
        return parentTransform to localTime
    }

    fun removeFromParent(){
        parent?.removeChild(this)
    }

    fun getParentBlendMode(default: BlendMode): BlendMode =
        if(blendMode == BlendMode.UNSPECIFIED) parent?.getParentBlendMode(default) ?: default else blendMode

    override fun isDefaultValue() = false

    fun clone() = TextWriter.toText(this, false).toTransform()
    open fun acceptsWeight() = false

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * callback is used to adjust the value
     * */
    @Suppress("UNCHECKED_CAST") // all casts are checked in all known use-cases ;)
    fun <V> VI(title: String, ttt: String, type: AnimatedProperty.Type?, value: V, style: Style, setValue: (V) -> Unit): Panel {
        return when(value){
            is Boolean -> BooleanInput(title, value, style)
                .setChangeListener { setValue(it as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Float -> FloatInput(title, value, type ?: AnimatedProperty.Type.FLOAT, style)
                .setChangeListener { setValue(it.toFloat() as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Double -> FloatInput(title, value, type ?: AnimatedProperty.Type.DOUBLE, style)
                .setChangeListener { setValue(it as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Vector2f -> VectorInput(style, title, value, type ?: AnimatedProperty.Type.VEC2)
                .setChangeListener { x, y, z, w -> setValue(Vector2f(x,y) as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Vector3f -> VectorInput(style, title, value, type ?: AnimatedProperty.Type.VEC3)
                .setChangeListener { x, y, z, w -> setValue(Vector3f(x,y,z) as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Vector4f -> {
                if(type == null || type == AnimatedProperty.Type.COLOR){
                    ColorInput(style, title, value, null)
                        .setChangeListener { r, g, b, a -> setValue(Vector4f(r,g,b,a) as V) }
                        .setIsSelectedListener { show(null) }
                        .setTooltip(ttt)
                } else {
                    VectorInput(style, title, value, type)
                        .setChangeListener { x, y, z, w -> setValue(Vector4f(x,y,z,w) as V) }
                        .setIsSelectedListener { show(null) }
                        .setTooltip(ttt)
                }
            }
            is Quaternionf -> VectorInput(style, title, value, type ?: AnimatedProperty.Type.QUATERNION)
                .setChangeListener { x, y, z, w -> setValue(Quaternionf(x,y,z,w) as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is String -> TextInput(title, style, value)
                .setChangeListener { setValue(it as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is File -> FileInput(title, style, value.toString())
                .setChangeListener { setValue(File(it) as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Enum<*> -> {
                val values = when(value){
                    is LoopingState -> LoopingState.values()
                    else -> throw RuntimeException("Missing enum .values() implementation for UI in Transform.kt")
                }
                val valueNames = values.map {
                    it to when(it){
                        LoopingState.PLAY_ONCE -> "Once"
                        LoopingState.PLAY_LOOP -> "Looping"
                        LoopingState.PLAY_REVERSING_LOOP -> "Reversing"
                        else -> it.name
                    }
                }
                EnumInput(title, true, valueNames.first { it.first == value }.second, valueNames.map { it.second }, style)
                    .setChangeListener { str -> setValue((valueNames.first { it.second == str }!!.first) as V) }
                    .setIsSelectedListener { show(null) }
                    .setTooltip(ttt)
            }
            else -> throw RuntimeException("Type $value not yet implemented!")
        }
    }

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * modifies the AnimatedProperty-Object, so no callback is needed
     * */
    fun VI(title: String, ttt: String, values: AnimatedProperty<*>, style: Style): Panel {
        val time = lastLocalTime
        return when(val value = values[time]){
            is Float -> FloatInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it.toFloat()) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Double -> FloatInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector2f -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, z, w -> putValue(values, Vector2f(x, y)) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector3f -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, z, w -> putValue(values, Vector3f(x, y, z)) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector4f -> {
                if(values.type == AnimatedProperty.Type.COLOR){
                    ColorInput(style, title, value)
                        .setChangeListener { r, g, b, a -> putValue(values, Vector4f(r, g, b, a)) }
                        .setIsSelectedListener { show(values) }
                        .setTooltip(ttt)
                } else {
                    VectorInput(title, values, time, style)
                        .setChangeListener { x, y, z, w -> putValue(values, Vector4f(x, y, z, w)) }
                        .setIsSelectedListener { show(values) }
                        .setTooltip(ttt)
                }
            }
            is Quaternionf -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, z, w -> putValue(values, Quaternionf(x, y, z, w)) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            else -> throw RuntimeException("Type $value not yet implemented!")
        }
    }

    open fun onDestroy(){}

    val listOfAll: Sequence<Transform> get() = sequence {
        yield(this@Transform)
        children.forEach { child ->
            yieldAll(child.listOfAll)
        }
    }

    companion object {
        // these values MUST NOT be changed
        // they are universal constants, and are used
        // within shaders, too
        var nextClickId = AtomicInteger()
        val xAxis = Vector3f(1f,0f,0f)
        val yAxis = Vector3f(0f,1f,0f)
        val zAxis = Vector3f(0f,0f,1f)
        fun String.toTransform() = TextReader.fromText(this).first() as Transform
    }


}