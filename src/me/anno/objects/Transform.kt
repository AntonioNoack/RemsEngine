package me.anno.objects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.utils.clamp
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.TextInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import org.joml.*
import java.lang.RuntimeException
import kotlin.math.max

// pivot? nah, always use the center to make things easy;
// or should we do it?... idk for sure...
// just make the tree work perfectly <3

// todo load 3D meshes :D
// todo gradients?

open class Transform(
    var localPosition: AnimatedProperty<Vector3f>?,
    var localScale: AnimatedProperty<Vector3f>?,
    var localRotationYXZ: AnimatedProperty<Vector3f>?,
    var localRotationQuaternion: AnimatedProperty<Quaternionf>?,
    var parent: Transform? = null
    ): Saveable(){

    constructor(parent: Transform?): this(null, null, null, null, parent)

    var localSkew: AnimatedProperty<Vector2f>? = null

    var timeOffset = 0f
    var timeDilation = 1f
    // todo make this animatable, calculate the integral to get a mapping
    var timeAnimated: AnimatedProperty<Float>? = null

    var color: AnimatedProperty<Vector4f>? = null

    var name = if(getClassName() == "Transform") "Folder" else getClassName()
    var comment = ""

    val rightPointingTriangle = "▶"
    val bottomPointingTriangle = "▼"
    val folder = "\uD83D\uDCC1"

    val children = ArrayList<Transform>()
    var isCollapsed = false

    var lastLocalTime = 0f

    fun <V> putValue(list: AnimatedProperty<V>, value: V){
        list.addKeyframe(lastLocalTime, value, 0.1f)
    }

    val usesEuler get() = localRotationQuaternion == null

    fun show(anim: AnimatedProperty<*>?){
        GFX.selectedProperty = anim
    }

    open fun createInspector(list: PanelListY){

        val style = list.style
        // todo update by time :)
        val one3 = Vector3f(1f,1f,1f)
        val one4 = Vector4f(1f,1f,1f,1f)

        list += TextInput("Name", style, name)
            .setChangeListener {
                name = if(it.isEmpty()) "-" else it

            }

        list += TextInput("Comment", style, comment)
            .setChangeListener {
                comment = it
            }

        list += VectorInput(style, "Position", localPosition?.get(lastLocalTime) ?: Vector3f(), AnimatedProperty.Type.POSITION).setChangeListener { x, y, z, w ->
            if(localPosition == null) localPosition = AnimatedProperty.pos()
            putValue(localPosition!!, Vector3f(x,y,z))
        }.setIsSelectedListener { show(localPosition) }
        list += VectorInput(style, "Scale", localScale?.get(lastLocalTime) ?: one3, AnimatedProperty.Type.SCALE).setChangeListener { x, y, z, w ->
            if(localScale == null) localScale = AnimatedProperty.scale()
            putValue(localScale!!, Vector3f(x,y,z))
        }.setIsSelectedListener { show(localScale) }

        if(usesEuler){
            list += VectorInput(style, "Rotation (YXZ)", localRotationYXZ?.get(lastLocalTime) ?: Vector3f(), AnimatedProperty.Type.ROT_YXZ).setChangeListener { x, y, z, w ->
                if(localRotationYXZ == null) localRotationYXZ = AnimatedProperty.rotYXZ()
                putValue(localRotationYXZ!!, Vector3f(x,y,z))
            }.setIsSelectedListener { show(localRotationYXZ) }
        } else {
            list += VectorInput(style, "Rotation (Quaternion)", localRotationQuaternion?.get(lastLocalTime) ?: Quaternionf()).setChangeListener { x, y, z, w ->
                if(localRotationQuaternion == null) localRotationQuaternion = AnimatedProperty.quat()
                putValue(localRotationQuaternion!!, Quaternionf(x,y,z,w+1e-9f).normalize())
            }.setIsSelectedListener { show(localRotationQuaternion) }
        }

        list += VectorInput(style, "Skew", localSkew?.get(lastLocalTime) ?: Vector2f(), AnimatedProperty.Type.SKEW_2D)
            .setChangeListener { x, y, z, w ->
                if(localSkew == null) localSkew = AnimatedProperty.skew()
                putValue(localSkew!!, Vector2f(x,y))
            }.setIsSelectedListener { show(localSkew) }

        list += ColorInput(style, "Color", color?.get(lastLocalTime) ?: one4, AnimatedProperty.Type.COLOR).setChangeListener { x, y, z, w ->
            if(color == null) color = AnimatedProperty.color()
            putValue(color!!, Vector4f(max(0f, x), max(0f, y), max(0f, z),
                clamp(w, 0f, 1f)
            ))
        }.setIsSelectedListener { show(color) }
        list += FloatInput(style, "Time Offset").setChangeListener { timeOffset = it }
        list += FloatInput(style, "Time Dilation").setChangeListener { timeDilation = it }
        list += FloatInput(style, "Time Manipulation").setChangeListener {  x ->
            if(timeAnimated == null) timeAnimated = AnimatedProperty.float()
            putValue(timeAnimated!!, x)
        }.setIsSelectedListener { show(timeAnimated) }

    }

    fun getLocalTime(parentTime: Float): Float {
        var localTime0 = (parentTime - timeOffset) * timeDilation
        val anim = timeAnimated
        if(anim != null) localTime0 += anim[localTime0]
        return localTime0
    }

    fun getLocalColor(): Vector4f = getLocalColor(parent?.getLocalColor() ?: Vector4f(1f,1f,1f,1f), lastLocalTime)
    fun getLocalColor(parentColor: Vector4f, time: Float): Vector4f {
        val col = color?.getValueAt(time)
        return if(col != null) Vector4f(col).mul(parentColor)
        else parentColor
    }

    fun applyTransform(transform: Matrix4f, parentTime: Float){

        val time = getLocalTime(parentTime)
        val translation = localPosition
        val scale = localScale
        val rotationYXZ = localRotationYXZ
        val rotationQuat = localRotationQuaternion
        val usesEuler = usesEuler
        val skew = localSkew

        if(translation != null) transform.translate(translation[time])
        if(usesEuler){// y x z
            if(rotationYXZ != null) {
                val euler = rotationYXZ[time]
                transform.rotate(toRadians(euler.y), Vector3f(0f,1f,0f))
                transform.rotate(toRadians(euler.x), Vector3f(1f,0f,0f))
                transform.rotate(toRadians(euler.z), Vector3f(0f,0f,1f))
            }
        } else {
            if(rotationQuat != null) transform.rotate(rotationQuat[time])
        }

        if(scale != null) transform.scale(scale[time])

        if(skew != null){
            val a = skew[time]
            transform.mul3x3(// works
                1f, a.y, 0f,
                a.x, 1f, 0f,
                0f, 0f, 1f
            )
        }

    }

    /**
     * stack with camera already included
     * */
    open fun draw(stack: Matrix4fStack, parentTime: Float, parentColor: Vector4f, style: Style){
        val time = getLocalTime(parentTime)
        lastLocalTime = time
        val color = getLocalColor(parentColor, time)
        if(color.w > 0.00025f){// 12 bit = 4k

            applyTransform(stack, parentTime)

            children.forEach { child ->

                stack.pushMatrix()
                child.draw(stack, time, color, style)
                stack.popMatrix()

            }

        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "parent", parent)
        writer.writeString("name", name)
        writer.writeObject(this, "position", localPosition)
        writer.writeObject(this, "scale", localScale)
        writer.writeObject(this, "rotationYXZ", localRotationYXZ)
        writer.writeObject(this, "rotationQuat", localRotationQuaternion)
        writer.writeFloat("timeOffset", timeOffset)
        writer.writeFloat("timeDilation", timeDilation)
        writer.writeObject(this, "timeAnimated", timeAnimated)
        writer.writeObject(this, "color", color)
        writer.writeList(this, "children", children)
    }

    override fun readObject(name: String, value: Saveable?) {
        when(name){
            "parent" -> {
                if(value is Transform){
                    parent = this
                }
            }
            "children" -> {
                if(value is Transform){
                    addChild(value)
                }
            }
            "position" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.POSITION){
                    localPosition = value as AnimatedProperty<Vector3f>
                }
            }
            "scale" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.SCALE){
                    localScale = value as AnimatedProperty<Vector3f>
                }
            }
            "rotationYXZ" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.ROT_YXZ){
                    localRotationYXZ = value as AnimatedProperty<Vector3f>
                }
            }
            "rotationQuat" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.QUATERNION){
                    localRotationQuaternion = value as AnimatedProperty<Quaternionf>
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



}