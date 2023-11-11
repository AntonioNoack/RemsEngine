package me.anno.ecs.components.anim

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.studio.Inspectable

class AnimationState(
    @Type("Animation/Reference")
    var source: FileReference,
    @Range(0.0, 1.0)
    var weight: Float,
    @Range(0.0, Double.POSITIVE_INFINITY)
    var progress: Float,
    var speed: Float,
    var repeat: LoopingState
) : Saveable(), Inspectable {

    constructor() : this(InvalidRef, 0f, 0f, 1f, LoopingState.PLAY_LOOP)

    override fun readFile(name: String, value: FileReference) {
        if (name == "source") source = value
        else super.readFile(name, value)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "weight" -> weight = value
            "progress" -> progress = value
            "speed" -> speed = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        if (name == "repeat") repeat = LoopingState.getState(value)
        else super.readInt(name, value)
    }

    private var lastDt = 0f
    private var lastTime = 0L
    fun update(ar: AnimMeshComponent, dt: Float, async: Boolean) {
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
                    ar.onAnimFinished(this)
                }
            }
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

    fun clone(): AnimationState {
        return AnimationState(source, weight, progress, speed, repeat)
    }

    override val className: String get() = "AnimationState"
}