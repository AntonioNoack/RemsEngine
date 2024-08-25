package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.query.GPUClockNanos
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.NodeOutput
import me.anno.utils.structures.lists.Lists.any2

abstract class RenderViewNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    lateinit var renderView: RenderView
    lateinit var pipeline: Pipeline

    var framebuffer: IFramebuffer? = null
    val timer = GPUClockNanos()

    open fun invalidate() {
    }

    override fun destroy() {
        super.destroy()
        timer.destroy()
        invalidate()
    }

    fun needsRendering(stage: PipelineStage): Boolean {
        val stage1 = pipeline.stages.getOrNull(stage.id)
        return stage1 != null && !stage1.isEmpty()
    }

    companion object {
        fun isOutputUsed(output: NodeOutput, maxDepth: Int = 10): Boolean {
            // if is RenderSceneDeferredNode, its output needs to be used, too
            return output.others.any2 { otherCon ->
                // is used?
                when (val intoNode = otherCon.node) {
                    is RenderDeferredNode -> {
                        val di = RenderDeferredNode.firstOutputIndex - RenderDeferredNode.firstInputIndex
                        val oi = intoNode.inputs.indexOf(otherCon) + di
                        if (oi > 0 && maxDepth > 0) {
                            isOutputUsed(intoNode.outputs[oi], maxDepth - 1)
                        } else true
                    }
                    else -> true
                }
            }
        }
    }
}