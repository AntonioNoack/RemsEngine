package me.anno.ui.impl.timeline

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.maths.clamp
import me.anno.maths.pow
import me.anno.ui.base.Panel
import me.anno.ui.style.Style

class TimelineBody(style: Style): Panel(style.getChild("deep")){

    var isColor = false

    var dtHalfLength = 1f
    var centralTime = 0f

    var dotSize = style.getSize("dotSize", 8)

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
        val size = if(GFX.selectedProperty == null) 0 else 100
        minW = size
        minH = size
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        val property = GFX.selectedProperty ?: return

        val minFrame = centralTime - dtHalfLength
        val maxFrame = centralTime + dtHalfLength

        val count = (maxFrame - minFrame)
        val avgSteps = 15
        // val stepSize = pow(10f, (log10(count / avgSteps) + avgSteps).roundToInt().toFloat())

        val type = property.type
        val halfSize = dotSize/2
        property.keyframes.forEach {
            val time = it.time
            val value = it.value
            val x = getXAt(time)
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

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float) {
        val delta = dx+dy
        val scale = pow(1.01f, delta)
        if(GFX.isControlDown){
            // zoom
            dtHalfLength = clamp(dtHalfLength * scale, 0.001f, 1e3f)
        } else {
            centralTime += dtHalfLength * 2f * delta / w
        }
        println("timeline wheel $centralTime +/- $dtHalfLength")
    }

}