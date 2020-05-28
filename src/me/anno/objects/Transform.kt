package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.utils.clamp
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.blending.BlendMode
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.*
import me.anno.ui.style.Style
import org.joml.*
import java.lang.RuntimeException
import kotlin.math.max

// pivot? nah, always use the center to make things easy;
// or should we do it?... idk for sure...
// just make the tree work perfectly <3

// todo load 3D meshes :D
// todo gradients?

open class Transform(var parent: Transform? = null): Saveable(){

    var position = AnimatedProperty.pos()
    var scale = AnimatedProperty.scale()
    var rotationYXZ = AnimatedProperty.rotYXZ()
    var rotationQuaternion: AnimatedProperty<Quaternionf>? = null
    var skew = AnimatedProperty.skew()
    var color = AnimatedProperty.color()

    var blendMode = BlendMode.UNSPECIFIED

    var timeOffset = 0f
    var timeDilation = 1f

    // todo make this animatable, calculate the integral to get a mapping
    var timeAnimated = AnimatedProperty.float()

    var name = if(getClassName() == "Transform") "Folder" else getClassName()
    var comment = ""

    val rightPointingTriangle = "▶"
    val bottomPointingTriangle = "▼"
    val folder = "\uD83D\uDCC1"

    val children = ArrayList<Transform>()
    var isCollapsed = false

    var lastLocalTime = 0f

    fun putValue(list: AnimatedProperty<*>, value: Any){
        list.addKeyframe(lastLocalTime, value, 0.1f)
    }

    val usesEuler get() = rotationQuaternion == null

    fun show(anim: AnimatedProperty<*>?){
        GFX.selectedProperty = anim
    }

    open fun createInspector(list: PanelListY, style: Style){

        // todo update by time :)

        list += TextInput("Name", style, name)
            .setChangeListener {
                name = if(it.isEmpty()) "-" else it

            }

        list += TextInput("Comment", style, comment)
            .setChangeListener {
                comment = it
            }

        list += VectorInput(style, "Position", position[lastLocalTime],
            AnimatedProperty.Type.POSITION, position)
            .setChangeListener { x, y, z, w ->
                putValue(position, Vector3f(x,y,z))
            }.setIsSelectedListener { show(position) }
        list += VectorInput(style, "Scale", scale[lastLocalTime],
            AnimatedProperty.Type.SCALE, scale)
            .setChangeListener { x, y, z, w ->
                putValue(scale, Vector3f(x,y,z)) }
            .setIsSelectedListener { show(scale) }

        if(usesEuler){
            list += VectorInput(style, "Rotation (YXZ)", rotationYXZ[lastLocalTime],
                AnimatedProperty.Type.ROT_YXZ, rotationYXZ)
                .setChangeListener { x, y, z, w ->
                    putValue(rotationYXZ, Vector3f(x,y,z)) }
                .setIsSelectedListener { show(rotationYXZ) }
        } else {
            list += VectorInput(style, "Rotation (Quaternion)", rotationQuaternion?.get(lastLocalTime) ?: Quaternionf())
                .setChangeListener { x, y, z, w ->
                    if(rotationQuaternion == null) rotationQuaternion = AnimatedProperty.quat()
                    putValue(rotationQuaternion!!, Quaternionf(x,y,z,w+1e-9f).normalize()) }
                .setIsSelectedListener { show(rotationQuaternion) }
        }

        list += VectorInput(style, "Skew", skew[lastLocalTime],
            AnimatedProperty.Type.SKEW_2D, skew)
            .setChangeListener { x, y, z, w ->
                putValue(skew, Vector2f(x,y))
            }.setIsSelectedListener { show(skew) }

        list += ColorInput(style, "Color", color[lastLocalTime],
            AnimatedProperty.Type.COLOR, color).setChangeListener { x, y, z, w ->
            putValue(color, Vector4f(max(0f, x), max(0f, y), max(0f, z),
                clamp(w, 0f, 1f)
            ))
        }.setIsSelectedListener { show(color) }
        list += FloatInput(style, "Time Offset", timeOffset).setChangeListener { timeOffset = it }
        list += FloatInput(style, "Time Dilation", timeDilation).setChangeListener { timeDilation = it }
        list += FloatInput(style, "Time Manipulation", timeAnimated[lastLocalTime]).setChangeListener {  x ->
            putValue(timeAnimated, x)
        }.setIsSelectedListener { show(timeAnimated) }
        list += TextInput("Blend Mode", style, blendMode.id)
            .setChangeListener { blendMode = BlendMode[it] }


    }

    fun getLocalTime(parentTime: Float): Float {
        var localTime0 = (parentTime - timeOffset) * timeDilation
        localTime0 += timeAnimated[localTime0]
        return localTime0
    }

    fun getLocalColor(): Vector4f = getLocalColor(parent?.getLocalColor() ?: Vector4f(1f,1f,1f,1f), lastLocalTime)
    fun getLocalColor(parentColor: Vector4f, time: Float): Vector4f {
        val col = color.getValueAt(time)
        return Vector4f(col).mul(parentColor)
    }

    fun applyTransformLT(transform: Matrix4f, time: Float){

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

        if(scale.x != 0f || scale.y != 0f || scale.z != 0f) transform.scale(scale)

        if(skew.x != 0f || skew.y != 0f) transform.mul3x3(// works
            1f, skew.y, 0f,
            skew.x, 1f, 0f,
            0f, 0f, 1f
        )

    }

    fun applyTransformPT(transform: Matrix4f, parentTime: Float) = applyTransformLT(transform, getLocalTime(parentTime))

    /**
     * stack with camera already included
     * */
    fun draw(stack: Matrix4fStack, parentTime: Float, parentColor: Vector4f){

        val time = getLocalTime(parentTime)
        lastLocalTime = time
        val color = getLocalColor(parentColor, time)

        if(color.w > 0.00025f){ // 12 bit = 4k
            blendMode.apply()
            applyTransformLT(stack, time)
            onDraw(stack, time, color)
            drawChildren(stack, time, color)
        }

    }

    fun drawChildren(stack: Matrix4fStack, time: Float, color: Vector4f){
        children.forEach { child ->

            blendMode.apply()

            stack.pushMatrix()
            child.draw(stack, time, color)
            stack.popMatrix()

        }
    }

    open fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){}

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "parent", parent)
        writer.writeString("name", name)
        writer.writeObject(this, "position", position)
        writer.writeObject(this, "scale", scale)
        writer.writeObject(this, "rotationYXZ", rotationYXZ)
        writer.writeObject(this, "rotationQuat", rotationQuaternion)
        writer.writeFloat("timeOffset", timeOffset)
        writer.writeFloat("timeDilation", timeDilation)
        writer.writeObject(this, "timeAnimated", timeAnimated)
        writer.writeObject(this, "color", color)
        writer.writeString("blendMode", blendMode.id)
        writer.writeList(this, "children", children)
    }

    override fun readObject(name: String, value: Saveable?) {
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
            "position" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.POSITION){
                    position = value as AnimatedProperty<Vector3f>
                }
            }
            "scale" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.SCALE){
                    scale = value as AnimatedProperty<Vector3f>
                }
            }
            "rotationYXZ" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.ROT_YXZ){
                    rotationYXZ = value as AnimatedProperty<Vector3f>
                }
            }
            "rotationQuat" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.QUATERNION){
                    rotationQuaternion = value as AnimatedProperty<Quaternionf>
                }
            }
            "timeAnimated" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.FLOAT){
                    timeAnimated = value as AnimatedProperty<Float>
                }
            }
            "color" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.COLOR){
                    color = value as AnimatedProperty<Vector4f>
                }
            }
            else -> super.readObject(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "timeDilation" -> timeDilation = value
            "timeOffset" -> timeOffset = value
            else -> super.readFloat(name, value)
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
        for(child in children){
            if(child === t || child.contains(t)) return true
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

    init {
        parent?.addChild(this)
    }

    fun clearIds(){
        uuid = 0L
        for(child in children) child.clearIds()
    }

    fun stringify(): String {
        clearIds()
        val myParent = parent
        parent = null
        val data = TextWriter.toText(this, false)
        parent = myParent
        return data
    }

    companion object {
        val xAxis = Vector3f(1f,0f,0f)
        val yAxis = Vector3f(0f,1f,0f)
        val zAxis = Vector3f(0f, 0f, 1f)
    }


}