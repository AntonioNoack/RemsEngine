package me.anno.ui.anim

import me.anno.animation.Interpolation
import me.anno.io.base.BaseWriter
import me.anno.ui.Panel

class ScaleAnimation(
    eventType: EventType,
    inInterpolation: Interpolation,
    outInterpolation: Interpolation,
    var centerX: Float = 0.5f, var centerY: Float = 0.5f
) : UIAnimation(eventType, inInterpolation, outInterpolation) {

    constructor(
        eventType: EventType, inInterpolation: Interpolation,
        cx: Float = 0.5f, cy: Float = 0.5f
    ) : this(eventType, inInterpolation, inInterpolation.getReversedType(), cx, cy)

    constructor() : this(EventType.HOVER, Interpolation.LINEAR_BOUNDED, 0f, 0f)

    override fun apply(parent: AnimContainer, child: Panel, strength: Float) {
        // todo how can we stabilize the title?
        // todo it would be helpful if we actually could scale the element
        val padding = parent.padding
        val inv = 1f - strength
        child.x += (padding.width * centerX * inv).toInt() - padding.left
        child.width += (padding.width * strength).toInt()
        child.y += (padding.height * centerY * inv).toInt() - padding.top
        child.height += (padding.height * strength).toInt()
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "centerX" -> centerX = value
            "centerY" -> centerY = value
            else -> super.readFloat(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("centerX", centerX)
        writer.writeFloat("centerY", centerY)
    }

    override val className: String get() = "UIScaleAnimation"

}