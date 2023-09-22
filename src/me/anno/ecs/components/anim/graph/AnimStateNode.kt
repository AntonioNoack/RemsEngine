package me.anno.ecs.components.anim.graph

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.states.StateNode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.posMod

class AnimStateNode : StateNode("AnimState", inputs, outputs) {

    companion object {
        const val SOURCE = 1
        const val SPEED = 2
        const val START = 3
        const val END = 4
        const val FADE = 5
        const val LOOP = 6
        private val inputs = listOf(
            "File", "Source",
            "Float", "Speed",
            "Float", "Start",
            "Float", "End",
            "Float", "Fade",
            "Bool", "Loop"
        )
        private val outputs = listOf(
            "Float", "Progress"
        )

        fun register() {
            NodeLibrary.init()
            registerCustomClass(AnimStateNode())
        }
    }

    var progress = 0f

    override fun onEnterState(oldState: StateNode?) {
        progress = getInput(START) as Float // start time
        setOutput(1, progress)
    }

    fun getDuration(): Float {
        val source = getInput(SOURCE) as FileReference
        val anim = AnimationCache[source, false] ?: return 10f
        return anim.duration
    }

    override fun update(): StateNode {
        val speed = getInput(SPEED) as Float
        progress += speed * Time.deltaTime.toFloat()
        setOutput(1, progress)
        var end = getInput(END) as Float
        if (end == 0f) end = getDuration()
        val fade = getInput(FADE) as Float
        if (progress > end - fade) return super.update()
        return this // continuing this animation
    }

    fun updateRenderer(previous: AnimStateNode?, target: AnimRenderer) {
        val targetSize = if (previous == null) 1 else 2
        if (target.animations.size != targetSize) {
            target.animations = if (previous == null) {
                listOf(AnimationState())
            } else {
                listOf(
                    AnimationState(),
                    AnimationState()
                )
            }
        }
        val animations = target.animations
        val loop = getInput(LOOP) as Boolean
        val start = getInput(START) as Float
        var end = getInput(END) as Float
        val duration = getDuration()
        if (end <= 0f) end = duration
        val progress = if (loop) posMod(progress, duration) else progress
        val fade = getInput(FADE) as Float
        val fadeIn = (progress - start) / fade
        val weight = clamp(min(fadeIn, (end - progress) / fade))
        val state = if (previous != null) {
            val state = animations[0]
            previous.fillState(1f - weight, state)
            state.progress += state.speed * Time.deltaTime.toFloat()
            animations[1]
        } else animations[0]
        fillState(weight, state)
        state.speed = getInput(SPEED) as Float
        state.progress = progress
        state.repeat = if (loop) LoopingState.PLAY_LOOP else LoopingState.PLAY_LOOP
    }

    fun fillState(weight: Float, target: AnimationState) {
        target.weight = weight
        target.source = getInput(SOURCE) as FileReference
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AnimStateNode
        dst.progress = progress
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("progress", progress)
    }

    override fun readFloat(name: String, value: Float) {
        if (name == "progress") progress = value
        else super.readFloat(name, value)
    }

    override val className: String get() = "AnimStateNode"
}