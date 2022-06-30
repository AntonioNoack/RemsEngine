package me.anno.ui.base.progress

import me.anno.Engine
import me.anno.config.DefaultStyle.black
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.maths.Maths
import me.anno.maths.Maths.mix

class ProgressBar(val unit: String, var total: Double) {

    private var progress = 0.0

    private var finishTime = 0L
    private var cancelTime = 0L
    private var lastUpdate = Engine.gameTime

    private var lastDrawnUpdate = 0.0
    private var lastDraw = Engine.gameTime

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
        finishTime = Engine.gameTime
    }

    fun cancel() {
        cancelTime = Engine.gameTime
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

    fun draw(x: Int, y: Int, w: Int, h: Int, time: Long) {
        val dt = Maths.dtTo01((time - lastDraw) * 1e-9 * updateSpeed)
        lastDraw = time
        val percentage = progress / total
        lastDrawnUpdate = mix(lastDrawnUpdate, percentage, dt)
        // todo show num/total unit
        // todo if unit = "Bytes", format file size
        // todo animation with shifted stripes?
        // todo blending the last stripe for subpixel-accuracy
        // todo smoothing?
        drawRect(x, y, w, h, black)
        drawRect(
            x, y, 5 + (percentage * (w - 5)).toInt(), h, when {
                finishTime > 0L -> 0x77ff77 or black
                cancelTime > 0L -> 0xff7777 or black
                else -> 0x999999 or black
            }
        )
    }

    companion object {
        private var endShowDuration = 3_000_000_000
        private var timeout = 10_000_000_000
    }

}