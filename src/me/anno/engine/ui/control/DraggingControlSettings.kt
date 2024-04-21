package me.anno.engine.ui.control

import me.anno.config.ConfigRef

class DraggingControlSettings : ControlSettings() {
    var snapSettings = SnappingSettings()
    var gridSettings = GridSettings()

    var debugX by ConfigRef("debug.test.vx", 0f)
    var debugY by ConfigRef("debug.test.vy", 0f)
}