package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Range
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SuperMaterial

open class ControlSettings : Inspectable {

    @Group("Control Options")
    var changeYByWASD by ConfigRef("ui.moveSettings.changeYByWASD", false)

    @Group("Control Options")
    var enableShiftSpaceControls by ConfigRef("ui.moveSettings.enableShiftSpace", false)

    @Group("Control Options")
    @Range(-360.0, 360.0)
    var vrRotateLeftRight by ConfigRef("ui.moveSettings.vrLeftRight", 45f)

    @Group("Control Speed")
    var moveSpeed by ConfigRef("ui.moveSettings.moveSpeed", 1f)

    @Group("Control Speed")
    var turnSpeed by ConfigRef("ui.moveSettings.turnSpeed", 1f)

    @Group("Rendering")
    @Range(0.01, 179.9)
    var fovY by ConfigRef("ui.camera.fovY", 90f)

    @Group("Debug")
    var showRenderTimes by ConfigRef("debug.ui.showRenderTimes", false)

    @Group("Debug")
    var showDebugFrames by ConfigRef("debug.ui.showDebugFrames", false)
}