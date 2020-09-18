package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.blending.blendModes
import me.anno.objects.effects.MaskType
import me.anno.objects.effects.ToneMappers
import me.anno.objects.modes.LoopingState
import me.anno.objects.modes.ArraySelectionMode
import me.anno.objects.modes.UVProjection
import me.anno.objects.particles.ParticleSystem
import me.anno.studio.RemsStudio.onLargeChange
import me.anno.studio.Scene
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.input.*
import me.anno.ui.style.Style
import org.joml.*
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger

// pivot? nah, always use the center to make things easy;
// or should we do it?... idk for sure...
// just make the tree work perfectly <3

// gradients? -> can be done using the mask layer
// done select by clicking

// todo option to copy css compliant rgba colors?

open class Transform(var parent: Transform? = null): Saveable(), Inspectable {

    // todo generally "play" the animation of a single transform for testing purposes?
    // todo maybe only for video or audio? for audio it would be simple :)
    // useful for audio, video, particle systems, generally animations
    // only available if the rest is stopped? yes.

    init {
        parent?.addChild(this)
    }

    val clickId = nextClickId.incrementAndGet()
    var timelineSlot = -1
    var isEditorOnly = false

    var position = AnimatedProperty.pos()
    var scale = AnimatedProperty.scale()
    var rotationYXZ = AnimatedProperty.rotYXZ()
    var rotationQuaternion: AnimatedProperty<Quaternionf>? = null
    var skew = AnimatedProperty.skew()
    var alignWithCamera = AnimatedProperty.float01()
    var color = AnimatedProperty.color()
    var colorMultiplier = AnimatedProperty.floatPlus(1f)

    var blendMode = BlendMode.UNSPECIFIED

    var timeOffset = 0.0
    var timeDilation = 1.0

    var timeAnimated = AnimatedProperty.double()

    var name = getDefaultDisplayName()
    var comment = ""

    open fun getDefaultDisplayName() = if(getClassName() == "Transform") "Folder" else getClassName()
    open fun isVisible(localTime: Double) = true

    val rightPointingTriangle = "▶"
    val bottomPointingTriangle = "▼"
    val folder = "\uD83D\uDCC1"

    val children = ArrayList<Transform>()
    var isCollapsed = false

    var lastLocalColor = Vector4f()
    var lastLocalTime = 0.0

    var weight = 1f

    fun putValue(list: AnimatedProperty<*>, value: Any){
        list.addKeyframe(if(list.isAnimated) editorTime else 0.0, value, TimelinePanel.propertyDt)
    }

    val usesEuler get() = rotationQuaternion == null

    fun show(anim: AnimatedProperty<*>?){
        Studio.selectedProperty = anim
    }

    open fun claimResources(parentTime: Double, alpha: Float){
        val localTime = getLocalTime(parentTime)
        val localAlpha = getLocalColor(Vector4f(0f, 0f, 0f, alpha), localTime).w
        if(localAlpha > minAlpha){
            claimLocalResources(localTime)
            children.forEach {
                it.claimResources(localTime, localAlpha)
            }
        }
    }

    open fun claimLocalResources(localTime: Double){
        // here is nothing to claim
        // only for things using video textures
    }

    override fun createInspector(list: PanelListY, style: Style){

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
        list += VI("Blend Mode", "", null, blendMode, style){ blendMode = it }

        if(parent?.acceptsWeight() == true){
            list += VI("Weight", "", AnimatedProperty.Type.FLOAT_PLUS, weight, style){
                weight = it
                (parent as? ParticleSystem)?.apply {
                    if(children.size > 1) clearCache()
                }
            }
        }

        list += VI("Timeline Slot", "< 1 means invisible", AnimatedProperty.Type.INT_PLUS, timelineSlot, style){ timelineSlot = it }
        list += VI("Alignment with Camera", "0 = in 3D, 1 = looking towards the camera; billboards", alignWithCamera, style)
        list += VI("Editor Only", "Just a guideline?", null, isEditorOnly, style){ isEditorOnly = it }

    }

    open fun getLocalTime(parentTime: Double): Double {
        var localTime0 = (parentTime - timeOffset) * timeDilation
        localTime0 += timeAnimated[localTime0]
        return localTime0
    }

    fun getLocalColor(): Vector4f = getLocalColor(parent?.getLocalColor() ?: Vector4f(1f,1f,1f,1f), lastLocalTime)
    fun getLocalColor(parentColor: Vector4f, localTime: Double): Vector4f {
        val col = color.getValueAt(localTime)
        val mul = colorMultiplier[localTime]
        return Vector4f(col).mul(parentColor).mul(mul, mul, mul, 1f)
    }

    fun applyTransformLT(transform: Matrix4f, time: Double){

        val position = position[time]
        val scale = scale[time]
        val euler = rotationYXZ[time]
        val rotationQuat = rotationQuaternion
        val usesEuler = usesEuler
        val skew = skew[time]
        val alignWithCamera = alignWithCamera[time]

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

        if(alignWithCamera != 0f){
            transform.alignWithCamera(alignWithCamera)
        }




    }

    fun Matrix4f.alignWithCamera(alignWithCamera: Float){
        // lerp rotation instead of full transform?
        if(alignWithCamera != 0f){
            val local = Scene.lGCTInverted
            val up = local.transformDirection(Vector3f(0f, 1f, 0f))
            val forward = local.transformDirection(Vector3f(0f, 0f, -1f))
            if(alignWithCamera == 1f){
                lookAlong(forward, up)
            } else {
                lerp(Matrix4f(this).lookAlong(forward, up), alignWithCamera)
            }
        }
    }

    fun applyTransformPT(transform: Matrix4f, parentTime: Double) = applyTransformLT(transform, getLocalTime(parentTime))

    /**
     * stack with camera already included
     * */
    fun draw(stack: Matrix4fArrayList, parentTime: Double, parentColor: Vector4f){

        val time = getLocalTime(parentTime)
        val color = getLocalColor(parentColor, time)

        if(color.w > minAlpha && !(isFinalRendering && isEditorOnly)){ // 12 bit = 4k
            applyTransformLT(stack, time)
            GFX.drawnTransform = this
            val bd = BlendDepth(blendMode, GFX.currentCamera.useDepth)
            bd.bind()
            onDraw(stack, time, color)
            drawChildren(stack, time, color, parentColor)
            bd.unbind()
        }

    }

    fun drawChildren(stack: Matrix4fArrayList, time: Double, color: Vector4f, parentColor: Vector4f){
        val passesOnColor = passesOnColor()
        val childColor = if(passesOnColor) color else parentColor
        if(drawChildrenAutomatically()){
            drawChildren(stack, time, childColor)
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
            stack.pushMatrix()
            child.draw(stack, time, color)
            stack.popMatrix()
        }
    }

    fun drawUICircle(stack: Matrix4fArrayList, scale: Float, inner: Float, color: Vector4f){
        // draw a small symbol to indicate pivot
        if(!isFinalRendering){
            stack.pushMatrix()
            if(scale != 1f) stack.scale(scale)
            stack.alignWithCamera(1f)
            GFX.draw3DCircle(stack, inner, 0f, 360f, color)
            stack.popMatrix()
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
        writer.writeObject(this, "alignWithCamera", alignWithCamera)
        writer.writeDouble("timeOffset", timeOffset)
        writer.writeDouble("timeDilation", timeDilation)
        writer.writeObject(this, "timeAnimated", timeAnimated)
        writer.writeObject(this, "color", color)
        writer.writeString("blendMode", blendMode.id)
        writer.writeList(this, "children", children)
        writer.writeInt("timelineSlot", timelineSlot, true)
        writer.writeBool("editorOnly", isEditorOnly)
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "editorOnly" -> isEditorOnly = value
            else -> super.readBool(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "timelineSlot" -> timelineSlot = value
            else -> super.readInt(name, value)
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
            "alignWithCamera" -> alignWithCamera.copyFrom(value)
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

    fun addBefore(child: Transform){
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index, child)
        child.parent = p
    }

    fun addAfter(child: Transform){
        val p = parent!!
        val index = p.children.indexOf(this)
        p.children.add(index+1, child)
        child.parent = p
    }

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
        parent = null
    }

    override fun isDefaultValue() = false

    fun clone() = TextWriter.toText(this, false).toTransform()
    open fun acceptsWeight() = false
    open fun passesOnColor() = true

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
            is Int -> IntInput(title, value, type ?: AnimatedProperty.Type.INT, style)
                .setChangeListener { setValue(it.toInt() as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Long -> IntInput(title, value, type ?: AnimatedProperty.Type.LONG, style)
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
                .setChangeListener { x, y, _, _ -> setValue(Vector2f(x,y) as V) }
                .setIsSelectedListener { show(null) }
                .setTooltip(ttt)
            is Vector3f -> VectorInput(style, title, value, type ?: AnimatedProperty.Type.VEC3)
                .setChangeListener { x, y, z, _ -> setValue(Vector3f(x,y,z) as V) }
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
            is BlendMode -> {
                val values = blendModes.values
                val valueNames = values.map { it to it.displayName }
                EnumInput(title, true, valueNames.first { it.first == value }.second, valueNames.map { it.second }, style)
                    .setChangeListener { _, index, _ -> setValue(valueNames[index].first as V) }
                    .setIsSelectedListener { show(null) }
                    .setTooltip(ttt)
            }
            is Enum<*> -> {
                val values = when(value){
                    is LoopingState -> LoopingState.values()
                    is ToneMappers -> ToneMappers.values()
                    is MaskType -> MaskType.values()
                    is FilteringMode -> FilteringMode.values()
                    is ArraySelectionMode -> ArraySelectionMode.values()
                    is UVProjection -> UVProjection.values()
                    is ClampMode -> ClampMode.values()
                    else -> throw RuntimeException("Missing enum .values() implementation for UI in Transform.kt for $value")
                }
                val valueNames = values.map {
                    it to when(it){
                        is LoopingState -> it.displayName
                        is ToneMappers -> it.displayName
                        is MaskType -> it.displayName
                        is FilteringMode -> it.displayName
                        is ArraySelectionMode -> it.displayName
                        is UVProjection -> it.displayName
                        is ClampMode -> it.displayName
                        else -> it.name
                    }
                }
                EnumInput(title, true, valueNames.first { it.first == value }.second, valueNames.map { it.second }, style)
                    .setChangeListener { _, index, _ -> setValue(values[index] as V) }
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
            is Int -> IntInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it.toInt()) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Long -> IntInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Float -> FloatInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it.toFloat()) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Double -> FloatInput(title, values, 0, time, style)
                .setChangeListener { putValue(values, it) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector2f -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, _, _ -> putValue(values, Vector2f(x, y)) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector3f -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, z, _ -> putValue(values, Vector3f(x, y, z)) }
                .setIsSelectedListener { show(values) }
                .setTooltip(ttt)
            is Vector4f -> {
                if(values.type == AnimatedProperty.Type.COLOR){
                    ColorInput(style, title, value, values)
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
    open fun destroy(){
        if(selectedTransform === this){
            GFX.select(null)
        }
        removeFromParent()
        onDestroy()
        onLargeChange()
    }

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
        val xAxis = Vector3f(1f,0f,0f)
        val yAxis = Vector3f(0f,1f,0f)
        val zAxis = Vector3f(0f,0f,1f)
        var nextClickId = AtomicInteger()
        fun String.toTransform() = TextReader.fromText(this).first() as Transform
        const val minAlpha = 0.00025f
    }


}