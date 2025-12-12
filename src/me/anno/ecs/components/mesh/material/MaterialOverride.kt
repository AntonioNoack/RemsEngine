package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Type
import me.anno.io.files.FileReference

interface MaterialOverride {
    @Docs("Overrides the mesh materials; InvalidRef/OutOfBounds will be ignored")
    @Type("List<Material/Reference>")
    val materials: List<FileReference>
}