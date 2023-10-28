package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.utils.structures.maps.LazyMap
import org.apache.logging.log4j.LogManager

object TextureMapper {

    private val LOGGER = LogManager.getLogger(TextureMapper::class)

    private val shaders = LazyMap<String, Shader> { mapping ->
        Shader(
            "Map<$mapping>", coordsList, coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.S2D, "srcTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main(){\n" +
                    "   vec4 source = texture(srcTex, uv);\n" +
                    (if ('l' in mapping) {
                        "float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));\n"
                    } else "") +
                    "   result = vec4(${
                        mapping.map {
                            when (it) {
                                in "RGBA" -> "1.0-source.${it.lowercaseChar()}"
                                in "rgba" -> "source.$it"
                                '0' -> "0.0"
                                '1' -> "1.0"
                                'l' -> "luminance"
                                else -> throw IllegalArgumentException("Unknown map '$it'")
                            }
                        }.joinToString()
                    });\n" +
                    "}\n"
        )
    }

    fun mapTexture(src: ITexture2D, dst: Texture2D, mapping: String, type: TargetType) {
        LOGGER.debug("Mapping {} to {}/{} via {}", src, dst, type, mapping)
        if (mapping.length != 4) throw IllegalArgumentException()
        if (GFX.isGFXThread()) {
            if (src !is Texture2D || src.isCreated) {
                dst.create(type)
                useFrame(dst, 0) {
                    renderPurely {
                        val shader = shaders[mapping]
                        shader.use()
                        src.bindTrulyLinear(0)
                        flat01.draw(shader)
                    }
                }
            } else LOGGER.warn("Mapping failed, because src wasn't created")
        } else {
            GFX.addGPUTask(mapping, dst.width, dst.height) {
                mapTexture(src, dst, mapping, type)
            }
        }
    }
}