package me.anno.tests.terrain.v1

import me.anno.ecs.Component
import me.anno.ecs.annotations.Type
import org.joml.Vector3f

class EditSettings : Component() {
    var editMode: TerrainEditMode = TerrainEditMode.PAINTING

    @Type("Color3")
    var color = Vector3f(1f, 0f, 0f)
}
