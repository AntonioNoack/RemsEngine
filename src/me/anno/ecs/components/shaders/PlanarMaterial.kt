package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.Shader

class PlanarMaterial : PlanarMaterialBase() {

    init {
        shader = PlanarShader
    }

    override val className: String get() = "PlanarMaterial"

}