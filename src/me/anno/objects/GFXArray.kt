package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.modes.ArraySelectionMode
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

class GFXArray(parent: Transform? = null): GFXTransform(parent) {

    val perChildTranslation = AnimatedProperty.pos()
    val perChildRotation = AnimatedProperty.rotYXZ()
    val perChildScale = AnimatedProperty.scale()
    val perChildSkew = AnimatedProperty.skew()
    var perChildDelay = AnimatedProperty.double()

    // val perChildTimeDilation = FloatArray(MAX_ARRAY_DIMENSION) // useful?, power vs linear

    // per child skew?

    override fun getSymbol() = DefaultConfig["ui.symbol.array", "[[["]

    val instanceCount = AnimatedProperty.intPlus(10)
    var selectionSeed = AnimatedProperty.long()
    var selectionMode = ArraySelectionMode.ROUND_ROBIN

    override fun acceptsWeight(): Boolean = true

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "instanceCount", instanceCount, true)
        writer.writeObject(this, "perChildTranslation", perChildTranslation)
        writer.writeObject(this, "perChildRotation", perChildRotation)
        writer.writeObject(this, "perChildScale", perChildScale)
        writer.writeObject(this, "perChildSkew", perChildSkew)
        writer.writeObject(this, "perChildDelay", perChildDelay)
        writer.writeObject(this, "selectionSeed", selectionSeed)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "instanceCount" -> instanceCount.copyFrom(value)
            "perChildTranslation" -> perChildTranslation.copyFrom(value)
            "perChildRotation" -> perChildRotation.copyFrom(value)
            "perChildScale" -> perChildScale.copyFrom(value)
            "perChildSkew" -> perChildSkew.copyFrom(value)
            "perChildDelay" -> perChildDelay.copyFrom(value)
            "selectionSeed" -> selectionSeed.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        super.onDraw(stack, time, color)

        // todo make text replacement simpler???
        val instanceCount = instanceCount[time]
        if(instanceCount > 0 && children.isNotEmpty()){
            val seed = selectionSeed[time]
            val random = Random(seed)
            random.nextInt() // first one otherwise is always 1 (with two elements)
            val perChildDelay = perChildDelay[time]
            drawArrayChild(stack, time, perChildDelay, color, 0, instanceCount, random,
            perChildTranslation[time], perChildRotation[time], perChildScale[time], perChildSkew[time])
        }

    }

    fun drawArrayChild(
        transform: Matrix4fArrayList, time: Double, perChildDelay: Double, color: Vector4f,
        index: Int, instanceCount: Int, random: Random,
        position: Vector3f, euler: Vector3f, scale: Vector3f, skew: Vector2f
    ){

        val childIndex = selectionMode[index, children.size, random]
        drawChild(transform, time, color, children[childIndex])

        if(index+1 < instanceCount){

            //val position = perChildTranslation[time]
            if(position.x != 0f || position.y != 0f || position.z != 0f){ transform.translate(position) }

            //val euler = perChildRotation[time]
            if(euler.y != 0f) transform.rotate(GFX.toRadians(euler.y), yAxis)
            if(euler.x != 0f) transform.rotate(GFX.toRadians(euler.x), xAxis)
            if(euler.z != 0f) transform.rotate(GFX.toRadians(euler.z), zAxis)

            //val scale = perChildScale[time]
            if(scale.x != 1f || scale.y != 1f || scale.z != 1f) transform.scale(scale)

           // val skew = perChildSkew[time]
            if(skew.x != 0f || skew.y != 0f) transform.mul3x3(// works
                1f, skew.y, 0f,
                skew.x, 1f, 0f,
                0f, 0f, 1f
            )

            drawArrayChild(transform,
                time+perChildDelay, perChildDelay, color, index+1, instanceCount, random,
                position, euler, scale, skew
            )

        }
    }

    override fun drawChildrenAutomatically() = false

    override fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, id: String) -> SettingCategory) {
        super.createInspector(list, style, getGroup)

        // todo create apply button?
        // todo we need to be able to insert properties...
        // todo replace? :D, # String Array

        val child = getGroup("Per-Child Transform", "per-child")
        child += vi("Offset/Child", "", perChildTranslation, style)
        child += vi("Rotation/Child", "", perChildRotation, style)
        child += vi("Scale/Child", "", perChildScale, style)
        child += vi("Delay/Child", "", perChildDelay, style)

        val instances = getGroup("Instances", "children")
        instances += vi("Instances", "", instanceCount, style)
        instances += vi("Selection Mode", "", null, selectionMode, style){ selectionMode = it }
        instances += vi("Selection Seed", "Only for randomized selection mode", selectionSeed, style)

    }

    override fun getClassName() = "GFXArray"
    override fun getDefaultDisplayName() = Dict["Array", "obj.array"]



}