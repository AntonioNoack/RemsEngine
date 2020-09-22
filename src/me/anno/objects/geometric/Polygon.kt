package me.anno.objects.geometric

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.objects.cache.StaticFloatBufferData
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.files.hasValidName
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Polygon(parent: Transform? = null): GFXTransform(parent){

    // todo round edges?

    var texture = File("")
    var autoAlign = false
    var filtering = FilteringMode.LINEAR

    var is3D = false
    var vertexCount = AnimatedProperty.intPlus(5)
    var starNess = AnimatedProperty.float01()

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f){
        val inset = clamp(starNess[time], 0f, 1f)
        val image = Cache.getImage(texture, 5000, true)
        if(image == null && texture.hasValidName() && GFX.isFinalRendering) throw MissingFrameException(texture)
        val texture = image ?: whiteTexture
        val count = vertexCount[time]//.roundToInt()
        if(inset == 1f && count % 2 == 0) return// invisible
        val selfDepth = scale[time].z
        stack.pushMatrix()
        if(autoAlign){
            stack.rotate(toRadians(if(count == 4) 45f else 90f), zAxis)
            stack.scale(sqrt2, sqrt2, if(is3D) 1f else 0f)
        } else if(!is3D){
            stack.scale(1f, 1f, 0f)
        }
        GFX.draw3DPolygon(stack, getBuffer(count, selfDepth > 0f), texture, color,
            inset, filtering, ClampMode.CLAMP)
        stack.popMatrix()
        return
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += VI("Vertex Count", "Quads, Triangles, all possible", vertexCount, style)
        list += VI("Star-ness", "Works best with even vertex count", starNess, style)
        list += VI("Pattern Texture", "For patterns like gradients radially; use a mask layer for images with polygon shape", null, texture, style){ texture = it }
        list += VI("Filtering", "Pixelated or soft look of pixels?", null, filtering, style){ filtering = it }
        list += VI("Auto-Align", "Rotate 45°/90, and scale a bit; for rectangles", null, autoAlign, style){ autoAlign = it }
        list += VI("Extrude", "Makes it 3D", null, is3D, style){ is3D = it }
    }

    override fun getClassName(): String = "Polygon"

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "vertexCount", vertexCount)
        writer.writeObject(this, "inset", starNess)
        writer.writeBool("autoAlign", autoAlign)
        writer.writeBool("is3D", is3D)
        writer.writeInt("filtering", filtering.id, true)
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

    override fun readInt(name: String, value: Int) {
        when(name){
            "filtering" -> filtering = filtering.find(value)
            else -> super.readInt(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "autoAlign" -> autoAlign = value
            "is3D" -> is3D = value
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
            val cached = Cache.getEntry("Mesh", "Polygon", n * 2 + (if(hasDepth) 1 else 0),
                meshTimeout, false){
                StaticFloatBufferData(createBuffer(n, hasDepth))
            } as StaticFloatBufferData
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