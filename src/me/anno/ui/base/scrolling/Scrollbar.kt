package me.anno.ui.base.scrolling

import me.anno.Time.uiDeltaTime
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.input.Input
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.canvas.Canvas
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha

open class Scrollbar(style: Style) : Panel(style.getChild("scrollbar")) {

    constructor(parent: Panel, style: Style) : this(style) {
        this.parent = parent
    }

    val activeAlpha = 0.2f

    var minSize = 5

    val scrollColor = -1
    val scrollColorAlpha = 0.3f
    val scrollBackground = -1

    @NotSerializedProperty
    var alpha = 0f

    fun updateAlpha() {
        alpha = mix(
            alpha, if (isHovered) if (Input.isLeftDown) 1f else 0.8f else 0f,
            dtTo01(10f * uiDeltaTime.toFloat())
        )
    }

    override fun draw(canvas: Canvas) {
        val color = mixARGB(background.color, scrollBackground, activeAlpha * alpha)
        canvas.drawRect(x, y, width, height, color.withAlpha(255))
    }

    override fun clone(): Scrollbar {
        val clone = Scrollbar(style)
        copyInto(clone)
        return clone
    }
}