package me.anno.graph.render.scene

import me.anno.engine.ui.render.RenderView
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.NodeOutput
import me.anno.graph.types.flow.actions.ActionNode

abstract class RenderSceneNode0(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {

    lateinit var renderView: RenderView
    lateinit var pipeline: Pipeline

    // todo this is suboptimal:
    //  if we have two RenderViews using the same node, different sizes,
    //  we'd constantly destroy this
    var framebuffer: IFramebuffer? = null

    open fun invalidate() {
    }

    override fun onDestroy() {
        super.onDestroy()
        invalidate()
        framebuffer?.destroy()
    }

    companion object {
        fun isOutputUsed(output: NodeOutput): Boolean {
            return output.others.isNotEmpty()
        }
    }
}