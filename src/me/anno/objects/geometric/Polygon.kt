package me.anno.objects.geometric

import me.anno.config.DefaultConfig
import me.anno.gpu.Buffer
import me.anno.gpu.GFX
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import org.joml.Matrix4fStack
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class Polygon(parent: Transform?): GFXTransform(parent){

    val vertexCount = AnimatedProperty.float().set(5f)

    // todo inner radius???
    // todo allow to rotate the inner radius?
    // todo round edges?

    override fun draw(stack: Matrix4fStack, parentTime: Float, parentColor: Vector4f, style: Style) {
        super.draw(stack, parentTime, parentColor, style)
        val time = getLocalTime(parentTime)
        val color = getLocalColor(parentColor, time)
        GFX.draw3D(stack, getBuffer(vertexCount[time].roundToInt()), GFX.whiteTexture, color, isBillboard[time])
    }

    override fun createInspector(list: PanelListY) {
        super.createInspector(list)
        list += FloatInput(list.style, "Vertex Count", vertexCount[lastLocalTime])
            .setChangeListener { putValue(vertexCount, it) }
            .setIsSelectedListener { show(vertexCount) }
    }

    companion object {
        val minEdges = 3
        val maxEdges = DefaultConfig["polygon.maxEdges"] as? Int ?: 50
        val buffers = HashMap<Int, StaticFloatBuffer>()
        fun getBuffer(n: Int): StaticFloatBuffer {
            val cached = buffers[n]
            if(cached != null) return cached
            if(n < minEdges) return getBuffer(minEdges)
            if(n > maxEdges) return getBuffer(maxEdges)
            val buffer = StaticFloatBuffer(listOf(Buffer.Attribute("attr0", 2)), (n-2) * 3 * 2)
            val angles = FloatArray(n+1){ i -> (i*Math.PI*2.0/n).toFloat() }
            val sin = angles.map { sin(it)*.5f+.5f }
            val cos = angles.map { -cos(it)*.5f+.5f }
            // optimized to only use n-2 triangles
            for(i in 2 until n){
                buffer.put(sin[0])
                buffer.put(cos[0])
                buffer.put(sin[i])
                buffer.put(cos[i])
                buffer.put(sin[i-1])
                buffer.put(cos[i-1])
            }
            return buffer
        }
    }

    override fun getClassName(): String = "Polygon"

}