package me.anno.gpu.deferred

import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable

open class DeferredShader(
    settings: DeferredSettingsV1,
    shaderName: String,
    v3D: String,
    y3D: List<Variable>,
    f3D: String,
    textures: List<String>?
) : Shader(shaderName, v3D, y3D, settings.f3D + f3D) {

    init {
        glslVersion = 330
        use()
        if (textures != null) {
            setTextureIndices(textures)
        }
    }

}