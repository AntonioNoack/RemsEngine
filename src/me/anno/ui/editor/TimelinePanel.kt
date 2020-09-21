package me.anno.ui.editor

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.GFX.openMenu
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.targetDuration
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer.Companion.isCross
import me.anno.ui.editor.graphs.GraphEditorBody
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.mixARGB
import me.anno.utils.pow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class TimelinePanel(style: Style) : Panel(style) {

    val accentColor = style.getColor("accentColor", DefaultStyle.black)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawBackground()
        drawTimeAxis(x0, y0, x1, y1, true)
    }

    // time
    companion object {

        var dtHalfLength = 30.0
        var centralTime = dtHalfLength

        val timeFractions = listOf(
            0.2f, 0.5f,
            1f, 2f, 5f, 10f, 20f, 30f, 60f,
            120f, 300f, 600f, 1200f, 1800f, 3600f,
            3600f * 1.5f, 3600f * 2f, 3600f * 5f,
            3600f * 6f, 3600f * 12f, 3600f * 24f
        )

        fun clampTime() {
            dtHalfLength = clamp(dtHalfLength, 2.0 / Studio.targetFPS, timeFractions.last().toDouble())
            centralTime = max(centralTime, dtHalfLength)
        }

        val movementSpeed get() = 0.05f * sqrt(GFX.width * GFX.height.toFloat())

        val propertyDt get() = 10f * dtHalfLength / GFX.width

        fun moveRight(sign: Float) {
            val delta = sign * dtHalfLength * 0.05f
            Studio.editorTime += delta
            Studio.updateAudio()
            centralTime += delta
            clampTime()
        }

        data class TimestampKey(val time: Double, val step: Double)

        val timestampCache = HashMap<TimestampKey, String>()


        fun get0XString(time: Int) = if (time < 10) "0$time" else "$time"
        fun get00XString(time: Int) = if (time < 100) "00$time" else if (time < 10) "0$time" else "$time"

        fun getTimeString(time: Double, step: Double): String {
            val key = TimestampKey(time, step)
            val old = timestampCache[key]
            if (old != null) return old
            if (timestampCache.size > 500) timestampCache.clear()
            val solution =
                if (time < 0) "-${getTimeString(-time, step)}"
                else {
                    val s = time.toInt()
                    val m = s / 60
                    val h = m / 60
                    val subTime = ((time % 1) * Studio.targetFPS).roundToInt()
                    if (h < 1) "${get0XString(m % 60)}:${get0XString(s % 60)}${if (step < 1f) "/${get0XString(subTime)}" else ""}"
                    else "${get0XString(h)}:${get0XString(m % 60)}:${get0XString(s % 60)}${if (step < 1f) "/${get0XString(
                        subTime
                    )}" else ""}"
                }
            timestampCache[key] = solution
            return solution
        }
    }

    val tinyFontSize = style.getSize("tinyTextSize", 10)
    val fontColor = style.getColor("textColor", DefaultStyle.fontGray)
    val fontName = style.getString("textFont", DefaultConfig.defaultFont)
    val isBold = style.getBoolean("textBold", false)
    val isItalic = style.getBoolean("textItalic", false)
    val endColor = style.getColor("endColor", mixARGB(fontColor, 0xffff0000.toInt(), 0.5f))

    fun normTime01(time: Double) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normTime01(time: Float) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normAxis11(lx: Float, x0: Int, size: Int) = (lx - x0) / size * 2f - 1f

    fun getTimeAt(mx: Float) = centralTime + dtHalfLength * normAxis11(mx, x, w)
    fun getXAt(time: Double) = x + w * normTime01(time)
    fun getXAt(time: Float) = x + w * normTime01(time)

    fun drawTimeAxis(x0: Int, y0: Int, x1: Int, y1: Int, drawText: Boolean) {

        val y0 = if (drawText) y0 else y0 - (2 + tinyFontSize)

        // make the step amount dependent on width and font size
        val deltaFrame = 500 * dtHalfLength * tinyFontSize / w

        val timeStep = getTimeStep(deltaFrame * 0.2)

        val strongLineColor = fontColor and 0x4fffffff
        val fineLineColor = fontColor and 0x1fffffff
        val veryFineLineColor = fontColor and 0x10ffffff

        // very fine lines, 20x as many
        drawTimeAxis(timeStep * 0.05, x0, y0, x1, y1, veryFineLineColor, false)

        // fine lines, 5x as many
        drawTimeAxis(timeStep * 0.2, x0, y0, x1, y1, fineLineColor, drawText)

        // strong lines
        drawTimeAxis(timeStep, x0, y0, x1, y1, strongLineColor, false)

        drawLine(targetDuration, y0, y1, endColor)
        drawLine(editorTime, y0, y1, accentColor)

    }

    fun drawTimeAxis(
        timeStep: Double, x0: Int, y0: Int, x1: Int, y1: Int,
        lineColor: Int, drawText: Boolean
    ) {

        val minFrame = centralTime - dtHalfLength
        val maxFrame = centralTime + dtHalfLength

        val minStepIndex = (minFrame / timeStep).toLong() - 1
        val maxStepIndex = (maxFrame / timeStep).toLong() + 1

        val fontSize = tinyFontSize
        val fontName = fontName
        val isBold = isBold
        val isItalic = isItalic
        val fontColor = fontColor
        val backgroundColor = backgroundColor

        val lineY = y0 + 2 + fontSize
        val lineH = y1 - y0 - 4 - fontSize

        // splitting this results in 30% less time used
        // probably because of program switching
        // 8% more are gained by assigning the color only once
        if(lineH > 0){
            GFX.flatColor(lineColor)
            for (stepIndex in maxStepIndex downTo minStepIndex) {
                val time = stepIndex * timeStep
                val x = getXAt(time).roundToInt()
                if (x > x0 + 1 && x + 2 < x1) {
                    GFX.drawRect(x, lineY, 1, lineH)
                }
            }
        }

        if (drawText) {
            for (stepIndex in maxStepIndex downTo minStepIndex) {
                val time = stepIndex * timeStep
                val x = getXAt(time).roundToInt()
                if (x > x0 + 1 && x + 2 < x1) {
                    val text = getTimeString(time, timeStep)
                    GFX.drawText(
                        x, y0, fontName, fontSize, isBold, isItalic,
                        text, fontColor, backgroundColor, -1, true
                    )
                }
            }
        }

    }

    fun drawLine(time: Double, y0: Int, y1: Int, color: Int) {
        GFX.drawRect(getXAt(time).roundToInt(), y0 + 2, 1, y1 - y0 - 4, color)
    }


    fun getTimeStep(time: Double): Double {
        return timeFractions.minBy { abs(it - time) }!!.toDouble()
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            isCross(x, y) -> super.onMouseClicked(x, y, button, long)
            button.isLeft -> jumpToX(x)
            else -> {
                val options = listOf(
                    "Set End Here" to {
                        Studio.project?.targetDuration = getTimeAt(x)
                    },
                    "Jump to Start" to {
                        jumpToT(0.0)
                    },
                    "Jump to End" to {
                        jumpToT(targetDuration)
                    }
                )
                openMenu(options)
            }
        }
    }

    fun jumpToX(x: Float) = jumpToT(getTimeAt(x))
    fun jumpToT(t: Double) {
        editorTime = t
        Studio.updateInspector()
        Studio.updateAudio()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = getTimeAt(x)
        if (0 in Input.mouseKeysDown) {
            if((Input.isShiftDown || Input.isControlDown) && editorTimeDilation == 0.0){
                // scrubbing
                editorTime = getTimeAt(x)
            } else {
                // move left/right
                val dt = dx * dtHalfLength / (w / 2)
                centralTime -= dt
                clampTime()
            }
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val delta = dx - dy
        val scale = pow(1.05f, delta)
        if (Input.isControlDown) { // zoom
            // set the center to the cursor
            // works great :D
            val normalizedX = (x - w / 2) / (w / 2)
            centralTime += normalizedX * dtHalfLength * (1f - scale)
            dtHalfLength *= scale
        } else { // move
            centralTime += dtHalfLength * 20f * delta / w
        }
        clampTime()
    }


}