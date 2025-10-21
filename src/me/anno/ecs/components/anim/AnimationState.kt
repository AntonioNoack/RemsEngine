package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.engine.inspector.Inspectable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToBool.getBool
import me.anno.utils.types.AnyToFloat.getFloat

/**
 * represents how animations shall be played;
 * this will be used in Lists, so you can smoothly lerp animations
 * */
class AnimationState(
    @property:Type("Animation/Reference")
    var source: FileReference,
    @property:Range(0.0, 1.0)
    var weight: Float,
    @property:Range(0.0, Double.POSITIVE_INFINITY)
    var progress: Float,
    var speed: Float,
    var repeat: LoopingState,
    var isContinuous: Boolean
) : Saveable(), Inspectable {

    constructor() : this(
        InvalidRef, 0f, 0f, 1f,
        LoopingState.PLAY_LOOP, false
    )

    constructor(source: FileReference, weight: Float = 1f) : this(
        source, weight, 0f, 1f,
        LoopingState.PLAY_LOOP, false
    )

    private var lastTime = 0L
    fun update(ar: AnimMeshComponent?, dt: Float) {
        val time = Time.gameTimeN
        if (lastTime != time) {
            lastTime = time
            progress += speed * dt
            val instance = AnimationCache[source] ?: return
            if (progress < 0f || progress >= instance.duration) {
                ar?.onAnimFinished(this)
            }
        }
    }

    fun setTime(time: Float) {
        val instance = AnimationCache[source] ?: return
        progress = repeat[time, instance.duration]
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("source", source)
        writer.writeFloat("weight", weight, true)
        writer.writeFloat("progress", progress, true)
        writer.writeFloat("speed", speed, true)
        writer.writeEnum("repeat", repeat)
        writer.writeBoolean("isContinuous", isContinuous)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "source" -> source = value as? FileReference ?: InvalidRef
            "weight" -> weight = getFloat(value)
            "progress" -> progress = getFloat(value)
            "speed" -> speed = getFloat(value)
            "repeat" -> repeat = LoopingState.getById(value as? Int ?: return)
            "isContinuous" -> isContinuous = getBool(value)
            else -> super.setProperty(name, value)
        }
    }

    override fun clone(): AnimationState {
        return AnimationState(source, weight, progress, speed, repeat, isContinuous)
    }
}