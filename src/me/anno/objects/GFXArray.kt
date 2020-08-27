package me.anno.objects

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

class GFXArray(parent: Transform? = null): GFXTransform(parent) {

    val perChildTranslation = AnimatedProperty.pos()
    val perChildRotation = AnimatedProperty.rotYXZ()
    val perChildScale = AnimatedProperty.scale()
    val perChildSkew = AnimatedProperty.skew()
    var perChildDelay = 0.1

    // val perChildTimeDilation = FloatArray(MAX_ARRAY_DIMENSION) // useful?, power vs linear

    // per child skew?

    val instanceCount = AnimatedProperty.intPlus(10)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "instanceCount", instanceCount, true)
        writer.writeObject(this, "perChildTranslation", perChildTranslation)
        writer.writeObject(this, "perChildRotation", perChildRotation)
        writer.writeObject(this, "perChildScale", perChildScale)
        writer.writeObject(this, "perChildSkew", perChildSkew)
        writer.writeDouble("perChildDelay", perChildDelay)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "instanceCount" -> instanceCount.copyFrom(value)
            "perChildTranslation" -> perChildTranslation.copyFrom(value)
            "perChildRotation" -> perChildRotation.copyFrom(value)
            "perChildScale" -> perChildScale.copyFrom(value)
            "perChildSkew" -> perChildSkew.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when(name){
            "perChildDelay" -> perChildDelay = value
            else -> super.readDouble(name, value)
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        super.onDraw(stack, time, color)

        // todo make text replacement simpler???
        val instanceCount = instanceCount[time]
        if(instanceCount > 0 && children.isNotEmpty()){
            drawArrayChild(stack, time, color, 0, instanceCount)
        }

    }

    fun drawArrayChild(transform: Matrix4fArrayList, time: Double, color: Vector4f, index: Int, instanceCount: Int){


        drawChild(transform, time, color, children[index % children.size])

        if(index+1 < instanceCount){

            val position = perChildTranslation[time]
            if(position.x != 0f || position.y != 0f || position.z != 0f){ transform.translate(position) }

            val euler = perChildRotation[time]
            if(euler.y != 0f) transform.rotate(GFX.toRadians(euler.y), yAxis)
            if(euler.x != 0f) transform.rotate(GFX.toRadians(euler.x), xAxis)
            if(euler.z != 0f) transform.rotate(GFX.toRadians(euler.z), zAxis)

            val scale = perChildScale[time]
            if(scale.x != 1f || scale.y != 1f || scale.z != 1f) transform.scale(scale)

            val skew = perChildSkew[time]
            if(skew.x != 0f || skew.y != 0f) transform.mul3x3(// works
                1f, skew.y, 0f,
                skew.x, 1f, 0f,
                0f, 0f, 1f
            )

            drawArrayChild(transform, time+perChildDelay, color, index+1, instanceCount)

        }
    }

    override fun drawChildrenAutomatically() = false

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)

        // todo create apply button?
        // todo we need to be able to insert properties...
        // todo replace? :D, # String Array

        list += VI("Offset/Child", "", perChildTranslation, style)
        list += VI("Rotation/Child", "", perChildRotation, style)
        list += VI("Scale/Child", "", perChildScale, style)
        list += VI("Delay/Child", "", AnimatedProperty.Type.FLOAT, perChildDelay, style){ perChildDelay = it }
        list += VI("Instances", "", instanceCount, style)

        list += ButtonPanel("Apply Array", style)
            .setSimpleClickListener { applyArray() }
            .setTooltip("Makes it less configurable; however you can change all aspects individually then")

    }

    fun applyArray(){
        // todo create save point
        val parent = parent ?: return
        val folder = Transform()
        parent.children.add(parent.children.indexOf(this), folder)

        removeFromParent()
    }

    override fun getClassName() = "GFXArray"
    override fun getDefaultDisplayName() = "Array"



}