package me.anno.ui.base.progress

import me.anno.Engine
import me.anno.gpu.GFX.clip
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mixARGB
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.Color.black
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Booleans.toInt

open class ProgressBar(
    val name: String,
    val unit: String,
    var total: Double
) {

    var progress = 0.0
        set(value) {
            if (field != value) {
                lastUpdate = Engine.nanoTime
                field = if (finishTime == 0L && value >= total) {
                    finishTime = lastUpdate
                    total
                } else value
            }
        }

    private var finishTime = 0L
    private var lastUpdate = Engine.nanoTime

    private var lastDrawnUpdate = 0.0
    private var lastDraw = Engine.nanoTime

    var isCancelled = false

    fun finish(done: Boolean = !isCancelled) {
        if (done) progress = total
        if (finishTime == 0L) finishTime = Engine.nanoTime
    }

    /**
     * marks the progress as cancelled;
     * if you skip work by detecting isCancelled, call finish anyway, so the progress bar can be removed from the UI!
     * */
    fun cancel(isFinished: Boolean) {
        isCancelled = true
        if (isFinished) finish()
    }

    fun canBeRemoved(time: Long): Boolean {
        return time - lastUpdate > timeout || (finishTime != 0L && time - finishTime > endShowDuration)
    }

    /**
     * how quickly it will converge to the actual value (for smoothing);
     * high value = less smoothing
     * */
    var updateSpeed = 1.0

    open val backgroundColor = black
    open val color
        get() = when {
            isCancelled -> 0xff7777 or black
            finishTime > 0L -> 0x77ff77 or black
            else -> 0x999999 or black
        }

    open fun formatText(): String {
        val progress = progress
        val total = total
        return if (
            unit == "Bytes" && progress.isFinite() && total.isFinite() &&
            progress >= 0.0 && total >= 0.0
        ) {
            "${progress.toLong().formatFileSize()} / ${total.toLong().formatFileSize()}"
        } else {
            "$progress / $total $unit"
        }
    }

    fun draw(x: Int, y: Int, w: Int, h: Int, time: Long) {
        val dt = Maths.dtTo01((time - lastDraw) * 1e-9 * updateSpeed)
        lastDraw = time
        val percentage = progress / total
        lastDrawnUpdate = mix(lastDrawnUpdate, percentage, dt)

        // todo animation with shifted stripes?
        // todo if total is NaN, draw indeterminate mode like Qt
        val wx = (5 + percentage * (w - 5)).toFloat()
        val leftColor = color
        val rightColor = backgroundColor
        val wxi = wx.toInt()
        val wxf = fract(wx)
        val mixedColor = mixARGB(rightColor, leftColor, wxf)
        drawRect(x, y, wxi, h, leftColor)
        drawRect(x + wxi, y, 1, h, mixedColor)
        drawRect(x + 1 + wxi, y, w - wxi - 1, h, rightColor)
        // show num/total unit
        val mid = wxi + (wxf >= 0.5f).toInt()
        val text = formatText()
        val xt = x + w.shr(1)
        val yt = y + (h - monospaceFont.sizeInt).shr(1)
        clip(x, y, mid, h) {
            drawSimpleTextCharByChar(
                xt, yt, 1, text,
                rightColor, leftColor, AxisAlignment.CENTER, AxisAlignment.MIN
            )
        }
        clip(x + mid, y, w - mid, h) {
            drawSimpleTextCharByChar(
                xt, yt, 1, text,
                leftColor, rightColor, AxisAlignment.CENTER, AxisAlignment.MIN
            )
        }
    }

    var timeout = Long.MAX_VALUE
    var endShowDuration = 3_000_000_000

}