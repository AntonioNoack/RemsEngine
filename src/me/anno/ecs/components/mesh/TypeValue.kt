package me.anno.ecs.components.mesh

import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.*
import me.anno.gpu.texture.TextureLib.whiteTex2da
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.ImageGPUCache
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import org.joml.*

open class TypeValue(var type: GLSLType, open var value: Any) : Saveable() {

    companion object {
        private val LOGGER = LogManager.getLogger(TypeValue::class)
    }

    override fun toString() = "$type:$value"
    override val className: String get() = "TypeValue"

    fun bind(shader: Shader, uniformName: String) {
        val location = when (type) {
            GLSLType.S2D, GLSLType.S2DI, GLSLType.S2DU,
            GLSLType.S2DMS, GLSLType.S3D, GLSLType.SCube,
            GLSLType.S2DA -> shader.getTextureIndex(uniformName)
            else -> shader[uniformName]
        }
        if (location >= 0) bind(shader, location)
    }

    fun bind(shader: Shader, location: Int) {
        val value = value
        when (type) {
            GLSLType.V1B -> when (value) {
                is Boolean -> shader.v1b(location, value)
                is Int -> shader.v1b(location, value != 0)
                is Float -> shader.v1b(location, value.isFinite() && value != 0f)
                is Double -> shader.v1b(location, value.isFinite() && value != 0.0)
                is () -> Any? -> shader.v1b(location, value.invoke() as Boolean)
                else -> LOGGER.warn("Unknown type for V1B, ${value::class.simpleName}")
            }
            GLSLType.V1I -> when (value) {
                is Int -> shader.v1i(location, value)
                is Long -> shader.v1i(location, value.toInt())
                is Float -> shader.v1i(location, value.toInt())
                is Double -> shader.v1i(location, value.toInt())
                is () -> Any? -> shader.v1i(location, value.invoke() as Int)
                else -> LOGGER.warn("Unknown type for V1I, ${value::class.simpleName}")
            }
            GLSLType.V2I -> shader.v2i(location, value as Vector2i)
            GLSLType.V3I -> shader.v3i(location, value as Vector3i)
            GLSLType.V4I -> shader.v4i(location, value as Vector4i)
            GLSLType.V1F -> when (value) {
                is Int -> shader.v1f(location, value.toFloat())
                is Long -> shader.v1f(location, value.toFloat())
                is Float -> shader.v1f(location, value)
                is Double -> shader.v1f(location, value.toFloat())
                is () -> Any? -> shader.v1f(location, value.invoke() as Float)
                else -> LOGGER.warn("Unknown type for V1F, ${value::class.simpleName}")
            }
            GLSLType.V2F -> shader.v2f(location, value as Vector2f)
            GLSLType.V3F -> shader.v3f(location, value as Vector3f)
            GLSLType.V4F -> when (value) {
                is Quaternionf -> shader.v4f(location, value)
                is Vector4f -> shader.v4f(location, value)
                is Planef -> shader.v4f(location, value.dirX, value.dirY, value.dirZ, value.distance)
                else -> LOGGER.warn("Unknown type for V4F, ${value::class.simpleName}")
            }
            GLSLType.M2x2 -> shader.m2x2(location, value as Matrix2f)
            GLSLType.M3x3 -> shader.m3x3(location, value as Matrix3f)
            GLSLType.M4x3 -> shader.m4x3(location, value as Matrix4x3f)
            GLSLType.M4x4 -> shader.m4x4(location, value as Matrix4f)
            GLSLType.S2D, GLSLType.S2DU, GLSLType.S2DI, GLSLType.S2DMS -> {
                when (value) {
                    is Texture2D -> {
                        if (value.isCreated) {
                            value.bind(location)
                        } else {
                            whiteTexture.bind(location)
                            LOGGER.warn("Texture ${value.name} has not been created")
                        }
                    }
                    is Framebuffer -> value.bindTexture0(location, GPUFiltering.TRULY_NEAREST, Clamping.REPEAT)
                    is ITexture2D -> value.bind(location, GPUFiltering.TRULY_NEAREST, Clamping.REPEAT)
                    is FileReference -> {
                        val value1 = ImageGPUCache[value, true]
                        if (value1 != null && value1.isCreated) {
                            value1.bind(location)
                        } else {
                            whiteTexture.bind(location)
                            if (value1 != null) LOGGER.warn("Texture ${value1.name} has not been created")
                        }
                    }
                    else -> LOGGER.warn("Unsupported type for S2D: ${value.javaClass}")
                }
            }
            GLSLType.S2DA -> {
                value as Texture2DArray
                if (value.isCreated) {
                    value.bind(location, value.filtering, value.clamping)
                } else {
                    whiteTex2da.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            GLSLType.S3D -> {
                value as Texture3D
                if (value.isCreated) {
                    value.bind(location, value.filtering, value.clamping)
                } else {
                    whiteTex3d.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            GLSLType.SCube -> {
                value as CubemapTexture
                if (value.isCreated) {
                    value.bind(location, value.filtering)
                } else {
                    whiteTex3d.bind(location)
                    LOGGER.warn("Texture ${value.name} has not been created")
                }
            }
            else -> LOGGER.warn("Type $type is not yet supported")
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type, true)
        writer.writeSomething(this, "value", value, true)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = GLSLType.values.firstOrNull { it.id == value } ?: type
        else super.readInt(name, value)
    }

    override fun readSomething(name: String, value: Any?) {
        if (name == "value" && value != null) this.value = value
        else super.readSomething(name, value)
    }

}
