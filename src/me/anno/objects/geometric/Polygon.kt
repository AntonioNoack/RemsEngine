package me.anno.objects.geometric

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.objects.cache.SFBufferData
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.utils.clamp
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class Polygon(parent: Transform?): GFXTransform(parent){

    // todo round edges?
    // todo auto align?

    init {
        scale.set(DefaultConfig["object.polygon.defaultScale", Vector3f(1f, 1f, 0f)])
    }

    var texture = File("")
    var autoAlign = false
    var nearestFiltering = false

    var vertexCount = AnimatedProperty.floatPlus().set(5f)
    var starNess = AnimatedProperty.float01()

    override fun onDraw(stack: Matrix4fStack, time: Float, color: Vector4f){
        val inset = clamp(starNess[time], 0f, 1f)
        if(inset == 1f) return// invisible
        val texture = Cache.getImage(texture, 5000) ?: GFX.whiteTexture
        val count = vertexCount[time].roundToInt()
        val selfDepth = scale[time].z
        if(autoAlign && count == 4){
            stack.rotate(toRadians(45f), zAxis)
            stack.scale(sqrt2, sqrt2, 1f)
        }
        GFX.draw3DPolygon(stack, getBuffer(count, selfDepth > 0f), texture, color,
            inset, isBillboard[time], if(texture == GFX.whiteTexture) true else nearestFiltering)
        return
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput("Vertex Count", vertexCount, lastLocalTime, style)
            .setChangeListener { putValue(vertexCount, it) }
            .setIsSelectedListener { show(vertexCount) }
        list += FloatInput("Star-ness", starNess, lastLocalTime, style)
            .setChangeListener { putValue(starNess, clamp(it, 0f, 1f)) }
            .setIsSelectedListener { show(starNess) }
        list += FileInput("Texture", style, texture.toString())
            .setChangeListener { texture = File(it) }
            .setIsSelectedListener { show(null) }
        list += BooleanInput("Nearest Filtering", nearestFiltering, style)
            .setChangeListener { nearestFiltering = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("Pixelated Style for the texture")
        list += BooleanInput("Auto-Align", autoAlign, style)
            .setChangeListener { autoAlign = it }
            .setIsSelectedListener { show(null) }
            .setTooltip("for quads")
    }

    override fun getClassName(): String = "Polygon"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "vertexCount", vertexCount)
        writer.writeObject(this, "inset", starNess)
        writer.writeBool("autoAlign", autoAlign, true)
        writer.writeBool("nearestFiltering", nearestFiltering, true)
        writer.writeFile("texture", texture)
    }

    override fun readString(name: String, value: String) {
        when(name){
            "texture" -> texture = File(name)
            else -> super.readString(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "vertexCount" -> vertexCount.copyFrom(value)
            "inset" -> starNess.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "autoAlign" -> autoAlign = value
            "nearestFiltering" -> nearestFiltering = value
            else -> super.readBool(name, value)
        }
    }

    companion object {

        val sqrt2 = sqrt(2f)
        val meshTimeout = 1000L
        private const val minEdges = 3
        private val maxEdges = DefaultConfig["polygon.maxEdges", 1000]

        fun getBuffer(n: Int, hasDepth: Boolean): StaticFloatBuffer {
            if(n < minEdges) return getBuffer(minEdges, hasDepth)
            if(n > maxEdges) return getBuffer(maxEdges, hasDepth)
            val cached = Cache.getEntry("Mesh", "Polygon", n * 2 + (if(hasDepth) 1 else 0), meshTimeout){
                SFBufferData(createBuffer(n, hasDepth))
            } as SFBufferData
            return cached.buffer
        }

        private fun createBuffer(n: Int, hasDepth: Boolean): StaticFloatBuffer {

            val frontCount = n * 3
            val sideCount = n * 6
            val vertexCount = if(hasDepth) frontCount * 2 + sideCount else frontCount
            val buffer = StaticFloatBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), vertexCount)
            val angles = FloatArray(n+1){ i -> (i*Math.PI*2.0/n).toFloat() }
            val sin = angles.map { sin(it)*.5f+.5f }
            val cos = angles.map { -cos(it)*.5f+.5f }

            val d1 = -1f
            val d2 = +1f

            val outline = ArrayList<Vector4f>()
            for(i in 0 until n){
                val inset = if(i % 2 == 0) 1f else 0f
                outline.add(Vector4f(sin[i], cos[i], inset, 1f))
            }

            outline.add(outline.first())

            fun putCenter(depth: Float){
                buffer.put(0.5f)
                buffer.put(0.5f)
                buffer.put(depth)
                buffer.put(0f)
                buffer.put(0f)
            }

            fun put(out: Vector4f, depth: Float){
                buffer.put(out.x)
                buffer.put(out.y)
                buffer.put(depth)
                buffer.put(out.z)
                buffer.put(out.w)
            }

            fun putFront(depth: Float){
                for(i in 0 until n){

                    putCenter(depth)
                    put(outline[i], depth)
                    put(outline[i+1], depth)

                }
            }

            if(hasDepth){
                putFront(d1)
                putFront(d2)
            } else {
                putFront(0f)
            }

            if(hasDepth){
                for(i in 0 until n){

                    // 012
                    put(outline[i], d1)
                    put(outline[i+1], d1)
                    put(outline[i+1], d2)

                    // 023
                    put(outline[i], d1)
                    put(outline[i+1], d2)
                    put(outline[i], d2)

                }
            }

            return buffer
        }
    }

}