package me.anno.gpu.deferred

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.*
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.lists.Lists.first2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector4f
import java.util.*
import kotlin.math.max

data class DeferredSettings(val layerTypes: List<DeferredLayerType>) {

    // todo shader to blit depth with different resolutions/formats

    class Layer(val type: DeferredLayerType, val textureName: String, val texIndex: Int, val mapping: String) {

        fun appendMapping(
            fragment: StringBuilder,
            dstSuffix: String,
            tmpSuffix: String,
            texSuffix: String,
            uv: String,
            imported: MutableSet<String>?,
            sampleVariableName: String?
        ) {
            val texName = textureName + texSuffix
            if (imported != null && imported.add(texName)) {
                fragment.append("vec4 ").append(textureName).append(tmpSuffix)
                if (sampleVariableName != null) {
                    fragment.append(" = texelFetch(").append(texName)
                    // texture will be sampler2DMS, so no lod is used as parameter
                    fragment.append(", ivec2(textureSize(").append(texName)
                    fragment.append(")*").append(uv)
                    fragment.append("), ").append(sampleVariableName).append(");\n")
                } else {
                    fragment.append(" = texture(").append(texName)
                    fragment.append(", ").append(uv).append(");\n")
                }
            }
            fragment.append(type.glslName).append(dstSuffix).append(" = ")
                .append(type.dataToWork).append('(').append(textureName).append(tmpSuffix)
                .append('.').append(mapping).append(");\n")
        }

        fun appendLayer(output: StringBuilder, defRR: String) {
            output.append(textureName)
            output.append('.')
            output.append(mapping)
            output.append(" = (")
            if (type == DeferredLayerType.DEPTH) {
                val depthVariableName = if ("gl_FragDepth" in output) "gl_FragDepth" else "gl_FragCoord.z"
                output.append(depthVariableName)
            } else {
                output.append(type.workToData).append('(')
                output.append(type.glslName).append(')')
            }
            // append random rounding
            output.append(")*(1.0+").append(defRR).append("*").append(textureName).append("RR.x")
            output.append(")+").append(defRR).append("*").append(textureName).append("RR.y")
            output.append(";\n")
        }
    }

    val layers = ArrayList<Layer>()
    val layers2: ArrayList<DeferredLayer>
    val emptySlots = ArrayList<Triple<Int, String, String>>() // index, name, mask

    init {

        val maxTextures = layerTypes.size
        val spaceInLayers = IntArray(maxTextures)
        spaceInLayers.fill(4)

        val needsHighPrecision = Array(maxTextures) { BufferQuality.LOW_8 }
        var usedTextures0 = -1

        fun addType(layerType: DeferredLayerType) {
            val dimensions = layerType.dataDims
            val layerIndex = spaceInLayers.indexOfFirst { it >= dimensions }
            val startIndex = 4 - spaceInLayers[layerIndex]
            val mapping = "rgba".substring(startIndex, startIndex + dimensions)
            val layer = Layer(layerType, "defLayer$layerIndex", layerIndex, mapping)
            layers.add(layer)
            spaceInLayers[layerIndex] -= dimensions
            usedTextures0 = max(usedTextures0, layerIndex)
            val op = needsHighPrecision[layerIndex]
            val np = layerType.minimumQuality
            needsHighPrecision[layerIndex] = if (op > np) op else np
        }

        // vec3s and vec4s come first, so vec3 is guaranteed to always be rgb, never gba
        for (i in layerTypes.indices) {
            val type = layerTypes[i]
            if (type.dataDims >= 3) addType(type)
        }

        for (i in layerTypes.indices) {
            val type = layerTypes[i]
            if (type.dataDims < 3) addType(type)
        }

        usedTextures0++

        layers2 = ArrayList(usedTextures0)
        for (layerIndex in 0 until usedTextures0) {
            val layer2 = DeferredLayer(
                "defLayer$layerIndex", when (needsHighPrecision[layerIndex]) {
                    BufferQuality.LOW_8 -> TargetType.UByteTarget4
                    BufferQuality.MEDIUM_12 -> TargetType.Normal12Target4
                    BufferQuality.HIGH_16 -> TargetType.FP16Target4
                    else -> TargetType.FloatTarget4
                }
            )
            layers2.add(layer2)
            val empty = spaceInLayers[layerIndex]
            if (empty > 0) {
                val mask = when (spaceInLayers[layerIndex]) {
                    1 -> "a"
                    2 -> "ba"
                    3 -> "gba"
                    else -> throw NotImplementedError()
                }
                emptySlots += Triple(layerIndex, layer2.name, mask)
            }
        }
    }

    val settingsV1 = layers2
    val targetTypes = Array(layers2.size) { layers2[it].type }

    fun createBaseBuffer(name: String, samples: Int): IFramebuffer {
        val layers = layers2
        val depthBufferType = if(GFX.supportsDepthTextures) DepthBufferType.TEXTURE
        else DepthBufferType.INTERNAL
        return if (layers.size <= GFX.maxColorAttachments) {
            Framebuffer(
                name, 1, 1, samples,
                targetTypes, depthBufferType
            )
        } else {
            MultiFramebuffer(
                name, 1, 1, samples,
                targetTypes, depthBufferType
            )
        }
    }

    fun createShader(
        shaderName: String,
        instanced: Boolean,
        vertexVariables: List<Variable>,
        vertexShader: String,
        varyings: List<Variable>,
        fragmentVariables: List<Variable>,
        fragmentShader: String,
        textures: List<String>?,
        postProcessing: List<ShaderStage>
    ): Shader {
        val vertex = if (instanced) "#define INSTANCED;\n$vertexShader" else vertexShader
        val builder = ShaderBuilder(shaderName, this)
        builder.addVertex(ShaderStage("def-vs", vertexVariables + varyings, vertex))
        builder.addFragment(ShaderStage("def-fs", fragmentVariables + varyings, fragmentShader))
        builder.addFragment(postProcessing)
        val shader = builder.create(null)
        shader.setTextureIndices(textures)
        return shader
    }

    fun appendLayerDeclarators(output: StringBuilder, disabledLayers: BitSet?) {
        val layers = settingsV1
        output.append("uniform float defRRT;\n")
        for (index in layers.indices) {
            if (disabledLayers == null || !disabledLayers[index]) {
                val type = layers[index]
                output.append("layout (location = ")
                output.append(index)
                output.append(") out vec4 ")
                output.append(type.name)
                output.append(";\n")
                output.append("uniform vec2 ").append(type.nameRR).append(";\n")
            }
        }
    }

    fun appendLayerWriters(output: StringBuilder, disabledLayers: BitSet?) {
        for (index in layers.indices) {
            val defRR = "defRR$index"
            output.append("float $defRR = random(0.001 * gl_FragCoord.xy + vec2($index.0,defRRT))-0.5;\n")
            val layer = layers[index]
            if (disabledLayers == null || !disabledLayers[layer.texIndex]) {
                layer.appendLayer(output, defRR)
            }
        }
        for ((index, name, map) in emptySlots) {
            if (disabledLayers == null || !disabledLayers[index]) {
                val value = when (map.length) {
                    1 -> " = finalAlpha;\n"
                    2 -> " = vec2(0.0,finalAlpha);\n"
                    else -> " = vec3(0.0,0.0,finalAlpha);\n"
                }
                output.append(name).append('.').append(map).append(value)
            }
        }
    }

    fun findLayer(type: DeferredLayerType): Layer? {
        return layers.firstOrNull2 { it.type == type }
    }

    fun zw(type: DeferredLayerType): Boolean {
        val layer = layers.first2 { it.type == type }
        if (layer.mapping.length != 2) throw IllegalStateException("layer is not 2d")
        return layer.mapping == "zw"
    }

    fun findMapping(type: DeferredLayerType): String? {
        return findLayer(type)?.mapping
    }

    fun findTexture(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = layers.firstOrNull2 { it.type == type } ?: return null
        return findTexture(buffer, layer)
    }

    fun findTextureMS(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = layers.firstOrNull2 { it.type == type } ?: return null
        return findTextureMS(buffer, layer)
    }

    fun findTexture(buffer: IFramebuffer, layer: Layer?): ITexture2D? {
        if (layer == null) return null
        return buffer.getTextureI(layer.texIndex)
    }

    fun findTextureMS(buffer: IFramebuffer, layer: Layer?): ITexture2D? {
        if (layer == null) return null
        return buffer.getTextureIMS(layer.texIndex)
    }

    fun split(index: Int, splitSize: Int): DeferredSettings {
        val index0 = index * splitSize
        val index1 = index0 + splitSize
        return DeferredSettings(
            layerTypes.filter { type ->
                findLayer(type)!!.texIndex in index0 until index1
            }
        )
    }

    companion object {
        val singleToVector = mapOf(
            "r" to Vector4f(1f, 0f, 0f, 0f),
            "g" to Vector4f(0f, 1f, 0f, 0f),
            "b" to Vector4f(0f, 0f, 1f, 0f),
            "a" to Vector4f(0f, 0f, 0f, 1f),
            "x" to Vector4f(1f, 0f, 0f, 0f),
            "y" to Vector4f(0f, 1f, 0f, 0f),
            "z" to Vector4f(0f, 0f, 1f, 0f),
            "w" to Vector4f(0f, 0f, 0f, 1f)
        )
    }
}