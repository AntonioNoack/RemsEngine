package me.anno.ecs.components.mesh

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureLib.whiteTexture
import org.apache.logging.log4j.LogManager
import org.joml.*

class TypeValue(val type: GLSLType, val value: Any) {

    companion object {
        private val LOGGER = LogManager.getLogger(TypeValue::class)
    }

    fun bind(shader: Shader, uniformName: String) {
        val location = when (type) {
            GLSLType.S2D, GLSLType.S3D, GLSLType.SCube -> shader.getTextureIndex(uniformName)
            else -> shader[uniformName]
        }
        if (location >= 0) bind(shader, location)
    }

    fun bind(shader: Shader, location: Int) {
        val value = value
        when (type) {
            GLSLType.BOOL -> when (value) {
                is Boolean -> shader.v1b(location, value)
                is () -> Any? -> shader.v1b(location, value.invoke() as Boolean)
                else -> LOGGER.warn("Unknown type for BOOL, ${value.javaClass.superclass}")
            }
            GLSLType.V1I -> when (value) {
                is Int -> shader.v1i(location, value)
                is () -> Any? -> shader.v1i(location, value.invoke() as Int)
                else -> LOGGER.warn("Unknown type for V1I, ${value.javaClass.superclass}")
            }
            GLSLType.V2I -> shader.v2i(location, value as Vector2ic)
            GLSLType.V3I -> shader.v3i(location, value as Vector3ic)
            GLSLType.V4I -> shader.v4i(location, value as Vector4ic)
            GLSLType.V1F -> when (value) {
                is Float -> shader.v1f(location, value)
                is () -> Any? -> shader.v1f(location, value.invoke() as Float)
                else -> LOGGER.warn("Unknown type for V1F, ${value.javaClass.superclass}")
            }
            GLSLType.V2F -> shader.v2f(location, value as Vector2fc)
            GLSLType.V3F -> shader.v3f(location, value as Vector3fc)
            GLSLType.V4F -> when (value) {
                is Quaternionfc -> shader.v4f(location, value)
                is Vector4fc -> shader.v4f(location, value)
                is Planef -> shader.v4f(location, value.a, value.b, value.c, value.d)
                else -> LOGGER.warn("Unknown type for V4F, ${value.javaClass.superclass}")
            }
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
            GLSLType.S3D -> {
                value as Texture3D
                if (value.isCreated) {
                    value.bind(location, value.filtering)
                } else {
                    // todo can we do that?
                    whiteTexture.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            GLSLType.SCube -> {
                value as CubemapTexture
                if (value.isCreated) {
                    value.bind(location, value.filtering)
                } else {
                    // todo can we do that?
                    whiteTexture.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            else -> LOGGER.warn("Type $type is not yet supported")
        }
    }

}
