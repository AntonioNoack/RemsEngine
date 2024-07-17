package me.anno.ecs.components.light.sky

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CubemapModel
import me.anno.ecs.components.light.sky.shaders.CubemapSkyboxShader
import me.anno.gpu.CullMode

/**
 * Typical skybox layout in a texture file:
 *    +y
 * -x -z +x +z
 *    -y
 * */
class CubemapSkybox : TextureSkybox() {

    init {
        material.shader = CubemapSkyboxShader
        material.cullMode = CullMode.BOTH
    }

    override fun getMesh(): Mesh = CubemapModel.model.back
}