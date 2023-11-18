package me.anno.engine.ui.control

import me.anno.config.ConfigRef
import me.anno.studio.Inspectable

class GridSettings : Inspectable {
    // todo setting to snap grid itself???
    var showGridXZ by ConfigRef("ui.gridSettings.showGridXZ", true)
    var showGridXY by ConfigRef("ui.gridSettings.showGridXY", false)
    var showGridYZ by ConfigRef("ui.gridSettings.showGridYZ", false)
}