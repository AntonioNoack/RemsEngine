package me.anno.ui.editor

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultStyle
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.GFX.openMenu
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.GFXx2D.flatColor
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.isPaused
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.targetDuration
import me.anno.studio.rems.RemsStudio.targetFPS
import me.anno.studio.StudioBase.Companion.updateAudio
import me.anno.studio.rems.Selection
import me.anno.ui.base.Panel
import me.anno.ui.custom.CustomContainer.Companion.isCross
import me.anno.ui.style.Style
import me.anno.utils.*
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.fract
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.mixARGB
import me.anno.utils.Maths.pow
import me.anno.utils.StringHelper.formatTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

open class TimelinePanel(style: Style) : Panel(style) {

    override fun getVisualState(): Any? = Quad(dtHalfLength, centralTime, editorTime, targetDuration)

    var drawnStrings = ArrayList<String>(64)

    val accentColor = style.getColor("accentColor", DefaultStyle.black)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawnStrings.clear()
        drawBackground()
        drawTimeAxis(x0, y0, x1, y1, true)
    }

    companion object {

        var centralValue = 0f
        var dvHalfHeight = 1f

        var dtHalfLength = 30.0
        var centralTime = dtHalfLength

        val timeFractions = listOf(
            0.2f, 0.5f,
            1f, 2f, 5f, 10f, 20f, 30f, 60f,
            120f, 300f, 600f, 1200f, 1800f, 3600f,
            3600f * 1.5f, 3600f * 2f, 3600f * 5f,
            3600f * 6f, 3600f * 12f, 3600f * 24f
        )

        var lastOwner: Transform = RemsStudio.root

        fun updateLocalTime(){

            lastOwner = Selection.selectedTransform ?: lastOwner

            val owner = lastOwner
            val child2root = owner.listOfInheritance.toList()
            val root2child = child2root.reversed()

            // only simple time transforms are supported
            time0 = 0.0
            time1 = 1.0

            root2child.forEach { t ->
                // localTime0 = (parentTime - timeOffset) * timeDilation
                time0 = (time0 - t.timeOffset) * t.timeDilation
                time1 = (time1 - t.timeOffset) * t.timeDilation
            }

            // make sure the values are ok-ish
            if (abs(time1 - time0) !in 0.001..1000.0) {
                time0 = 0.0
                time1 = 1.0
            }

        }

        var time0 = 0.0
        var time1 = 1.0

        fun kf2Global(t: Double) = (t - time0) / (time1 - time0)
        fun global2Kf(t: Double) = mix(time0, time1, t)

        fun clampTime() {
            dtHalfLength = clamp(dtHalfLength, 2.0 / targetFPS, timeFractions.last().toDouble())
            // centralTime = max(centralTime, dtHalfLength)
        }

        val movementSpeed get() = 0.05f * sqrt(GFX.width * GFX.height.toFloat())

        val propertyDt get() = 10f * dtHalfLength / GFX.width

        fun moveRight(sign: Float) {
            val delta = sign * dtHalfLength * 0.05f
            editorTime += delta
            updateAudio()
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
                    val subTime = ((time % 1) * targetFPS).roundToInt()
                    if (h < 1) "${get0XString(m % 60)}:${get0XString(s % 60)}${if (step < 1f) "/${get0XString(subTime)}" else ""}"
                    else "${get0XString(h)}:${get0XString(m % 60)}:${get0XString(s % 60)}${if (step < 1f) "/${get0XString(
                        subTime
                    )}" else ""}"
                }
            timestampCache[key] = solution
            return solution
        }
    }

    val font = style.getFont("tinyText", defaultFont)
    val fontColor = style.getColor("textColor", DefaultStyle.fontGray)
    val endColor = style.getColor("endColor", mixARGB(fontColor, 0xffff0000.toInt(), 0.5f))

    fun normTime01(time: Double) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normTime01(time: Float) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normAxis11(lx: Float, x0: Int, size: Int) = (lx - x0) / size * 2f - 1f

    fun getTimeAt(mx: Float) = centralTime + dtHalfLength * normAxis11(mx, x, w)
    fun getXAt(time: Double) = x + w * normTime01(time)
    fun getXAt(time: Float) = x + w * normTime01(time)

    fun drawTimeAxis(x0: Int, y0: Int, x1: Int, y1: Int, drawText: Boolean) {

        val y0 = if (drawText) y0 else y0 - (2 + font.sizeInt)

        // make the step amount dependent on width and font size
        val deltaFrame = 500 * dtHalfLength * font.size / w

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

    override fun tickUpdate() {
        super.tickUpdate()
        drawnStrings.forEach { text -> FontManager.getString(font, text, -1) }
    }

    fun drawTimeAxis(
        timeStep: Double, x0: Int, y0: Int, x1: Int, y1: Int,
        lineColor: Int, drawText: Boolean
    ) {

        val minFrame = centralTime - dtHalfLength
        val maxFrame = centralTime + dtHalfLength

        val minStepIndex = (minFrame / timeStep).toLong() - 1
        val maxStepIndex = (maxFrame / timeStep).toLong() + 1

        val fontSize = font.sizeInt
        val fontColor = fontColor
        val backgroundColor = backgroundColor

        val lineY = y0 + 2 + fontSize
        val lineH = y1 - y0 - 4 - fontSize

        // splitting this results in 30% less time used
        // probably because of program switching
        // 8% more are gained by assigning the color only once
        if(lineH > 0){
            flatColor(lineColor)
            for (stepIndex in maxStepIndex downTo minStepIndex) {
                val time = stepIndex * timeStep
                val x = getXAt(time).roundToInt()
                if (x > x0 + 1 && x + 2 < x1) {
                    drawRect(x, lineY, 1, lineH)
                }
            }
        }

        if (drawText) {
            for (stepIndex in maxStepIndex downTo minStepIndex) {
                val time = stepIndex * timeStep
                val x = getXAt(time).roundToInt()
                if (x > x0 + 1 && x + 2 < x1) {
                    val text = getTimeString(time, timeStep)
                    drawnStrings.add(text)
                    GFXx2D.drawText(
                        x, y0, font,
                        text, fontColor, backgroundColor, -1, true
                    )
                }
            }
        }

    }

    fun drawLine(time: Double, y0: Int, y1: Int, color: Int) {
        drawRect(getXAt(time).roundToInt(), y0 + 2, 1, y1 - y0 - 4, color)
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
                    GFX.MenuOption("Set End Here", "Sets the end of the project to here") {
                        project?.targetDuration = getTimeAt(x)
                    },
                    GFX.MenuOption("Jump to Start", "Set the time to 0") {
                        jumpToT(0.0)
                    },
                    GFX.MenuOption("Jump to End", "Set the time to the end of the project") {
                        jumpToT(targetDuration)
                    }
                )
                openMenu(options)
            }
        }
    }

    fun jumpToX(x: Float) = jumpToT(getTimeAt(x))
    fun jumpToT(t: Double) {
        RemsStudio.largeChange("Timeline jump to ${t.formatTime()}/${(fract(t)*targetFPS).toInt()}"){
            editorTime = t
        }
        updateAudio()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = getTimeAt(x)
        if (0 in Input.mouseKeysDown) {
            if((Input.isShiftDown || Input.isControlDown) && isPaused){
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
        if(Input.isShiftDown || Input.isControlDown){
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
        } else {
            super.onMouseWheel(x, y, dx, dy)
        }
    }


}