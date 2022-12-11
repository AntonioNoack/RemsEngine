package me.anno.ui.anim

import me.anno.animation.Interpolation
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.mix
import me.anno.ui.Panel

class MoveAnimation(
    eventType: EventType,
    inInterpolation: Interpolation,
    outInterpolation: Interpolation,
    var srcX: Float, var srcY: Float,
    var dstX: Float, var dstY: Float
) : UIAnimation(eventType, inInterpolation, outInterpolation) {

    constructor(
        eventType: EventType, inInterpolation: Interpolation,
        srcX: Float, srcY: Float, dstX: Float, dstY: Float
    ) : this(eventType, inInterpolation, inInterpolation.getReversedType(), srcX, srcY, dstX, dstY)

    constructor() : this(EventType.HOVER, Interpolation.LINEAR_BOUNDED, 0f, 0f, 0f, 0f)

    override fun apply(parent: AnimContainer, child: Panel, strength: Float) {
        // todo how can we stabilize the title
        // todo it would be helpful if we actually could scale the element
        val padding = parent.padding
        child.x += (padding.width * mix(srcX, dstX, strength)).toInt() - padding.left
        child.y += (padding.height * mix(srcY, dstY, strength)).toInt() - padding.top
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "srcX" -> srcX = value
            "srcY" -> srcY = value
            "dstX" -> dstX = value
            "dstY" -> dstY = value
            else -> super.readFloat(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("srcX", srcX)
        writer.writeFloat("srcY", srcY)
        writer.writeFloat("dstX", dstX)
        writer.writeFloat("dstY", dstY)
    }

    override val className get() = "UIMoveAnimation"

}
