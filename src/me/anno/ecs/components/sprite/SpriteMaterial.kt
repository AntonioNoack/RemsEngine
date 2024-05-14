package me.anno.ecs.components.sprite

import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.gpu.shader.GLSLType
import org.joml.Vector2i

class SpriteMaterial : Material() {

    var textureTileCount = Vector2i(16, 16)
        set(value) {
            field.set(value)
        }

    init {
        shaderOverrides["textureTileCount"] = TypeValue(GLSLType.V2I, textureTileCount)
        linearFiltering = false
    }
}