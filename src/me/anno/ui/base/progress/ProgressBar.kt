package me.anno.ui.base.progress

import me.anno.Engine
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mixARGB
import me.anno.utils.Color.black

open class ProgressBar(val unit: String, var total: Double) {

    var progress = 0.0

    private var finishTime = 0L
    private var cancelTime = 0L
    private var lastUpdate = Engine.gameTime

    private var lastDrawnUpdate = 0.0
    private var lastDraw = Engine.gameTime

    val isCancelled get() = cancelTime != 0L

    fun add(deltaProgress: Int) = add(deltaProgress.toDouble())
    fun add(deltaProgress: Long) = add(deltaProgress.toDouble())
    fun add(deltaProgress: Float) = add(deltaProgress.toDouble())

    fun add(deltaProgress: Double) {
        progress += deltaProgress
        lastUpdate = Engine.gameTime
        checkFinish()
    }

    fun update(newProgress: Double) {
        progress = newProgress
        lastUpdate = Engine.gameTime
        checkFinish()
    }

    fun checkFinish() {
        if (progress >= total) {
            finishTime = Engine.gameTime
            progress = total
        }
    }

    fun finish() {
        progress = total
        if (finishTime == 0L) finishTime = Engine.gameTime
    }

    fun cancel() {
        if (cancelTime == 0L) cancelTime = Engine.gameTime
    }

    fun canBeRemoved(time: Long): Boolean {
        return time - lastUpdate > timeout ||
                (finishTime != 0L && time - finishTime > endShowDuration) ||
                (cancelTime != 0L && time - cancelTime > endShowDuration)
    }

    /**
     * how quickly it will converge to the actual value (for smoothing);
     * high value = less smoothing
     * */
    var updateSpeed = 1.0

    open val backgroundColor = black
    open val color
        get() = when {
            finishTime > 0L -> 0x77ff77 or black
            cancelTime > 0L -> 0xff7777 or black
            else -> 0x999999 or black
        }

    fun draw(x: Int, y: Int, w: Int, h: Int, time: Long) {
        val dt = Maths.dtTo01((time - lastDraw) * 1e-9 * updateSpeed)
        lastDraw = time
        val percentage = progress / total
        lastDrawnUpdate = mix(lastDrawnUpdate, percentage, dt)
        // todo show num/total unit
        // todo if unit = "Bytes", format file size
        // todo animation with shifted stripes?
        val wx = 5 + percentage * (w - 5)
        val color = color
        val backgroundColor = backgroundColor
        val wxi = wx.toInt()
        if (w - wxi - 1 > 0) drawRect(x + 1 + wxi, y, w - wxi - 1, h, backgroundColor)
        drawRect(x, y, wxi, h, color)
        drawRect(x + wxi, y, 1, h, mixARGB(backgroundColor, color, fract(wx).toFloat()))
    }

    companion object {
        @JvmStatic
        private var endShowDuration = 3_000_000_000

        @JvmStatic
        private var timeout = 10_000_000_000
    }

}