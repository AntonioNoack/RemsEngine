package me.anno.ui.editor

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultStyle
import me.anno.fonts.FontManager
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.GFXx2D.flatColor
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.StudioBase.Companion.updateAudio
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.isPaused
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.targetDuration
import me.anno.studio.rems.RemsStudio.targetFPS
import me.anno.studio.rems.Selection
import me.anno.ui.base.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.custom.CustomContainer.Companion.isCross
import me.anno.ui.style.Style
import me.anno.utils.Color.mulAlpha
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.fract
import me.anno.utils.maths.Maths.mix
import me.anno.utils.maths.Maths.mixARGB
import me.anno.utils.maths.Maths.pow
import me.anno.utils.structures.tuples.Quad
import me.anno.utils.types.Strings.formatTime
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

// todo subpixel adjusted lines, only if subpixel rendering affects x-axis

open class TimelinePanel(style: Style) : Panel(style) {

    override fun getVisualState(): Any? =
        Quad(dtHalfLength, centralTime, editorTime, targetDuration)

    var drawnStrings = ArrayList<TextCacheKey>(64)

    val accentColor = style.getColor("accentColor", DefaultStyle.black)

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        drawnStrings.clear()
        drawBackground()
        drawTimeAxis(x0, y0, x1, y1, true)
    }

    val font = style.getFont("tinyText", defaultFont)
    val fontColor = style.getColor("textColor", DefaultStyle.fontGray)
    val endColor = style.getColor("endColor", mixARGB(fontColor, 0xffff0000.toInt(), 0.5f))

    fun drawCurrentTime() {
        GFX.loadTexturesSync.push(true)
        val text = getTimeString(editorTime, 0.0)
        val color = mixARGB(fontColor, backgroundColor, 0.8f)
        drawSimpleTextCharByChar(x + w / 2, y + h / 2, 0, text, color, backgroundColor, AxisAlignment.CENTER)
        GFX.loadTexturesSync.pop()
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

        fun updateLocalTime() {

            lastOwner = Selection.selectedTransform ?: lastOwner

            val owner = lastOwner
            val child2root = owner.listOfInheritance.toList()
            val root2child = child2root.reversed()

            // only simple time transforms are supported
            time0 = 0.0
            time1 = 1.0

            root2child.forEach { t ->
                // localTime0 = (parentTime - timeOffset) * timeDilation
                val offset = t.timeOffset.value
                val dilation = t.timeDilation.value
                time0 = (time0 - offset) * dilation
                time1 = (time1 - offset) * dilation
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
                    else "${get0XString(h)}:${get0XString(m % 60)}:${get0XString(s % 60)}${
                        if (step < 1f) "/${
                            get0XString(
                                subTime
                            )
                        }" else ""
                    }"
                }
            timestampCache[key] = solution
            return solution
        }
    }

    fun normTime01(time: Double) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normTime01(time: Float) = (time - centralTime) / dtHalfLength * 0.5f + 0.5f
    fun normAxis11(lx: Float, x0: Int, size: Int) = (lx - x0) / size * 2f - 1f

    fun getTimeAt(mx: Float) = centralTime + dtHalfLength * normAxis11(mx, x, w)
    fun getXAt(time: Double) = x + w * normTime01(time)
    fun getXAt(time: Float) = x + w * normTime01(time)

    fun drawTimeAxis(x0: Int, y0: Int, x1: Int, y1: Int, drawText: Boolean) {

        val y02 = if (drawText) y0 else y0 - (2 + font.sizeInt)

        // make the step amount dependent on width and font size
        val deltaFrame = 500 * dtHalfLength * font.size / w

        val timeStep = getTimeStep(deltaFrame * 0.2)

        val strongLineColor = fontColor and 0x4fffffff
        val fineLineColor = fontColor and 0x1fffffff
        val veryFineLineColor = fontColor and 0x10ffffff

        // very fine lines, 20x as many
        drawTimeAxis(timeStep * 0.05, x0, y02, x1, y1, veryFineLineColor, false)

        // fine lines, 5x as many
        drawTimeAxis(timeStep * 0.2, x0, y02, x1, y1, fineLineColor, drawText)

        // strong lines
        drawTimeAxis(timeStep, x0, y02, x1, y1, strongLineColor, false)

        drawLine(targetDuration, y02, y1, endColor)
        drawLine(editorTime, y02, y1, accentColor)

    }

    override fun tickUpdate() {
        super.tickUpdate()
        for (key in drawnStrings) {
            FontManager.getString(key)
        }
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
        if (lineH > 0) {
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
                val text = getTimeString(time, timeStep)
                drawSimpleTextCharByChar(
                    x, y0, 2, text, fontColor, backgroundColor, AxisAlignment.CENTER
                )
            }
        }

    }

    fun drawLine(time: Double, y0: Int, y1: Int, color: Int) {
        if (!time.isFinite()) return
        // if there are sub-pixels, we could use those...
        val x = getXAt(time).toFloat()
        val xFloor = floor(x)
        val x0 = xFloor.toInt()
        val alpha1 = x - xFloor
        val alpha0 = 1f - alpha1
        // simple interpolation
        // it looks way better than without (it looks a little lagging without)
        drawRect(x0 + 0, y0 + 2, 1, y1 - y0 - 4, color.mulAlpha(alpha0))
        drawRect(x0 + 1, y0 + 2, 1, y1 - y0 - 4, color.mulAlpha(alpha1))
    }

    fun getTimeStep(time: Double): Double {
        return timeFractions.minByOrNull { abs(it - time) }!!.toDouble()
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            isCross(x, y) -> super.onMouseClicked(x, y, button, long)
            button.isLeft -> jumpToX(x)
            else -> {
                val options = listOf(
                    MenuOption(
                        NameDesc(
                            "Set End Here",
                            "Sets the end of the project to here",
                            "ui.timePanel.setEndHere"
                        )
                    ) {
                        project?.targetDuration = getTimeAt(x)
                    },
                    MenuOption(NameDesc("Jump to Start", "Set the time to 0", "ui.timePanel.jumpToStart")) {
                        jumpToT(0.0)
                    },
                    MenuOption(
                        NameDesc(
                            "Jump to End",
                            "Set the time to the end of the project",
                            "ui.timePanel.jumpToEnd"
                        )
                    ) {
                        jumpToT(targetDuration)
                    }
                )
                openMenu(windowStack, options)
            }
        }
    }

    fun jumpToX(x: Float) = jumpToT(getTimeAt(x))
    fun jumpToT(t: Double) {
        RemsStudio.largeChange("Timeline jump to ${t.formatTime()}/${(fract(t) * targetFPS).toInt()}") {
            editorTime = t
        }
        updateAudio()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = getTimeAt(x)
        if (0 in Input.mouseKeysDown) {
            if ((Input.isShiftDown || Input.isControlDown) && isPaused) {
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

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {

        val scale = pow(1.05f, dx)
        // set the center to the cursor
        // works great :D
        val normalizedX = (x - w / 2) / (w / 2)
        centralTime += normalizedX * dtHalfLength * (1f - scale)
        dtHalfLength *= scale
        centralTime += dtHalfLength * 20f * dy / w

        clampTime()

    }

    override val className: String = "TimelinePanel"

}