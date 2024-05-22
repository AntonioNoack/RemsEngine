package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.engine.inspector.Inspectable
import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

/**
 * represents how animations shall be played;
 * this will be used in Lists, so you can smoothly lerp animations
 * */
class AnimationState(
    @Type("Animation/Reference") // todo this isn't properly displayed yet... why?? -> because Saveable, not PrefabSaveable
    var source: FileReference,
    @Range(0.0, 1.0)
    var weight: Float,
    @Range(0.0, Double.POSITIVE_INFINITY)
    var progress: Float,
    var speed: Float,
    var repeat: LoopingState
) : Saveable(), Inspectable {

    constructor() : this(InvalidRef, 0f, 0f, 1f, LoopingState.PLAY_LOOP)

    private var lastDt = 0f
    private var lastTime = 0L
    fun update(ar: AnimMeshComponent?, dt: Float, async: Boolean) {
        if (lastDt == dt && lastTime == Time.gameTimeN) {
            return // duplicate call
        } else {
            lastDt = dt
            lastTime = Time.gameTimeN
            val instance = AnimationCache[source, async]
            if (instance != null) {
                progress += speed * dt
                val duration = instance.duration
                if (progress < 0f || progress >= duration) {
                    ar?.onAnimFinished(this)
                }
            }
        }
    }


    fun set(time: Float, async: Boolean) {
        val instance = AnimationCache[source, async]
        if (instance != null) {
            progress = repeat[time, instance.duration]
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("source", source)
        writer.writeFloat("weight", weight, true)
        writer.writeFloat("progress", progress, true)
        writer.writeFloat("speed", speed, true)
        writer.writeEnum("repeat", repeat)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "source" -> source = value as? FileReference ?: InvalidRef
            "weight" -> weight = value as? Float ?: return
            "progress" -> progress = value as? Float ?: return
            "speed" -> speed = value as? Float ?: return
            "repeat" -> repeat = LoopingState.getState(value as? Int ?: return)
            else -> super.setProperty(name, value)
        }
    }

    fun clone(): AnimationState {
        return AnimationState(source, weight, progress, speed, repeat)
    }
}