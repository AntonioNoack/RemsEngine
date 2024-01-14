package me.anno.ui.anim

import me.anno.animation.Interpolation
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.engine.EngineBase
import me.anno.ui.Panel

// todo (interactable) springs as well :) -> make the UI joyful
// todo layouts could have springs as well, so the elements move to their target position with lerp(pos,target,dt*10)

abstract class UIAnimation(
    var eventType: EventType,
    var inInterpolation: Interpolation,
    var outInterpolation: Interpolation = inInterpolation.getReversedType()
) : Saveable() {

    abstract fun apply(parent: AnimContainer, child: Panel, strength: Float)

    override fun readInt(name: String, value: Int) {
        when (name) {
            "in" -> inInterpolation = Interpolation.entries.firstOrNull { it.id == value } ?: inInterpolation
            "out" -> inInterpolation = Interpolation.entries.firstOrNull { it.id == value } ?: outInterpolation
            "eventType" -> eventType = EventType.entries.firstOrNull { it.id == value } ?: eventType
            else -> super.readInt(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("eventType", eventType)
        writer.writeEnum("in", inInterpolation)
        writer.writeEnum("out", outInterpolation)
    }

    fun clone() = JsonStringReader.readFirst<UIAnimation>(
        JsonStringWriter.toText(this, EngineBase.workspace), EngineBase.workspace
    )

    override val className: String get() = "UIAnimation"
}