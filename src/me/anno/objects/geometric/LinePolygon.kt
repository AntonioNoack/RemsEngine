package me.anno.objects.geometric

import me.anno.cache.CacheSection
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.y3D
import me.anno.gpu.shader.Shader
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.animation.AnimatedProperty
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.builder.Variable
import me.anno.objects.attractors.EffectColoring
import me.anno.objects.attractors.EffectMorphing
import me.anno.objects.modes.UVProjection
import me.anno.studio.rems.Scene.noiseFunc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.mulAlpha
import me.anno.utils.types.Vectors.times
import org.joml.*
import kotlin.math.floor

/**
 * children should be circles or polygons,
 * creates a outline shape, which can be animated
 * like https://youtu.be/ToTWaZtGOj8?t=87 (1:27, Linus Tech Tips, Upload date Apr 28, 2021)
 * */
class LinePolygon(parent: Transform? = null) : GFXTransform(parent) {

    // todo if closed, modulo positions make sense, so a line could swirl around multiple times

    override val className get() = "LinePolygon"
    override val defaultDisplayName: String = "Line"

    val startOffset = AnimatedProperty.float(0f)
    val endOffset = AnimatedProperty.float(0f)
    val lineStrength = AnimatedProperty.float01(1f)
    var fadingOnEnd = AnimatedProperty.floatPlus(0.1f)

    var isClosed = AnimatedProperty.float01()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "startOffset", startOffset)
        writer.writeObject(this, "endOffset", endOffset)
        writer.writeObject(this, "lineStrength", lineStrength)
        writer.writeObject(this, "fadingOnEnd", fadingOnEnd)
        writer.writeObject(this, "isClosed", isClosed)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "startOffset" -> startOffset.copyFrom(value)
            "endOffset" -> endOffset.copyFrom(value)
            "lineStrength" -> lineStrength.copyFrom(value)
            "fadingOnEnd" -> fadingOnEnd.copyFrom(value)
            "isClosed" -> isClosed.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val group = getGroup("Line", "Properties of the line", "line")
        group += vi("Start Offset", "", startOffset, style)
        group += vi("End Offset", "", endOffset, style)
        group += vi("Line Strength", "", lineStrength, style)
        group += vi("Is Closed", "", isClosed, style)
        group += vi("Fading", "How much the last points fade, if the offsets exclude everything", fadingOnEnd, style)
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        // todo coloring and morphing needs to be applied to the children

        val points = children.filter { it !is EffectMorphing && it !is EffectColoring }
        if (points.isEmpty()) {
            super.onDraw(stack, time, color)
        } else if (points.size == 1) {
            drawChild(stack, time, color, children[0])
        } else {

            val transforms = points.map { it.getLocalTransform(time, this) }
            val positions = transforms.map { getPosition(it) }

            val times = points.map { it.getLocalTime(time) }
            val colors = points.mapIndexed { index, it -> it.getLocalColor(color, times[index]) }

            val lineStrength = lineStrength[time] * 0.5f

            val scales = points.mapIndexed { index, it ->
                val s = it.scale[times[index]]
                (s.x() + s.y()) * lineStrength
            }

            val size = points.size
            val lastIndex = size - 1

            fun getPosition(i0: Int) = positions[i0]
            fun getPosition(f0: Float): Vector3f {
                val i0 = floor(f0).toInt()
                if (i0 >= lastIndex) return positions[lastIndex]
                return Vector3f(getPosition(i0)).lerp(getPosition(i0 + 1), fract(f0))
            }

            fun getScale(i0: Int) = scales[i0]
            fun getScale(f0: Float): Float {
                val i0 = floor(f0).toInt()
                if (i0 >= lastIndex) return scales[lastIndex]
                return Maths.mix(getScale(i0), getScale(i0 + 1), fract(f0))
            }

            fun getColor(i0: Int) = colors[i0]
            fun getColor(f0: Float): Vector4f {
                val i0 = floor(f0).toInt()
                if (i0 >= lastIndex) return colors[lastIndex]
                return Vector4f(getColor(i0)).lerp(getColor(i0 + 1), fract(f0))
            }

            fun drawChild(index: Int, fraction: Float) {
                // draw child at fraction
                val localTransform = mix(transforms[index], transforms[index + 1], fraction)
                val c0 = children[index]
                // get interpolated color
                stack.next {
                    stack.mul(localTransform)
                    c0.drawDirectly(stack, time, color, getColor(index + fraction))
                }
            }

            fun drawChild(index: Int) {
                drawChild(stack, time, color, children[index])
            }

            val lengths = FloatArray(transforms.size + 1)
            var length = 0f
            for (i in 1 until points.size) {
                length += positions[i - 1].distance(positions[i])
                lengths[i] = length
            }
            lengths[points.size] = Float.POSITIVE_INFINITY // use sth realistic instead?

            // todo if start is negative or end is negative, extrapolate
            val start = startOffset[time]
            val end = length - endOffset[time]

            // draw end point fading
            if (start > length) {
                val fading = 1f - (start - length) / fadingOnEnd[time]
                if (fading > 0f) {
                    drawChild(stack, time, color.mulAlpha(fading), children.last())
                }
                return
            }

            // draw start point fading
            if (end < 0f) {
                val fading = 1f + end / fadingOnEnd[time]
                if (fading > 0f) {
                    drawChild(stack, time, color.mulAlpha(fading), children.first())
                }
                return
            }

            val mappedIndices = ArrayList<Float>()
            for (i in points.indices) {
                val l = lengths[i]
                if (l in start..end) {
                    if (i > 0) {
                        if (mappedIndices.isEmpty()) {
                            // add previous fract index
                            val lx = lengths[i - 1]
                            val fraction = (l - start) / (l - lx)
                            if (fraction < 1f) {
                                mappedIndices.add(i - fraction)
                            }
                        }
                    }
                    mappedIndices.add(i.toFloat())
                    if (i + 1 < points.size) {
                        val lx = lengths[i + 1]
                        if (lx > end) {
                            // add next fract index
                            val fraction = (end - l) / (lx - l)
                            if (fraction > 0f) {
                                mappedIndices.add(i + fraction)
                            }
                            break
                        }
                    }
                }
            }

            // todo if angle < 90°, use the projected, correct positions

            // todo use correct forward direction
            val forward = Vector3f(0f, 0f, 1f)

            val shader = shader.value.value

            fun drawSegment(i0: Float, i1: Float, alpha: Float) {
                val p0 = getPosition(i0)
                val p1 = getPosition(i1)
                val delta = p1 - p0
                val deltaNorm = Vector3f(delta).normalize()
                val dx = deltaNorm.cross(forward)
                val d0 = dx * (getScale(i0) * alpha)
                val d1 = dx * (getScale(i1) * alpha)
                drawSegment(
                    shader,
                    Vector3f(p0).add(d0), Vector3f(p0).sub(d0),
                    Vector3f(p1).add(d1), Vector3f(p1).sub(d1),
                    getColor(i0).mulAlpha(alpha),
                    getColor(i1).mulAlpha(alpha),
                    stack
                )
            }

            shader.use()
            uploadAttractors(shader, time)
            for (i in 1 until mappedIndices.size) {
                val i0 = mappedIndices[i - 1]
                val i1 = mappedIndices[i]
                drawSegment(i0, i1, 1f)
            }

            val isClosed = isClosed[time]
            if (isClosed > 0f && mappedIndices.size > 2) {
                drawSegment(mappedIndices.first(), mappedIndices.last(), isClosed)
            }

            for (i in mappedIndices) {
                val ii = i.toInt()
                if (ii.toFloat() == i) {
                    drawChild(ii)
                } else {
                    drawChild(ii, fract(i))
                }
            }

        }

    }

    fun mix(m0: Matrix4f, m1: Matrix4f, f: Float): Matrix4f {
        return m0.lerp(m1, f, Matrix4f())
    }

    private fun drawSegment(
        shader: Shader,
        a0: Vector3f, a1: Vector3f,
        b0: Vector3f, b1: Vector3f,
        c0: Vector4f, c1: Vector4f,
        stack: Matrix4fArrayList
    ) {
        GFX.check()
        shader.use()
        GFXx3D.shader3DUniforms(shader, stack, -1)
        shader.v3f("pos0", a0)
        shader.v3f("pos1", a1)
        shader.v3f("pos2", b0)
        shader.v3f("pos3", b1)
        shader.v4f("col0", c0)
        shader.v4f("col1", c1)
        UVProjection.Planar.getBuffer().draw(shader)
        GFX.check()
    }

    private fun getPosition(m0: Matrix4f) = m0.getTranslation(Vector3f())

    override fun drawChildrenAutomatically(): Boolean = false

    companion object {
        val cache = CacheSection("LineCache")
        val shader = lazy {
            ShaderLib.createShaderPlus(
                "linePolygon", ShaderLib.v3DBase +
                        "$attribute vec3 attr0;\n" +
                        "$attribute vec2 attr1;\n" +
                        "uniform vec4 tiling;\n" +
                        "uniform vec3 pos0, pos1, pos2, pos3;\n" +
                        "uniform vec4 col0, col1;\n" +
                        "void main(){\n" +
                        "   vec2 att = attr0.xy*0.5+0.5;\n" +
                        "   localPosition = mix(mix(pos0, pos1, att.x), mix(pos2, pos3, att.x), att.y);\n" +
                        "   gl_Position = transform * vec4(localPosition, 1.0);\n" +
                        ShaderLib.flatNormal +
                        ShaderLib.positionPostProcessing +
                        "   uv = attr1;\n" +
                        "   uvw = attr0;\n" +
                        "   colX = mix(col0, col1, att.y);\n" +
                        "}", y3D + Variable(GLSLType.V4F,"colX"), "" +
                        ShaderLib.getTextureLib +
                        ShaderLib.getColorForceFieldLib +
                        noiseFunc +
                        "void main(){\n" +
                        "   vec4 color = colX;\n" +
                        "   if(${ShaderLib.hasForceFieldColor}) color *= getForceFieldColor();\n" +
                        // does work, just the error should be cleaner...
                        // "   gl_FragDepth += 0.01 * random(uv);\n" +
                        "   gl_FragColor = color;\n" +
                        "}", listOf()
            )
        }
    }

}