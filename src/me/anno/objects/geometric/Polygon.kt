package me.anno.objects.geometric

import me.anno.cache.instances.ImageCache.getImage
import me.anno.cache.instances.MeshCache
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.toRadians
import me.anno.gpu.GFXx3D.draw3DPolygon
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.hasValidName
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.Maths.clamp
import me.anno.video.MissingFrameException
import org.joml.*
import java.io.File
import java.lang.Math
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Polygon(parent: Transform? = null) : GFXTransform(parent) {

    // todo round edges?

    var texture = File("")
    var autoAlign = false
    var filtering = Filtering.LINEAR

    var is3D = false
    var vertexCount = AnimatedProperty.intPlus(5)
    var starNess = AnimatedProperty.float01()

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        val inset = clamp(starNess[time], 0f, 1f)
        val image = getImage(texture, 5000, true)
        if (image == null && texture.hasValidName() && GFX.isFinalRendering) throw MissingFrameException(texture)
        val texture = image ?: whiteTexture
        val count = vertexCount[time]//.roundToInt()
        if (inset == 1f && count % 2 == 0) return// invisible
        val selfDepth = scale[time].z
        stack.next {
            if (autoAlign) {
                stack.rotate(toRadians(if (count == 4) 45f else 90f), zAxis)
                stack.scale(sqrt2, sqrt2, if (is3D) 1f else 0f)
            } else if (!is3D) {
                stack.scale(1f, 1f, 0f)
            }
            draw3DPolygon(
                this, time, stack, getBuffer(count, selfDepth > 0f), texture, color,
                inset, filtering, Clamping.CLAMP
            )
        }
        return
    }

    override fun transformLocally(pos: Vector3fc, time: Double): Vector3fc {
        val count = vertexCount[time]
        val z = if (is3D) pos.z() else 0f
        return if (autoAlign) {
            if (count == 4) {
                Vector3f(0.5f * (pos.x() + pos.y()), 0.5f * (pos.x() - pos.y()), z)
            } else {
                Vector3f(sqrt2, -sqrt2, z)
            }
        } else Vector3f(pos.x(), -pos.y(), z)
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val geo = getGroup("Geometry", "", "geometry")
        geo += vi("Vertex Count", "Quads, Triangles, all possible", "polygon.vertexCount", vertexCount, style)
        geo += vi("Star-ness", "Works best with even vertex count", "polygon.star-ness", starNess, style)
        geo += vi(
            "Auto-Align",
            "Rotate 45°/90, and scale a bit; for rectangles", "polygon.autoAlign",
            null, autoAlign, style
        ) { autoAlign = it }
        geo += vi("Extrude", "Makes it 3D", "polygon.extrusion", null, is3D, style) { is3D = it }
        val tex = getGroup("Pattern", "", "texture")
        tex += vi(
            "Pattern Texture",
            "For patterns like gradients radially; use a mask layer for images with polygon shape", "polygon.pattern",
            null, texture, style
        ) { texture = it }
        tex += vi(
            "Filtering",
            "Pixelated or soft look of pixels?",
            "texture.filtering",
            null, filtering, style
        ) { filtering = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "vertexCount", vertexCount)
        writer.writeObject(this, "inset", starNess)
        writer.writeBoolean("autoAlign", autoAlign)
        writer.writeBoolean("is3D", is3D)
        writer.writeInt("filtering", filtering.id, true)
        writer.writeFile("texture", texture)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "texture" -> texture = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "vertexCount" -> vertexCount.copyFrom(value)
            "inset" -> starNess.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "filtering" -> filtering = filtering.find(value)
            else -> super.readInt(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "autoAlign" -> autoAlign = value
            "is3D" -> is3D = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun getClassName() = "Polygon"
    override fun getDefaultDisplayName() = Dict["Polygon", "obj.polygon"]
    override fun getSymbol() =
        if (vertexCount.isAnimated) DefaultConfig["ui.symbol.polygon.any", "⭐"]
        else when (vertexCount[0.0]) {
            3 -> DefaultConfig["ui.symbol.polygon.3", "△"]
            4 -> DefaultConfig["ui.symbol.polygon.4", "⬜"]
            5 -> DefaultConfig["ui.symbol.polygon.5", "⭐"]
            6 -> DefaultConfig["ui.symbol.polygon.6", "⬡"]
            in 30 until Integer.MAX_VALUE -> DefaultConfig["ui.symbol.polygon.circle", "◯"]
            else -> DefaultConfig["ui.symbol.polygon.any", "⭐"]
        }

    companion object {

        val sqrt2 = sqrt(2f)
        val meshTimeout = 1000L
        private const val minEdges = 3
        private val maxEdges = DefaultConfig["objects.polygon.maxEdges", 1000]

        fun getBuffer(n: Int, hasDepth: Boolean): StaticBuffer {
            if (n < minEdges) return getBuffer(minEdges, hasDepth)
            if (n > maxEdges) return getBuffer(maxEdges, hasDepth)
            return MeshCache.getEntry(
                n * 2 + (if (hasDepth) 1 else 0),
                meshTimeout, false
            ) {
                createBuffer(n, hasDepth)
            } as StaticBuffer
        }

        private fun createBuffer(n: Int, hasDepth: Boolean): StaticBuffer {

            val frontCount = n * 3
            val sideCount = n * 6
            val vertexCount = if (hasDepth) frontCount * 2 + sideCount else frontCount
            val buffer = StaticBuffer(listOf(Attribute("attr0", 3), Attribute("attr1", 2)), vertexCount)
            val angles = FloatArray(n + 1) { i -> (i * Math.PI * 2.0 / n).toFloat() }
            val sin = angles.map { +sin(it) }
            val cos = angles.map { -cos(it) }

            val d1 = -1f
            val d2 = +1f

            val outline = ArrayList<Vector4f>()
            for (i in 0 until n) {
                val inset = if (i % 2 == 0) 1f else 0f
                outline.add(Vector4f(sin[i], cos[i], inset, 1f))
            }

            outline.add(outline.first())

            fun putCenter(depth: Float) {
                buffer.put(0f)
                buffer.put(0f)
                buffer.put(depth)
                buffer.put(0f)
                buffer.put(0f)
            }

            fun put(out: Vector4f, depth: Float) {
                buffer.put(out.x)
                buffer.put(out.y)
                buffer.put(depth)
                buffer.put(out.z)
                buffer.put(out.w)
            }

            fun putFront(depth: Float) {
                for (i in 0 until n) {

                    putCenter(depth)
                    put(outline[i], depth)
                    put(outline[i + 1], depth)

                }
            }

            if (hasDepth) {
                putFront(d1)
                putFront(d2)
            } else {
                putFront(0f)
            }

            if (hasDepth) {
                for (i in 0 until n) {

                    // 012
                    put(outline[i], d1)
                    put(outline[i + 1], d1)
                    put(outline[i + 1], d2)

                    // 023
                    put(outline[i], d1)
                    put(outline[i + 1], d2)
                    put(outline[i], d2)

                }
            }

            return buffer
        }
    }

}