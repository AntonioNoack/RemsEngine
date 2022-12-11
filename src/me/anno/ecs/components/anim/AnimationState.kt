package me.anno.ecs.components.anim

import me.anno.animation.LoopingState
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.cache.AnimationCache
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

class AnimationState(
    var source: FileReference,
    @Range(0.0, 1.0)
    var weight: Float,
    @Range(0.0, Double.POSITIVE_INFINITY)
    var progress: Float,
    var speed: Float,
    var repeat: LoopingState
) : Saveable() {

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

    fun update(ar: AnimRenderer, dt: Float, async: Boolean) {
        val instance = AnimationCache[source, async]
        if (instance != null) {
            progress += speed * dt
            val duration = instance.duration
            if (progress < 0f || progress >= duration) {
                ar.onAnimFinished(this)
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

    override val className get() = "AnimationState"
}