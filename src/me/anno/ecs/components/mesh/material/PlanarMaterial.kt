package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.material.shaders.PlanarShader

class PlanarMaterial : PlanarMaterialBase() {
    init {
        shader = PlanarShader
    }
}