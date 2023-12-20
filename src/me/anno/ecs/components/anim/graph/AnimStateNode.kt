package me.anno.ecs.components.anim.graph

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.types.states.StateNode
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.utils.structures.lists.Lists.firstOrNull2

/**
 * node for AnimController to represent when another animation shall be started
 * */
class AnimStateNode : StateNode("AnimState", inputs, outputs) {

    companion object {
        val SOURCE = 1
        val SPEED = 2
        val FADE = 3
        val LOOP = 4
        val FORCE_ONCE = 5
        private val inputs = listOf(
            "File", "Source",
            "Float", "Speed",
            "Float", "Fade",
            "Bool", "Loop",
            "Bool", "Force Once"
        )
        private val outputs = listOf(
            "Float", "Progress",
            "Float", "Relative Progress"
        )
    }

    init {
        setInput(SOURCE, InvalidRef)
        setInput(SPEED, 1f)
        setInput(FADE, 5f)
        setInput(LOOP, true)
        setInput(FORCE_ONCE, false)
    }

    private var progress = 0f
    override fun onEnterState(oldState: StateNode?) {
        progress = 0f
        lastTime = Time.gameTimeN
        setOutput(1, 0f)
        setOutput(2, 0f)
    }

    fun getDuration(): Float {
        val source = getInput(SOURCE) as FileReference
        val anim = AnimationCache[source, false] ?: return 10f
        return anim.duration
    }

    override fun update(): StateNode {
        setOutput(1, progress)
        setOutput(2, progress / getDuration())
        if (canReturnAnyTime() || hasReachedEnd()) return super.update()
        return this // continuing this animation
    }

    private fun canReturnAnyTime(): Boolean {
        return getInput(FORCE_ONCE) != true
    }

    private fun hasReachedEnd(): Boolean {
        val fade = getInput(FADE) as Float
        val goodEndTime = getDuration() - 3f / fade
        return progress > goodEndTime
    }

    private var lastTime = 0L
    fun updateRenderer(target: AnimMeshComponent) {

        val time = Time.gameTimeN
        if (time == lastTime) return

        val source = getInput(SOURCE) as FileReference
        val speed = getInput(SPEED) as Float
        val loop = getInput(LOOP) as Boolean
        val dt = (time - lastTime) * speed / 1e9f
        lastTime = time

        // if we need to, add this animation to the existing animation states
        var selfAnimation = target.animations.firstOrNull2 { it.source == source && it.speed == speed }
        if (selfAnimation == null) {
            selfAnimation = AnimationState(
                source, 0f, progress,
                speed, if (loop) LoopingState.PLAY_LOOP else LoopingState.PLAY_ONCE
            )
            target.animations += selfAnimation
        }

        val animations = target.animations
        val fade = getInput(FADE) as Float
        val fade01 = dtTo01(fade * dt)

        // fade animations in/out
        var needsRemoval = false
        val minWeight = 1e-3f
        for (i in animations.indices) {
            val anim = animations[i]
            val targetWeight = if (anim === selfAnimation) 1f else 0f
            anim.weight = mix(anim.weight, targetWeight, fade01)
            anim.update(null, dt, true)
            if (targetWeight == 0f && anim.weight < minWeight) needsRemoval = true
        }

        progress += dt
        selfAnimation.progress = selfAnimation.repeat[progress, getDuration()]

        if (needsRemoval) {
            target.animations = animations
                .filter { !(it.weight < minWeight && it !== selfAnimation) }
        }
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