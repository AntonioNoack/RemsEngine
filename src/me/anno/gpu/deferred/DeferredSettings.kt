package me.anno.gpu.deferred

import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.first2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector4f
import kotlin.math.max

data class DeferredSettings(val layerTypes: List<DeferredLayerType>) {

    // todo shader to blit depth with different resolutions/formats

    data class SemanticLayer(val type: DeferredLayerType, val textureName: String, val texIndex: Int, val mapping: String) {

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
                    fragment.append(", ivec2(vec2(textureSize(").append(texName)
                    fragment.append("))*").append(uv)
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

        fun appendLayer(output: StringBuilder, defRR: String?, useRandomness: Boolean) {
            output.append(textureName)
            output.append('.')
            output.append(mapping)
            val useRandomRounding = useRandomness && when (type) {
                DeferredLayerType.CLICK_ID, DeferredLayerType.GROUP_ID -> false
                else -> true
            }
            output.append(if (useRandomRounding) " = (" else " = ")
            if (type == DeferredLayerType.DEPTH) {
                val depthVariableName = if ("gl_FragDepth" in output) "gl_FragDepth" else "gl_FragCoord.z"
                output.append(depthVariableName)
            } else {
                val w2d = type.workToData
                when {
                    '.' in w2d -> output.append(w2d)
                    w2d.isNotEmpty() -> output.append(w2d).append('(').append(type.glslName).append(')')
                    else -> output.append(type.glslName)
                }
            }
            // append random rounding
            if (useRandomRounding) {
                output.append(")*(1.0+").append(defRR).append("*").append(textureName).append("RR.x")
                output.append(")+").append(defRR).append("*").append(textureName).append("RR.y")
            }
            output.append(";\n")
        }
    }

    val semanticLayers = ArrayList<SemanticLayer>()
    val storageLayers: ArrayList<DeferredLayer>
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
            val semanticLayer = SemanticLayer(layerType, "defLayer$layerIndex", layerIndex, mapping)
            semanticLayers.add(semanticLayer)
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

        storageLayers = ArrayList(usedTextures0)
        for (layerIndex in 0 until usedTextures0) {
            val layer2 = DeferredLayer(
                "defLayer$layerIndex", when (needsHighPrecision[layerIndex]) {
                    BufferQuality.LOW_8 -> TargetType.UInt8x4
                    BufferQuality.MEDIUM_12 -> TargetType.Normal12Target4
                    BufferQuality.HIGH_16 -> TargetType.Float16x4
                    else -> TargetType.Float32x4
                }
            )
            storageLayers.add(layer2)
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

    val targetTypes = storageLayers.map { it.type }

    fun createBaseBuffer(name: String, samples: Int): IFramebuffer {
        val layers = storageLayers
        val depthBufferType = if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE
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
        vertexPostProcessing: List<ShaderStage>,
        pixelPostProcessing: List<ShaderStage>,
        ditherMode: DitherMode
    ): Shader {
        val vertex = if (instanced) "#define INSTANCED;\n$vertexShader" else vertexShader
        val builder = ShaderBuilder(shaderName, this, ditherMode)
        builder.addVertex(ShaderStage("def-vs", vertexVariables + varyings, vertex))
        builder.addVertex(vertexPostProcessing)
        builder.addFragment(ShaderStage("def-fs", fragmentVariables + varyings, fragmentShader))
        builder.addFragment(pixelPostProcessing)
        val shader = builder.create(null)
        shader.setTextureIndices(textures)
        return shader
    }

    fun appendLayerDeclarators(disabledLayers: BooleanArrayList?, uniforms: HashSet<Variable>, useRandomness: Boolean) {
        val layers = storageLayers
        if (useRandomness) {
            uniforms.add(Variable(GLSLType.V1F, "defRRT"))
        }
        for (index in layers.indices) {
            if (disabledLayers == null || !disabledLayers[index]) {
                val type = layers[index]
                val outVariable = Variable(GLSLType.V4F, type.name, VariableMode.OUT)
                outVariable.slot = index
                uniforms.add(outVariable)
                if (useRandomness) {
                    uniforms.add(Variable(GLSLType.V2F, type.nameRR))
                }
            }
        }
    }

    fun appendLayerWriters(
        output: StringBuilder, disabledLayers: BooleanArrayList?,
        useRandomness: Boolean, defined: Set<Variable>
    ) {
        for (index in semanticLayers.indices) {
            val defRR = if (useRandomness) "defRR$index" else null
            if (useRandomness) {
                output.append("float $defRR = random(0.001 * gl_FragCoord.xy + vec2($index.0,defRRT))-0.5;\n")
            }
            val layer = semanticLayers[index]
            if (disabledLayers == null || !disabledLayers[layer.texIndex]) {
                layer.appendLayer(output, defRR, useRandomness)
            }
        }
        val hasAlpha = Variable(GLSLType.V1F, "finalAlpha") in defined
        for ((index, name, map) in emptySlots) {
            if (disabledLayers == null || !disabledLayers[index]) {
                val value = if (hasAlpha) {
                    when (map.length) {
                        1 -> " = finalAlpha;\n"
                        2 -> " = vec2(0.0,finalAlpha);\n"
                        else -> " = vec3(0.0,0.0,finalAlpha);\n"
                    }
                } else when (map.length) {
                    1 -> " = 1.0;\n"
                    2 -> " = vec2(0.0,1.0);\n"
                    else -> " = vec3(0.0,0.0,1.0);\n"
                }
                output.append(name).append('.').append(map).append(value)
            }
        }
    }

    fun findLayer(type: DeferredLayerType): SemanticLayer? {
        return semanticLayers.firstOrNull2 { it.type == type }
    }

    fun zw(type: DeferredLayerType): Boolean {
        val layer = semanticLayers.first2 { it.type == type }
        if (layer.mapping.length != 2) throw IllegalStateException("layer is not 2d")
        return layer.mapping == "zw"
    }

    fun findMapping(type: DeferredLayerType): String? {
        return findLayer(type)?.mapping
    }

    fun findTexture(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = semanticLayers.firstOrNull2 { it.type == type } ?: return null
        return findTexture(buffer, layer)
    }

    fun findTextureMS(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = semanticLayers.firstOrNull2 { it.type == type } ?: return null
        return findTextureMS(buffer, layer)
    }

    fun findTexture(buffer: IFramebuffer, semanticLayer: SemanticLayer?): ITexture2D? {
        if (semanticLayer == null) return null
        return buffer.getTextureI(semanticLayer.texIndex)
    }

    fun findTextureMS(buffer: IFramebuffer, semanticLayer: SemanticLayer?): ITexture2D? {
        if (semanticLayer == null) return null
        return buffer.getTextureIMS(semanticLayer.texIndex)
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
        val singleToVectorR = Vector4f(1f, 0f, 0f, 0f)
        val singleToVector = run {
            val x = singleToVectorR
            val y = Vector4f(0f, 1f, 0f, 0f)
            val z = Vector4f(0f, 0f, 1f, 0f)
            val w = Vector4f(0f, 0f, 0f, 1f)
            mapOf(
                "r" to x, "g" to y, "b" to z, "a" to w,
                "x" to x, "y" to y, "z" to z, "w" to w
            )
        }
    }
}