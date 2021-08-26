package me.anno.gpu.deferred

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import kotlin.math.max

class DeferredSettingsV2(
    val layerTypes: List<DeferredLayerType>,
    fpLights: Boolean
) {

    class Layer(val type: DeferredLayerType, val textureName: String, val mapping: String) {

        fun appendMapping(fragment: StringBuilder) {
            fragment.append(textureName)
            fragment.append('.')
            fragment.append(mapping)
            fragment.append(type.map10)
            fragment.append(";\n")
        }

    }

    val layers = ArrayList<Layer>()
    val layers2: ArrayList<DeferredLayer>

    init {

        val maxTextures = layerTypes.size
        val spaceInLayers = IntArray(maxTextures) { 4 }
        val needsHighPrecision = BooleanArray(maxTextures)
        var usedTextures0 = 0

        for (layerType in layerTypes) {
            val dimensions = layerType.dimensions
            val layerIndex = spaceInLayers.indexOfFirst { it >= dimensions }
            val startIndex = 4 - spaceInLayers[layerIndex]
            val mapping = "rgba".substring(startIndex, startIndex + dimensions)
            val layer = Layer(layerType, "defLayer$layerIndex", mapping)
            layers.add(layer)
            spaceInLayers[layerIndex] -= dimensions
            usedTextures0 = max(usedTextures0, layerIndex + 1)
            if (layerType.needsHighPrecision) needsHighPrecision[layerIndex] = true
        }

        layers2 = ArrayList(usedTextures0)
        for (i in 0 until usedTextures0) {
            val layer2 = DeferredLayer(
                "vec4", "defLayer$i",
                if (needsHighPrecision[i]) TargetType.FloatTarget4
                else TargetType.UByteTarget4
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
        varyingSource: List<Variable>,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {
        // what do we do, if the position is missing? we cannot do anything...
        val vertex = if (instanced) "#define INSTANCED;\n$vertexSource" else vertexSource
        val fragment = StringBuilder(16)
        appendLayerDeclarators(fragment)
        val oldFragmentCode = fragmentSource
            .substring(0, fragmentSource.lastIndexOf('}'))
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

        val shader = Shader(shaderName, geometrySource, vertex, varyingSource, fragment.toString())
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

    fun appendLayerWriters(output: StringBuilder) {
        for (layer in layers) {
            output.append(layer.textureName)
            output.append('.')
            output.append(layer.mapping)
            output.append(" = ")
            output.append(layer.type.glslName)
            output.append(layer.type.map01)
            output.append(";\n")
        }
    }

    fun createPostProcessingShader(
        shaderName: String,
        geometrySource: String?,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {
        val vertex = "" +
                "a2 attr0;\n" +
                "u2 pos, size;\n" +
                "void main(){\n" +
                "   gl_Position = vec4((pos + attr0 * size)*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}"
        val varying = listOf(Variable("vec2", "uv"))
        return createPostProcessingShader(shaderName, vertex, varying, geometrySource, fragmentSource, textures)
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

    companion object {
        val glslTypes = listOf("float", "vec2", "vec3", "vec4")
    }

}