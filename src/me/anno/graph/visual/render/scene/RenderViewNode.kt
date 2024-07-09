package me.anno.graph.visual.render.scene

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.node.NodeOutput
import me.anno.utils.structures.lists.Lists.any2

abstract class RenderViewNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    lateinit var renderView: RenderView
    lateinit var pipeline: Pipeline

    // todo this is suboptimal:
    //  if we have two RenderViews using the same node, different sizes,
    //  we'd constantly destroy this
    var framebuffer: IFramebuffer? = null

    open fun invalidate() {
    }

    override fun destroy() {
        super.destroy()
        invalidate()
        framebuffer?.destroy()
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