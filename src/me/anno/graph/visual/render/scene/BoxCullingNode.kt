package me.anno.graph.visual.render.scene

import me.anno.gpu.GFXState.timeRendering

class BoxCullingNode : RenderViewNode(
    "BoxCulling", emptyList(), emptyList()
) {
    override fun executeAction() {
        val depthFB = pipeline.prevDepthBuffer
        // make this optional, because it has pop-in artefacts
        val enable = renderView.controlScheme?.settings?.useBoxCulling ?: true
        if (enable && depthFB != null) {
            timeRendering(name, timer) {
                pipeline.bakeBoxCulling(depthFB)
            }
        }
    }
}