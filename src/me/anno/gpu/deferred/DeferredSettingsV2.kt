package me.anno.gpu.deferred

import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.OpenGLShader.Companion.attribute
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import org.joml.Vector4f
import kotlin.math.max

class DeferredSettingsV2(
    val layerTypes: List<DeferredLayerType>,
    fpLights: Boolean
) {

    class Layer(val type: DeferredLayerType, val textureName: String, val layerIndex: Int, val mapping: String) {

        fun appendMapping(fragment: StringBuilder) {
            fragment.append(textureName)
            fragment.append('.')
            fragment.append(mapping)
            fragment.append(type.map10)
            fragment.append(";\n")
        }

        fun appendMapping(fragment: StringBuilder, suffix: String, uv: String, imported: MutableSet<String>) {
            if (imported.add(textureName)) {
                fragment.append("vec4 ")
                fragment.append(textureName)
                fragment.append(suffix)
                fragment.append(" = texture(")
                fragment.append(textureName)
                fragment.append(", ")
                fragment.append(uv)
                fragment.append(");\n")
            }
            fragment.append(glslTypes[type.dimensions - 1])
            fragment.append(' ')
            fragment.append(type.glslName)
            fragment.append(" = ")
            fragment.append(textureName)
            fragment.append(suffix)
            fragment.append('.')
            fragment.append(mapping)
            fragment.append(type.map10)
            fragment.append(";\n")
        }

        fun appendLayer(output: StringBuilder) {
            output.append(textureName)
            output.append('.')
            output.append(mapping)
            output.append(" = ")
            output.append(type.glslName)
            output.append(type.map01)
            output.append(";\n")
        }

    }

    val layers = ArrayList<Layer>()
    val layers2: ArrayList<DeferredLayer>

    init {

        val maxTextures = layerTypes.size
        val spaceInLayers = IntArray(maxTextures) { 4 }
        val needsHighPrecision = Array(maxTextures) { BufferQuality.LOW_8 }
        var usedTextures0 = -1

        for (layerType in layerTypes) {
            val dimensions = layerType.dimensions
            val layerIndex = spaceInLayers.indexOfFirst { it >= dimensions }
            val startIndex = 4 - spaceInLayers[layerIndex]
            val mapping = "rgba".substring(startIndex, startIndex + dimensions)
            val layer = Layer(layerType, "defLayer$layerIndex", layerIndex, mapping)
            layers.add(layer)
            spaceInLayers[layerIndex] -= dimensions
            usedTextures0 = max(usedTextures0, layerIndex)
            needsHighPrecision[layerIndex] = needsHighPrecision[layerIndex].max(layerType.minimumQuality)
        }
        usedTextures0++

        layers2 = ArrayList(usedTextures0)
        for (i in 0 until usedTextures0) {
            val layer2 = DeferredLayer(
                "vec4", "defLayer$i",
                when (needsHighPrecision[i]) {
                    BufferQuality.LOW_8 -> TargetType.UByteTarget4
                    BufferQuality.MEDIUM_12 -> TargetType.Normal12Target4
                    BufferQuality.HIGH_16 -> TargetType.HalfFloatTarget4
                    else -> TargetType.FloatTarget4
                }
            )
            layers2.add(layer2)
        }

    }

    val settingsV1 = DeferredSettingsV1(layers2, fpLights)

    fun createBaseBuffer() = DeferredBuffers.getBaseBuffer(settingsV1)
    fun createLightBuffer() = DeferredBuffers.getLightBuffer(settingsV1)

    fun createShader(
        shaderName: String,
        geometrySource: String?,
        instanced: Boolean,
        vertexSource: String,
        varyings: List<Variable>,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {

        // what do we do, if the position is missing? we cannot do anything...
        val vertex = if (instanced) "#define INSTANCED;\n$vertexSource" else vertexSource
        val fragment = StringBuilder(16)

        appendLayerDeclarators(fragment)

        val lio = fragmentSource.lastIndexOf('}')
        if (lio < 0) throw RuntimeException("Expected to find } in fragment source, but only got '$vertexSource'/'$geometrySource'/'$fragmentSource'")

        val oldFragmentCode = fragmentSource
            .substring(0, lio)
            .replace("gl_FragColor", "vec4 glFragColor")

        fragment.append(oldFragmentCode)

        val hasFragColor = "gl_FragColor" in fragmentSource
        if (hasFragColor) {
            fragment.append("vec3 finalColor = glFragColor.rgb;\n")
            fragment.append("float finalAlpha = glFragColor.a;\n")
        }

        appendMissingDeclarations(fragment)
        appendLayerWriters(fragment)

        fragment.append("}")

        val shader = Shader(shaderName, geometrySource, vertex, varyings, fragment.toString())
        shader.glslVersion = 330
        shader.setTextureIndices(textures)
        return shader
    }

    fun appendMissingDeclarations(output: StringBuilder) {
        for (type in layerTypes) {
            if (type.glslName !in output) {
                type.appendDefinition(output)
                output.append(" = ")
                type.appendDefaultValue(output)
                output.append(";\n")
            }
        }
    }

    fun appendLayerDeclarators(output: StringBuilder) {
        for ((index, type) in settingsV1.layers.withIndex()) {
            output.append("layout (location = ")
            output.append(index)
            output.append(") out vec4 ")
            output.append(type.name)
            output.append(";\n")
        }
    }


    fun getLayerOutputVariables(): List<Variable> {
        return settingsV1.layers.map { type ->
            Variable(GLSLType.V4F, type.name, VariableMode.OUT)
        }
    }

    fun appendLayerWriters(output: StringBuilder) {
        for (layer in layers) {
            layer.appendLayer(output)
        }
    }

    fun createPostProcessingShader(
        shaderName: String,
        geometrySource: String?,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {
        val vertex = "" +
                "$attribute vec2 attr0;\n" +
                "uniform vec2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}"
        return createPostProcessingShader(shaderName, vertex, uvList, geometrySource, fragmentSource, textures)
    }

    fun createPostProcessFragment(
        fragmentSource: String,
        textures: List<String>?
    ): String {
        val index = fragmentSource.indexOf("main")
        val index2 = fragmentSource.indexOf('{', index + 4) + 1
        val fragment = StringBuilder()
        if (textures != null) {
            // declare all layer values
            for (layer in layers2) {
                if (layer.name in textures) {
                    fragment.append("uniform sampler2D ")
                    fragment.append(layer.name)
                    fragment.append(";\n")
                }
            }
        }
        fragment.append(fragmentSource.substring(0, index2))
        // only assign them, if they appear
        if (textures != null) {
            // load all layer values
            for (layer in layers2) {
                if (layer.name in textures) {
                    fragment.append("vec4 _")
                    fragment.append(layer.name)
                    fragment.append(" = texture(")
                    fragment.append(layer.name)
                    fragment.append(", uv);\n")
                }
            }
            // assign them to their meaning
            for (layer in layers) {
                if (layer.textureName in textures) {
                    val type = layer.type
                    type.appendDefinition(fragment)
                    fragment.append(" = _")
                    layer.appendMapping(fragment)
                }
            }
        }
        fragment.append(fragmentSource.substring(index2))
        return fragment.toString()
    }

    fun createPostProcessingShader(
        shaderName: String,
        vertex: String,
        varying: List<Variable>,
        geometrySource: String?,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {
        val fragment = createPostProcessFragment(fragmentSource, textures)
        val shader = Shader(shaderName, geometrySource, vertex, varying, fragment.toString())
        shader.setTextureIndices(textures)
        return shader
    }

    fun createPostProcessingShader(
        shaderName: String,
        vertex: String,
        varying: List<Variable>,
        fragmentSource: String,
        textures: List<String>?
    ): BaseShader {
        val fragment = createPostProcessFragment(fragmentSource, textures)
        val shader = BaseShader(shaderName, vertex, varying, fragment)
        shader.setTextureIndices(textures)
        return shader
    }

    fun findLayer(type: DeferredLayerType): Layer? {
        return layers.firstOrNull { it.type == type }
    }

    fun findTexture(buffer: Framebuffer, type: DeferredLayerType): Texture2D? {
        val layer = layers.firstOrNull { it.type == type } ?: return null
        return buffer.textures[layer.layerIndex]
    }

    fun findTexture(buffer: Framebuffer, layer: Layer): Texture2D? {
        return buffer.textures[layer.layerIndex]
    }

    companion object {

        val glslTypes = listOf(GLSLType.V1F, GLSLType.V2F, GLSLType.V3F, GLSLType.V4F)

        val singleToVector = mapOf(
            "r" to Vector4f(1f, 0f, 0f, 0f),
            "g" to Vector4f(0f, 1f, 0f, 0f),
            "b" to Vector4f(0f, 0f, 1f, 0f),
            "a" to Vector4f(0f, 0f, 0f, 1f)
        )

    }

}