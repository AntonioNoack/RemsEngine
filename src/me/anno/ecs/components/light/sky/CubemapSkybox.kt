package me.anno.ecs.components.light.sky

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CubemapModel
import me.anno.ecs.components.light.sky.shaders.CubemapSkyboxShader

/**
 * Typical skybox layout in a texture file:
 *    +y
 * -x -z +x +z
 *    -y
 * */
class CubemapSkybox : TextureSkybox() {

    init {
        material.shader = CubemapSkyboxShader
    }

    override fun getMesh(): Mesh = CubemapModel

    override val className: String
        get() = "CubemapSkybox"
}