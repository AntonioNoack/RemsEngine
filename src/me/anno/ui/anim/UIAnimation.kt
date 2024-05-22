package me.anno.ui.anim

import me.anno.animation.Interpolation
import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.ui.Panel

// todo (interactable) springs as well :) -> make the UI joyful
// todo layouts could have springs as well, so the elements move to their target position with lerp(pos,target,dt*10)

abstract class UIAnimation(
    var eventType: EventType,
    var inInterpolation: Interpolation,
    var outInterpolation: Interpolation = inInterpolation.getReversedType()
) : Saveable() {

    abstract fun apply(parent: AnimContainer, child: Panel, strength: Float)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("eventType", eventType)
        writer.writeEnum("in", inInterpolation)
        writer.writeEnum("out", outInterpolation)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "in" -> {
                if (value !is Int) return
                inInterpolation = Interpolation.entries.firstOrNull { it.id == value } ?: inInterpolation
            }
            "out" -> {
                if (value !is Int) return
                inInterpolation = Interpolation.entries.firstOrNull { it.id == value } ?: outInterpolation
            }
            "eventType" -> {
                if (value !is Int) return
                eventType = EventType.entries.firstOrNull { it.id == value } ?: eventType
            }
            else -> super.setProperty(name, value)
        }
    }

    fun clone(): UIAnimation = JsonStringReader.clone(this)
}