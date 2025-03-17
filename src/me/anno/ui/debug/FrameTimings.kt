package me.anno.ui.debug

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.OSWindow
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.popBetterBlending
import me.anno.gpu.drawing.DrawTexts.pushBetterBlending
import me.anno.gpu.drawing.GFXx2D.noTiling
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Texture2D
import me.anno.io.xml.ComparableStringBuilder
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.float32ToFloat16
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Displays a set of graphs, where each graph is defined by its color.
 * By default, this class is only used for frame times, but it also can show simulation timings and such.
 * */
object FrameTimings : Panel(DefaultConfig.style.getChild("fps")) {

    class TimeContainer(val width: Int, val color: Int) : Comparable<TimeContainer> {

        var maxValue = 0f
        var average = 0f
        val values = FloatArray(width)
        var nextIndex = 0
        var fillLevel = 0

        fun putValue(value: Float) {
            values[nextIndex] = value
            nextIndex = (nextIndex + 1) % width
            fillLevel = min(fillLevel + 1, width1)
            val max = values.maxOrNull() ?: 0f // max() causes issues with some Java versions
            maxValue = max(maxValue * Maths.clamp((1f - 3f * value), 0f, 1f), max)
            average = values.sum() / fillLevel
        }

        override fun compareTo(other: TimeContainer): Int {
            return average.compareTo(other.average)
        }
    }

    val width1 = 200 * max(DefaultConfig.style.getSize("fontSize", 12), 12) / 12
    val height1 = width1 / 4

    private val texture = Texture2D("frameTimes", width1, 1, 1)
    private val fp16s = ByteBufferPool.allocateDirect(2 * width1).asShortBuffer()

    private val shader = BaseShader(
        "frameTimes",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, ShaderLib.uvList, listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V4F, "background"),
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V1F, "height")
        ), "" +
                "void main(){\n" +
                "   float v = clamp((texture(tex, uv).x + uv.y - 1.0) * height + 0.5, 0.0, 1.0);\n" +
                "   gl_FragColor = mix(background, color, v);\n" +
                "   if(gl_FragColor.a <= 0.0) discard;\n" +
                "}"
    )

    var textColor = TextPanel("", style).textColor

    override fun calculateSize(w: Int, h: Int) {
        minW = width1
        minH = height1
    }

    private val timeContainer = TimeContainer(width1, textColor)
    private val containers = arrayListOf(timeContainer)

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
        val container = TimeContainer(width1, color)
        containers.add(container)
        container.putValue(value)
    }

    val withoutInterpolation get() = OS.isAndroid

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val containers = containers
        if (containers.isEmpty()) {
            drawBackground(x0, y0, x1, y1)
            return
        }

        containers.sortDescending()
        val maxValue = containers[0].maxValue

        var background = backgroundColor
        for (j in containers.indices) {
            val container = containers[j]
            val nextIndex = container.nextIndex
            val values = container.values
            val barColor = container.color
            val width = values.size
            val indexOffset = nextIndex - 1 + width

            if (withoutInterpolation) {

                if (j == 0) drawBackground(x0, y0, x1, y1)

                var lastX = x0
                var lastBarHeight = 0
                val scale = height1 / maxValue

                val b = DrawRectangles.startBatch()
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
                DrawRectangles.finishBatch(b)
            } else {

                // it might be faster to draw this with batching 😄
                // -> I compared it to withoutInterpolation, and
                // it's the same speed on my RTX 3070

                val scale = 1f / maxValue
                for (x in x0 until x1) {
                    val i = x - this.x
                    val v = values[(indexOffset + i) % width]
                    fp16s.put(x - x0, float32ToFloat16(max(v * scale, 0f)).toShort())
                }

                texture.createMonochromeFP16(fp16s, false)

                GFX.check()
                val shader = shader.value
                shader.use()
                shader.v1f("height", height1.toFloat())
                posSize(shader, x, y, width, height)
                shader.v4f("color", barColor)
                shader.v4f("background", background)
                noTiling(shader)
                texture.bindTrulyNearest(0)
                SimpleBuffer.flat01.draw(shader)
                GFX.check()

                background = background.withAlpha(0)
            }
        }
    }

    // to reduce draw calls by bundling stacks of the same height
    fun drawLine(lastX: Int, nextX: Int, barHeight: Int, barColor: Int) {
        if (lastX < nextX) {
            DrawRectangles.drawRect(lastX, y + height1 - barHeight, nextX - lastX, barHeight, barColor)
        }
    }

    val text = ComparableStringBuilder("theFPS, min: theFPS")

    fun add(nanos: Long, color: Int) {
        putValue(nanos * 1e-9f, color)
    }

    fun showFPS(window: OSWindow) {
        val x0 = max(0, window.width - width1)
        val y0 = max(0, window.height - height1)
        showFPS(x0, y0)
    }

    private fun showFPS(x0: Int, y0: Int) {
        setPosSize(x0, y0, width1, height1)

        canBeSeen = true
        draw(x, y, x + width, y + height)

        formatNumber(text.value, 0, 6, Time.currentFPS.toFloat())
        formatNumber(text.value, 13, 6, Time.currentMinFPS.toFloat())

        val pad = 2
        val x = pushBetterBlending(true)
        drawSimpleTextCharByChar(
            x0 + pad, y0 + pad, pad, text,
            textColor, backgroundColor.withAlpha(180)
        )
        popBetterBlending(x)
    }

    fun getChar(digit: Int) = ((digit % 10) + '0'.code).toChar()

    fun formatNumber(chars: CharArray, index: Int, space: Int, number: Float) {
        if (number.isFinite()) {
            if (number < 0) {
                chars[index] = '-'
                formatNumber(chars, index + 1, space - 1, -number)
                return
            }
            if (number >= 999.5f) {
                formatNumber(chars, index, space, number.roundToInt()) // is NaN-safe
            }
            val numberX = (number * 10).roundToInt() // is NaN-safe
            chars[index + space - 2] = '.'
            chars[index + space - 1] = getChar(numberX)
            formatNumber(chars, index, space - 2, numberX / 10)
        } else {
            chars[index] = 'N'
            chars[index + 1] = 'a'
            chars[index + 2] = 'N'
        }
    }

    fun formatNumber(chars: CharArray, index: Int, space: Int, number: Int) {

        if (number < 0) {
            chars[index] = '-'
            formatNumber(chars, index + 1, space - 1, -number)
            return
        }

        var limit = 1
        for (i in 0 until space) {
            limit *= 10
        }

        if (number >= limit) {
            // all 9, maybe an x for extra much?
            chars.fill('x', index, index + space)
            return
        }

        // can print it :)
        printNumber(chars, index, space, number)
    }

    fun printNumber(chars: CharArray, index: Int, space: Int, number: Int) {
        // can print it :)
        chars[index + space - 1] = getChar(number)
        if (space > 1) {
            // we need to print more
            if (number >= 10) {
                printNumber(chars, index, space - 1, number / 10)
            } else {
                chars.fill(' ', index, index + space - 1)
            }
        }
    }
}