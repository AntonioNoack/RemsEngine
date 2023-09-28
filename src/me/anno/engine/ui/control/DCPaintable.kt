package me.anno.engine.ui.control

import me.anno.ecs.components.mesh.Material
import me.anno.io.files.FileReference

interface DCPaintable {
    fun paint(self: DraggingControls, color: Material, file: FileReference)
}