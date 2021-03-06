package me.anno.ecs.components.anim.graph

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.Node
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.states.StateNode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min

class AnimStateNode : StateNode("AnimState", inputs, outputs) {

    companion object {
        private const val SOURCE = 1
        private const val SPEED = 2
        private const val START = 3
        private const val END = 4
        private const val FADE = 5
        private const val LOOP = 6
        private val inputs = listOf(
            "Flow", "Input",
            "File", "Source",
            "Float", "Speed",
            "Float", "Start",
            "Float", "End",
            "Float", "Fade",
            "Bool", "Loop"
        )
        private val outputs = listOf(
            "Flow", "Next",
            "Float", "Progress"
        )
        fun register() {
            NodeLibrary.init()
            registerCustomClass(AnimStateNode::class)
        }
    }

    var progress = 0f

    override fun executeAction(graph: FlowGraph) {
        progress = getInput(graph, START) as Float // start time
        setOutput(progress, 1)
        // throws
        super.executeAction(graph)
    }

    fun getDuration(graph: FlowGraph): Float {
        val source = getInput(graph, SOURCE) as FileReference
        val anim = AnimationCache[source, false] ?: return 10f
        return anim.duration
    }

    override fun update(graph: FlowGraph): StateNode {
        val speed = getInput(graph, SPEED) as Float
        progress += speed * Engine.deltaTime
        setOutput(progress, 1)
        var end = getInput(graph, END) as Float
        if (end == 0f) end = getDuration(graph)
        val fade = getInput(graph, FADE) as Float
        if (progress > end - fade)
            return super.update(graph)
        return this // continuing this animation
    }

    fun posMod(x: Float, y: Float): Float {
        return ((x % y) + y) % y
    }

    fun updateRenderer(graph: FlowGraph, previous: AnimStateNode?, target: AnimRenderer) {
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
        val loop = getInput(graph, LOOP) as Boolean
        val start = getInput(graph, START) as Float
        var end = getInput(graph, END) as Float
        val duration = getDuration(graph)
        if (end <= 0f) end = duration
        val progress = if (loop) posMod(progress, duration) else progress
        val fade = getInput(graph, FADE) as Float
        val fadeIn = (progress - start) / fade
        val weight = clamp(min(fadeIn, (end - progress) / fade))
        val state = if (previous != null) {
            val state = animations[0]
            previous.fillState(clamp(fadeIn), state)
            state.progress += state.speed * Engine.deltaTime
            animations[1]
        } else animations[0]
        fillState(weight, state)
        state.speed = getInput(graph, 1) as Float
        state.progress = progress
        state.repeat = if (loop) LoopingState.PLAY_LOOP else LoopingState.PLAY_LOOP
    }

    fun fillState(weight: Float, target: AnimationState) {
        target.weight = weight
    }

    override fun clone(): Node {
        val clone = AnimStateNode()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as AnimStateNode
        clone.progress = progress
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("progress", progress)
    }

    override fun readFloat(name: String, value: Float) {
        if (name == "progress") progress = value
        else super.readFloat(name, value)
    }

    override val className = "AnimStateNode"

}