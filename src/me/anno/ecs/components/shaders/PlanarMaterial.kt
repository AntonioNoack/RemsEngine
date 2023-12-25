package me.anno.ecs.components.shaders

class PlanarMaterial : PlanarMaterialBase() {

    init {
        shader = PlanarShader
    }

    override val className: String get() = "PlanarMaterial"
}