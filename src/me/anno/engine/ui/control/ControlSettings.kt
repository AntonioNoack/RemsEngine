package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.engine.inspector.Inspectable

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
    @Range(0.01, 179.9) // todo range isn't properly checked/clamped!!
    var fovY by ConfigRef("ui.camera.fovY", 90f)

    @Group("Rendering")
    var useBoxCulling by ConfigRef("gfx.fx.enableBoxCulling", true)

    @Group("Debug")
    var showRenderTimes by ConfigRef("debug.ui.showRenderTimes", false)

    @Group("Debug")
    var showDebugFrames by ConfigRef("debug.ui.showDebugFrames", false)

    @Group("Debug")
    var debugGhostingMillis by ConfigRef("debug.ui.debugGhostingMillis", 0L)

    @Group("Debug")
    var inspectedX by ConfigRef("debug.ui.inspectedX", 0)

    @Group("Debug")
    var inspectedY by ConfigRef("debug.ui.inspectedY", 0)

    @Group("Debug")
    var drawInspected by ConfigRef("debug.ui.drawInspected", false)

    @Group("Debug")
    var displayVRInRedCyan by ConfigRef("debug.showStereoInRedCyan", false)
}