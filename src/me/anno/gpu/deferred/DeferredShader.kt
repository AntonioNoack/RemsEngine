package me.anno.gpu.deferred

import me.anno.gpu.shader.Shader
import org.lwjgl.opengl.GL20

open class DeferredShader(
    settings: DeferredSettingsV1,
    shaderName: String,
    v3D: String,
    y3D: String,
    f3D: String,
    textures: List<String>?
) : Shader(shaderName, v3D, y3D, settings.f3D + f3D) {

    init {
        glslVersion = 330
        use()
        if (textures != null) {
            for (index in textures.indices) {
                val textureName = textures[index]
                val nameIndex = this[textureName]
                if (nameIndex >= 0) {
                    GL20.glUniform1i(nameIndex, index)
                }
            }
        }
    }

}