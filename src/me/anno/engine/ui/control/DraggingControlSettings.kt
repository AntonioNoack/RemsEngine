package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.DebugAction
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs

// todo sprite atlas
// todo sprite chunk renderer, 32² chunks or so

class DraggingControlSettings : ControlSettings() {
    var snapSettings = SnappingSettings()
    var gridSettings = GridSettings()

    var debugX by ConfigRef("debug.test.vx", 0f)
    var debugY by ConfigRef("debug.test.vy", 0f)

    @DebugAction
    fun editCurrentRenderGraph() {
        val graph = RenderView.currentInstance?.renderMode?.renderGraph ?: return
        ECSSceneTabs.open(graph.ref, PlayMode.EDITING, true)
    }
}