package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.io.text.TextReader
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.clamp
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class GFXArray(parent: Transform?): GFXTransform(parent) {

    companion object {
        val MAX_ARRAY_DIMENSION = DefaultConfig["array.dimension.max", 3]
    }

    // 0 .. 3, everything else is stupid and can be achieved using recursion
    var dimensions = 1

    // todo replace element properties with array values
    // todo decide dimension automatically?
    // todo decide element count automatically
    // todo dimensions and data from pure, multiline text

    val elementCount = IntArray(MAX_ARRAY_DIMENSION)

    val perChildLocation = ArrayList<AnimatedProperty<Vector3f>>()
    val perChildRotation = ArrayList<AnimatedProperty<Vector3f>>()
    val perChildScale = ArrayList<AnimatedProperty<Vector3f>>()
    val perChildTimeOffset = FloatArray(MAX_ARRAY_DIMENSION)
    // val perChildTimeDilation = FloatArray(MAX_ARRAY_DIMENSION) // useful?, power vs linear

    // per child skew?

    var instanceCount = 10

    fun clampDimensions(){
        dimensions = clamp(dimensions, 0, MAX_ARRAY_DIMENSION)
    }

    fun updateDimensions(){
        clampDimensions()
        while(dimensions > perChildLocation.size) perChildLocation.add(AnimatedProperty.pos())
        while(dimensions > perChildRotation.size) perChildRotation.add(AnimatedProperty.rotYXZ())
        while(dimensions > perChildScale.size) perChildScale.add(AnimatedProperty.scale())
    }

    fun updateChildren(){
        children.removeAll(children.filterIndexed { index, _ -> index >= instanceCount })
        if(children.size in 0 until instanceCount){
            val base = children.first().stringify()
            while(children.size < instanceCount){
                addChild(object: Transform(null){
                    init {
                        val clone = TextReader.fromText(base).first { it is Transform } as Transform
                        children.add(clone)
                    }
                })
            }
        }
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Float, color: Vector4f) {
        super.onDraw(stack, time, color)
        // todo replace 1:1 every time? how? drivers??? (text)

    }

    override fun drawChildrenAutomatically() = false

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        // todo create apply button?
        // todo we need to be able to insert properties...
        // todo replace? :D, # String Array
    }




}