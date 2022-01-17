package me.anno.ecs.components.mesh

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import org.apache.logging.log4j.LogManager
import org.joml.*

class TypeValue(val type: GLSLType, val value: Any) {

    companion object {
        private val LOGGER = LogManager.getLogger(TypeValue::class)
    }

    fun bind(shader: Shader, uniformName: String) {
        val location = when (type) {
            GLSLType.S2D, GLSLType.SCube -> shader.getTextureIndex(uniformName)
            else -> shader[uniformName]
        }
        if (location >= 0) bind(shader, location)
    }

    fun bind(shader: Shader, location: Int) {
        when (type) {
            GLSLType.BOOL -> shader.v1b(location, value as Boolean)
            GLSLType.V1I -> shader.v1i(location, value as Int)
            GLSLType.V2I -> shader.v2i(location, value as Vector2ic)
            GLSLType.V3I -> shader.v3i(location, value as Vector3ic)
            GLSLType.V4I -> shader.v4i(location, value as Vector4ic)
            GLSLType.V1F -> shader.v1f(location, value as Float)
            GLSLType.V2F -> shader.v2f(location, value as Vector2fc)
            GLSLType.V3F -> shader.v3f(location, value as Vector3fc)
            GLSLType.V4F -> shader.v4f(location, value as Vector4fc)
            GLSLType.M3x3 -> shader.m3x3(location, value as Matrix3fc)
            GLSLType.M4x3 -> shader.m4x3(location, value as Matrix4x3fc)
            GLSLType.M4x4 -> shader.m4x4(location, value as Matrix4fc)
            GLSLType.S2D -> {
                value as Texture2D
                if (value.isCreated) {
                    value.bind(location)
                } else {
                    whiteTexture.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            else -> LOGGER.warn("Type $type is not yet supported")
        }
    }

}
