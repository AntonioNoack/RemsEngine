package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.DebugAction
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs

class DraggingControlSettings : ControlSettings() {
    var snapSettings = SnappingSettings()
    var gridSettings = GridSettings()

    var interFrames by ConfigRef("gpu.frameGen.intermediateFrames", 1)

    @DebugAction
    fun editCurrentRenderGraph() {
        val graph = RenderView.currentInstance?.renderMode?.renderGraph ?: return
        ECSSceneTabs.open(graph.ref, PlayMode.EDITING, true)
    }
}