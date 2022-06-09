package me.anno.ui.debug

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.simpleVertexShader
import me.anno.gpu.shader.ShaderLib.simpleVertexShaderV2
import me.anno.gpu.shader.ShaderLib.simpleVertexShaderV2List
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.ui.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

object FrameTimes : Panel(DefaultConfig.style.getChild("fps")) {

    val width = 200 * max(DefaultConfig.style.getSize("fontSize", 12), 12) / 12
    val height = width / 4

    private val texture = Texture2D("frameTimes", width, 1, 1)
    private val floats = Texture2D.bufferPool[4 * width, false, false]
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val shader = BaseShader(
        "frameTimes",
        simpleVertexShaderV2List,
        simpleVertexShaderV2, uvList, listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V4F, "background"),
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V1F, "height")
        ), "" +
                "void main(){\n" +
                "   float v = min((texture(tex, uv).x + uv.y - 1.0) * height + 0.5, 1.0);\n" +
                "   if(v <= 0.0) discard;\n" +
                "   gl_FragColor = mix(background, color, v);\n" +
                "}"
    )

    val colors = TextPanel("", style)
    val textColor = colors.textColor

    override fun calculateSize(w: Int, h: Int) {
        minW = width
        minH = height
    }

    val timeContainer = TimeContainer(width, textColor)
    val containers = arrayListOf(timeContainer)

    fun putTime(value: Float) {
        putValue(value, textColor)
    }

    fun putValue(value: Float, color: Int) {
        val containers = containers
        for (i in containers.indices) {
            val container = containers[i]
            if (container.color == color) {
                container.putValue(value)
                return
            }
        }
        val container = TimeContainer(width, color)
        containers.add(container)
        container.putValue(value)
    }

    fun draw() {
        canBeSeen = true
        draw(x, y, x + w, y + h)
    }

    val withoutInterpolation get() = OS.isAndroid

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground(x0, y0, x1, y1)

        val containers = containers
        if (containers.isEmpty()) return

        containers.sortDescending()
        val maxValue = containers[0].maxValue

        for (j in containers.indices) {
            val container = containers[j]
            val nextIndex = container.nextIndex
            val values = container.values
            val barColor = container.color
            val width = width
            val indexOffset = nextIndex - 1 + width

            if (withoutInterpolation) {

                var lastX = x0
                var lastBarHeight = 0
                val scale = height / maxValue

                for (x in x0 until x1) {
                    val i = x - this.x
                    val v = values[(indexOffset + i) % width]
                    val barHeight = (v * scale).toInt()
                    if (barHeight != lastBarHeight) {
                        drawLine(lastX, x, lastBarHeight, barColor)
                        lastX = x
                        lastBarHeight = barHeight
                    }
                }

                drawLine(lastX, x1, lastBarHeight, barColor)

            } else {

                val scale = 1f / maxValue
                for (x in x0 until x1) {
                    val i = x - this.x
                    val v = values[(indexOffset + i) % width]
                    floats.put(x - x0, max(v * scale, 0f))
                }

                texture.createMonochromeFP16(floats, false)

                GFX.check()
                val shader = shader.value
                shader.use()
                shader.v1f("height", height.toFloat())
                GFXx2D.posSize(shader, x, y, w, h)
                shader.v4f("color", barColor)
                shader.v4f("background", backgroundColor)
                GFXx2D.noTiling(shader)
                texture.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                GFX.flat01.draw(shader)
                GFX.check()

            }
        }
    }

    // to reduce draw calls by bundling stacks of the same height
    fun drawLine(lastX: Int, nextX: Int, barHeight: Int, barColor: Int) {
        if (lastX < nextX) {
            drawRect(lastX, y + height - barHeight, nextX - lastX, barHeight, barColor)
        }
    }

}