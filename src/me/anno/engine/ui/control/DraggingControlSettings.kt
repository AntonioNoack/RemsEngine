package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Group
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SuperMaterial
import me.anno.engine.ui.scenetabs.ECSSceneTabs

class DraggingControlSettings : ControlSettings() {

    var snapSettings = SnappingSettings()

    @Group("Grid")
    var showGridXZ by ConfigRef("ui.gridSettings.showGridXZ", true)

    @Group("Grid")
    var showGridXY by ConfigRef("ui.gridSettings.showGridXY", false)

    @Group("Grid")
    var showGridYZ by ConfigRef("ui.gridSettings.showGridYZ", false)

    @Group("Rendering")
    var renderMode = RenderMode.DEFAULT

    @Group("Rendering")
    var superMaterial = SuperMaterial.NONE

    @DebugAction
    fun editCurrentRenderGraph() {
        val graph = RenderView.currentInstance?.renderMode?.renderGraph ?: return
        ECSSceneTabs.open(graph.ref, PlayMode.EDITING, true)
    }
}