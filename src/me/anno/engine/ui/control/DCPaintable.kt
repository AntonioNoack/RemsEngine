package me.anno.engine.ui.control

import me.anno.ecs.components.mesh.material.MaterialBase
import me.anno.io.files.FileReference

interface DCPaintable {
    fun paint(self: DraggingControls, color: MaterialBase, file: FileReference)
}