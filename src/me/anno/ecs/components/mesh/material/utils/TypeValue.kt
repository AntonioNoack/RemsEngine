package me.anno.ecs.components.mesh.material.utils

import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import org.apache.logging.log4j.LogManager
import org.joml.Matrix2f
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Planef
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i

open class TypeValue(var type: GLSLType, open var value: Any) : Saveable() {

    companion object {
        private val LOGGER = LogManager.getLogger(TypeValue::class)
    }

    override fun toString() = "$type:$value"

    fun bind(shader: GPUShader, uniformName: String) {
        val location = when (type) {
            GLSLType.S2D, GLSLType.S2DI, GLSLType.S2DU,
            GLSLType.S2DMS, GLSLType.S3D, GLSLType.SCube,
            GLSLType.S2DA -> shader.getTextureIndex(uniformName)
            else -> shader[uniformName]
        }
        if (location >= 0) bind(shader, location)
    }

    open fun bind(shader: GPUShader, location: Int) {
        val value = value
        when (type) {
            GLSLType.V1B -> when (value) {
                is Boolean -> shader.v1b(location, value)
                is Int -> shader.v1b(location, value != 0)
                is Float -> shader.v1b(location, value.isFinite() && value != 0f)
                is Double -> shader.v1b(location, value.isFinite() && value != 0.0)
                is () -> Any? -> shader.v1b(location, value.invoke() as Boolean)
                else -> warnUnknownType("V1B", value)
            }
            GLSLType.V1I -> when (value) {
                is Number -> shader.v1i(location, AnyToInt.getInt(value))
                is () -> Any? -> shader.v1i(location, value.invoke() as Int)
                else -> warnUnknownType("V1I", value)
            }
            GLSLType.V2I -> shader.v2i(location, value as Vector2i)
            GLSLType.V3I -> shader.v3i(location, value as Vector3i)
            GLSLType.V4I -> shader.v4i(location, value as Vector4i)
            GLSLType.V1F -> when (value) {
                is Number -> shader.v1f(location, AnyToFloat.getFloat(value))
                is () -> Any? -> shader.v1f(location, value.invoke() as Float)
                else -> warnUnknownType("V1F", value)
            }
            GLSLType.V2F -> shader.v2f(location, value as Vector2f)
            GLSLType.V3F -> shader.v3f(location, value as Vector3f)
            GLSLType.V4F -> when (value) {
                is Quaternionf -> shader.v4f(location, value)
                is Vector4f -> shader.v4f(location, value)
                is Planef -> shader.v4f(location, value.dirX, value.dirY, value.dirZ, value.distance)
                else -> warnUnknownType("V4F", value)
            }
            GLSLType.M2x2 -> shader.m2x2(location, value as Matrix2f)
            GLSLType.M3x3 -> shader.m3x3(location, value as Matrix3f)
            GLSLType.M4x3 -> shader.m4x3(location, value as Matrix4x3f)
            GLSLType.M4x4 -> shader.m4x4(location, value as Matrix4f)
            GLSLType.S2D, GLSLType.S2DU, GLSLType.S2DI -> {
                when (value) {
                    is Texture2D -> {
                        if (value.wasCreated) {
                            value.bind(location)
                        } else {
                            TextureLib.whiteTexture.bind(location)
                            warnNotCreated("Texture", value)
                        }
                    }
                    is Framebuffer -> value.bindTexture0(location, Filtering.TRULY_NEAREST, Clamping.REPEAT)
                    is ITexture2D -> value.bind(location, Filtering.TRULY_NEAREST, Clamping.REPEAT)
                    is FileReference -> {
                        val value1 = TextureCache[value, true]
                        if (value1 != null && value1.isCreated()) {
                            value1.bind(location)
                        } else {
                            TextureLib.whiteTexture.bind(location)
                            if (value1 != null) warnNotCreated("Texture", value.absolutePath)
                        }
                    }
                    else -> LOGGER.warn("Unsupported type for S2D: ${value::class}")
                }
            }
            GLSLType.S2DMS -> {
                when (value) {
                    is Texture2D -> {
                        if (value.wasCreated) {
                            value.bind(location)
                        } else {
                            TextureLib.whiteTexture.bind(location)
                            warnNotCreated("Texture", value)
                        }
                    }
                    is Framebuffer -> value.getTextureIMS(0).bind(location, Filtering.TRULY_NEAREST, Clamping.REPEAT)
                    is ITexture2D -> value.bind(location, Filtering.TRULY_NEAREST, Clamping.REPEAT)
                    is FileReference -> {
                        val value1 = TextureCache[value, true]
                        if (value1 != null && value1.isCreated()) {
                            value1.bind(location)
                        } else {
                            TextureLib.whiteTexture.bind(location)
                            if (value1 != null) warnNotCreated("Texture", value.absolutePath)
                        }
                    }
                    else -> LOGGER.warn("Unsupported type for S2D: ${value::class}")
                }
            }
            GLSLType.S2DA -> {
                value as Texture2DArray
                if (value.wasCreated) {
                    value.bind(location, value.filtering, value.clamping)
                } else {
                    TextureLib.whiteTex2da.bind(location)
                    warnNotCreated("Texture2DArray", value)
                }
            }
            GLSLType.S3D -> {
                value as Texture3D
                if (value.wasCreated) {
                    value.bind(location, value.filtering, value.clamping)
                } else {
                    TextureLib.whiteTex3d.bind(location)
                    warnNotCreated("Texture3D", value)
                }
            }
            GLSLType.SCube -> {
                value as CubemapTexture
                if (value.wasCreated) {
                    value.bind(location, value.filtering)
                } else {
                    TextureLib.whiteTex3d.bind(location)
                    warnNotCreated("CubemapTexture", value)
                }
            }
            else -> LOGGER.warn("Type $type is not yet supported")
        }
    }

    private fun warnUnknownType(type: String, value: Any) {
        LOGGER.warn("Unknown type for $type, ${value::class.simpleName}")
    }

    private fun warnNotCreated(type: String, texture: ITexture2D) {
        warnNotCreated(type, texture.name)
    }

    private fun warnNotCreated(type: String, name: String) {
        LOGGER.warn("$type $name has not been created")
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type, true)
        writer.writeSomething(this, "value", value, true)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "value" -> this.value = value ?: return
            "type" -> {
                if (value !is Int) return
                type = GLSLType.entries.firstOrNull { it.id == value } ?: type
            }
            else -> super.setProperty(name, value)
        }
    }
}