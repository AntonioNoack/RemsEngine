package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.ecs.annotations.Range
import me.anno.engine.inspector.Inspectable

open class ControlSettings : Inspectable {

    var changeYByWASD by ConfigRef("ui.moveSettings.changeYByWASD", false)
    var enableShiftSpaceControls by ConfigRef("ui.moveSettings.enableShiftSpace", false)

    var moveSpeed by ConfigRef("ui.moveSettings.moveSpeed", 1f)
    var turnSpeed by ConfigRef("ui.moveSettings.turnSpeed", 1f)

    @Range(0.01, 179.9)
    var fovY by ConfigRef("ui.camera.fovY", 90f)
}