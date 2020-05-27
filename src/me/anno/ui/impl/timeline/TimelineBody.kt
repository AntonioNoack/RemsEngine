package me.anno.ui.impl.timeline

import me.anno.config.DefaultStyle.black
import me.anno.config.DefaultStyle.fontGray
import me.anno.gpu.GFX
import me.anno.utils.clamp
import me.anno.utils.pow
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import kotlin.math.*

class TimelineBody(style: Style): Panel(style.getChild("deep")){

    var isColor = false

    var dtHalfLength = 30f
    var centralTime = dtHalfLength

    var dotSize = style.getSize("dotSize", 8)
    val tinyFontSize = style.getSize("tinyTextSize", 10)
    val fontColor = style.getColor("textColor", fontGray)

    init {
        backgroundColor = black
        minW = 100
        minH = 100
    }

    fun getTimeAt(mx: Float): Float {
        val normed01 = (mx-x)/w
        val normed = normed01 * 2 - 1
        return centralTime + dtHalfLength * normed
    }

    fun getXAt(time: Float): Float {
        val normed11 = (time-centralTime)/dtHalfLength
        val normed01 = normed11 * 0.5f + 0.5f
        return x + w * normed01
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val size = 100
        minW = size
        minH = size
    }

    fun getTensString(time: Int) = if(time < 10) "0$time" else "$time"

    fun getTimeString(time: Float, step: Float): String {
        if(time < 0) return "-${getTimeString(-time, step)}"
        val s = time.toInt()
        val m = s / 60
        val h = m / 60
        val subTime = ((time % 1) * GFX.targetFPS).roundToInt()
        return if(h < 1) "${getTensString(m % 60)}:${getTensString(s % 60)}${if(step < 1f) "/${getTensString(subTime)}" else ""}"
        else "${getTensString(h)}:${getTensString(m % 60)}:${getTensString(s % 60)}${if(step < 1f) "/${getTensString(subTime)}" else ""}"
    }

    fun getBeautifulFraction(time: Float): Float {
        return timeFractions.minBy { abs(it - time) }!!
    }

    val timeFractions = listOf(
        0.2f,
        1f, 2f, 5f, 10f, 20f, 30f, 60f,
        120f, 300f, 600f, 1200f, 1800f, 3600f,
        3600f * 1.5f, 3600f * 2f, 3600f * 5f,
        3600f * 6f, 3600f * 12f, 3600f * 24f
    )

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val minFrame = centralTime - dtHalfLength
        val maxFrame = centralTime + dtHalfLength

        val deltaFrame = 2 * dtHalfLength
        val timeStep = getBeautifulFraction(deltaFrame * 0.2f)

        val minStepIndex = (minFrame / timeStep).toInt() - 1
        val maxStepIndex = (maxFrame / timeStep).toInt() + 1

        // todo major lines?
        for(stepIndex in maxStepIndex downTo minStepIndex){
            val time = stepIndex * timeStep
            val x = getXAt(time).roundToInt()
            GFX.drawRect(x, y0, 1, y1-y0, fontColor and 0x7fffffff)
            val text = getTimeString(time, timeStep)
            val w = GFX.getTextSize(tinyFontSize, text).first
            GFX.drawText(x - w/2, y0, tinyFontSize, text, fontColor, backgroundColor)
        }

        val property = GFX.selectedProperty ?: return

        val type = property.type
        val halfSize = dotSize/2
        property.keyframes.forEach {
            val keyTime = it.time
            val keyValue = it.value
            val x = getXAt(keyTime)
            GFX.drawTexture(x.toInt()-halfSize, y+h/2-halfSize, dotSize, dotSize, GFX.colorShowTexture, black or 0xff0000)
            // GFX.drawRect(x.toInt()-1, y+h/2, 2,2, black or 0xff0000)
        }

        // todo draw all data points <3
        // todo controls:
        // todo mouse wheel -> left/right, +control = zoom
        // todo double click = add point
        // todo select points
        // todo delete selected points
        // todo copy paste timeline pieces?
        // todo select multiple points by area -> via time?

    }

    fun jumpToX(x: Float){
        GFX.editorTime = getTimeAt(x)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        GFX.editorHoverTime = x
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        jumpToX(x)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val delta = if(GFX.isShiftDown) (dx-dy)/5 else (dx-dy)
        val scale = pow(1.05f, delta)
        if(GFX.isControlDown){ // zoom
            // set the center to the cursor
            // works great :D
            val normalizedX = (x-w/2)/(w/2)
            centralTime += normalizedX * dtHalfLength * (1f - scale)
            dtHalfLength *= scale
        } else { // move
            centralTime += dtHalfLength * 20f * delta / w
        }
        dtHalfLength = clamp(dtHalfLength, 2f/GFX.targetFPS, timeFractions.last())
        centralTime = max(centralTime, dtHalfLength)
    }

}