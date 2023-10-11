package me.anno.ecs.components.shaders

/**
 * UVs: U ~ atan2(z,x), V ~ atan2(length(x,z),y)
 * */
class UVSkybox : TextureSkybox() {

    init {
        material.shader = defaultShader
    }

    override val className: String
        get() = "UVSkybox"

    companion object {
        val defaultShader = UVSkyboxShader("uv-skybox")
    }
}