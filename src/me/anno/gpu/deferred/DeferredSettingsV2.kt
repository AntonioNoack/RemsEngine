package me.anno.gpu.deferred

import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.lists.Lists.firstOrNull2
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
                    BufferQuality.HIGH_16 -> TargetType.FP16Target4
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
        vertexVariables: List<Variable>,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentVariables: List<Variable>,
        fragmentShader: String,
        textures: List<String>?
    ): Shader {

        // what do we do, if the position is missing? we cannot do anything...
        val vertex = if (instanced) "#define INSTANCED;\n$vertexShader" else vertexShader
        val fragment = StringBuilder(16)

        appendLayerDeclarators(fragment)

        val lio = fragmentShader.lastIndexOf('}')
        if (lio < 0) throw RuntimeException("Expected to find } in fragment source, but only got '$vertexShader'/'$geometrySource'/'$fragmentShader'")

        val oldFragmentCode = fragmentShader
            .substring(0, lio)
            .replace("gl_FragColor", "vec4 glFragColor")

        fragment.append(oldFragmentCode)

        val hasFragColor = "gl_FragColor" in fragmentShader
        if (hasFragColor) {
            fragment.append("vec3 finalColor = glFragColor.rgb;\n")
            fragment.append("float finalAlpha = glFragColor.a;\n")
        }

        appendMissingDeclarations(fragment)
        appendLayerWriters(fragment)

        fragment.append("}")

        val shader = Shader(
            shaderName, geometrySource,
            vertexVariables, vertex, varyings,
            fragmentVariables, fragment.toString()
        )
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
            layer.appendLayer(output)
        }
    }

    fun findLayer(type: DeferredLayerType): Layer? {
        return layers.firstOrNull2 { it.type == type }
    }

    fun findMapping(type: DeferredLayerType): String? {
        return findLayer(type)?.mapping
    }

    fun findTexture(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = layers.firstOrNull2 { it.type == type } ?: return null
        return findTexture(buffer, layer)
    }

    fun findTexture(buffer: IFramebuffer, layer: Layer): ITexture2D {
        return buffer.getTextureI(layer.layerIndex)
    }

    fun split(index: Int, splitSize: Int): DeferredSettingsV2 {
        val index0 = index * splitSize
        val index1 = index0 + splitSize
        return DeferredSettingsV2(
            layerTypes.filter { type ->
                findLayer(type)!!.layerIndex in index0 until index1
            },
            settingsV1.fpLights
        )
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