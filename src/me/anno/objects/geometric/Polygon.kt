package me.anno.objects.geometric

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.objects.cache.SFBufferData
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.utils.clamp
import org.joml.Matrix4fStack
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class Polygon(parent: Transform?): GFXTransform(parent){

    // todo polygon depth :)
    // todo inner radius??? -> by texture possible? we need masking!
    // todo round edges?

    var vertexCount = AnimatedProperty.float().set(5f)
    var inset = AnimatedProperty.float()

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){
        val inset = clamp(inset[time], 0f, 1f)
        if(inset == 1f) return// invisible
        // todo correct the texture somehow... very awkward
        val count = vertexCount[time].roundToInt()
        GFX.draw3DPolygon(stack, getBuffer(count), GFX.whiteTexture, color,
            inset, isBillboard[time], true)
        return
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput(list.style, "Vertex Count", vertexCount[lastLocalTime])
            .setChangeListener { putValue(vertexCount, it) }
            .setIsSelectedListener { show(vertexCount) }
        list += FloatInput(list.style, "Star-ness", inset[lastLocalTime])
            .setChangeListener { putValue(inset, clamp(it, 0f, 1f)) }
            .setIsSelectedListener { show(inset) }
    }

    override fun getClassName(): String = "Polygon"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "vertexCount", vertexCount)
        writer.writeObject(this, "inset", inset)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "vertexCount" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.FLOAT){
                    vertexCount = value as AnimatedProperty<Float>
                }
            }
            "inset" -> {
                if(value is AnimatedProperty<*> && value.type == AnimatedProperty.Type.FLOAT){
                    inset = value as AnimatedProperty<Float>
                }
            }
            else -> super.readObject(name, value)
        }
    }

    companion object {

        private const val minEdges = 3
        private val maxEdges = DefaultConfig["polygon.maxEdges", 1000]

        fun getBuffer(n: Int): StaticFloatBuffer {
            if(n < minEdges) return getBuffer(minEdges)
            if(n > maxEdges) return getBuffer(maxEdges)
            val cached = Cache.getEntry("Mesh", "Polygon", n){
                SFBufferData(createBuffer(n))
            } as SFBufferData
            return cached.buffer
        }

        private fun createBuffer(n: Int): StaticFloatBuffer {
            val buffer = StaticFloatBuffer(listOf(Attribute("attr0", 2), Attribute("attr1", 2)), n * 3 * 4)
            val angles = FloatArray(n+1){ i -> (i*Math.PI*2.0/n).toFloat() }
            val sin = angles.map { sin(it)*.5f+.5f }
            val cos = angles.map { -cos(it)*.5f+.5f }
            for(i in 0 until n){

                val inset = if(i % 2 == 0) 1f else 0f

                buffer.put(0.5f)
                buffer.put(0.5f)
                buffer.put(0f)
                buffer.put(0f)

                buffer.put(sin[i])
                buffer.put(cos[i])
                buffer.put(inset)
                buffer.put(1f)

                buffer.put(sin[i+1])
                buffer.put(cos[i+1])
                buffer.put(if(((i+1) % n) % 2 == 0) 1f else 0f)
                buffer.put(1f)

            }
            return buffer
        }
    }

}