package me.anno.gpu.deferred

import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.first2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Vector4f
import kotlin.math.max

data class DeferredSettings(val layerTypes: List<DeferredLayerType>) {

    val semanticLayers: List<SemanticLayer>
    val storageLayers: List<DeferredLayer>
    val emptySlots: List<EmptySlot>

    var isSRGBMask = 0

    init {

        semanticLayers = ArrayList()

        val maxTextures = layerTypes.size
        val layerRemaining = IntArray(maxTextures)
        layerRemaining.fill(4)

        val layerQualities = createArrayList(maxTextures) { BufferQuality.UINT_8 }
        var usedTextures0 = -1

        fun addType(layerType: DeferredLayerType) {
            val dimensions = layerType.dataDims
            val layerIndex = layerRemaining.indices.indexOfFirst {
                val remaining = layerRemaining[it]
                remaining == 4 || (remaining >= dimensions && layerQualities[it].isCompatibleWith(layerType.minimumQuality))
            }
            val remaining = layerRemaining[layerIndex]
            val startIndex = 4 - remaining
            val mapping = "rgba".substring(startIndex, startIndex + dimensions)
            val semanticLayer = SemanticLayer(layerType, "defLayer$layerIndex", layerIndex, mapping)
            when (layerType) { // mark this layer as sRGB
                DeferredLayerType.COLOR, DeferredLayerType.EMISSIVE, DeferredLayerType.COLOR_EMISSIVE -> {
                    isSRGBMask = isSRGBMask or (1 shl layerIndex)
                }
            }
            semanticLayers.add(semanticLayer)
            layerRemaining[layerIndex] -= dimensions
            usedTextures0 = max(usedTextures0, layerIndex)
            layerQualities[layerIndex] =
                if (remaining == 4) layerType.minimumQuality
                else layerQualities[layerIndex].combineWith(layerType.minimumQuality)!!
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
        emptySlots = ArrayList(usedTextures0)

        for (layerIndex in 0 until usedTextures0) {
            val empty = layerRemaining[layerIndex]
            val layer2 = DeferredLayer(
                "defLayer$layerIndex", when (layerQualities[layerIndex]) {
                    BufferQuality.UINT_8 -> TargetType.UInt8xI
                    BufferQuality.UINT_16 -> TargetType.UInt16xI
                    BufferQuality.FP_16 -> TargetType.Float16xI
                    BufferQuality.DEPTH_U32, BufferQuality.FP_32 -> TargetType.Float32xI
                }[3 - empty]
            )
            storageLayers.add(layer2)
            if (empty > 0) {
                val mask = when (layerRemaining[layerIndex]) {
                    1 -> "a"
                    2 -> "ba"
                    else -> "gba"
                }
                emptySlots.add(EmptySlot(layerIndex, layer2.name, mask))
            }
        }
    }

    val targetTypes = storageLayers.map { it.type }

    fun createBaseBuffer(name: String, samples: Int): IFramebuffer {
        val depthBufferType = if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
        val fb = IFramebuffer.createFramebuffer(name, 1, 1, samples, targetTypes, depthBufferType)
        fb.isSRGBMask = isSRGBMask
        return fb
    }

    fun getBaseBufferFBStack(name: String, width: Int, height: Int, samples: Int): IFramebuffer {
        val depthBufferType = if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE else DepthBufferType.INTERNAL
        val fb = FBStack[name, width, height, targetTypes, samples, depthBufferType]
        fb.isSRGBMask = isSRGBMask
        return fb
    }

    fun createShader(
        shaderName: String,
        key: BaseShader.ShaderKey,
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
        val shader = builder.create(key, "def")
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

    fun appendLayerWriters(output: StringBuilder, disabledLayers: BooleanArrayList?, useRandomness: Boolean) {
        for (index in semanticLayers.indices) {
            val defRR = if (useRandomness) "defRR$index" else null
            if (useRandomness) {
                output.append("float ").append(defRR)
                    .append(" = random(0.001 * gl_FragCoord.xy + vec2(")
                    .append(index).append(".0,defRRT))-0.5;\n")
            }
            val layer = semanticLayers[index]
            if (disabledLayers == null || !disabledLayers[layer.texIndex]) {
                layer.appendLayer(output, defRR, useRandomness)
            }
        }
        for (slot in emptySlots) {
            if (disabledLayers == null || !disabledLayers[slot.index]) {
                val value = when (slot.mask.length) {
                    1 -> " = 1.0;\n"
                    2 -> " = vec2(0.0,1.0);\n"
                    else -> " = vec3(0.0,0.0,1.0);\n"
                }
                output.append(slot.name).append('.').append(slot.mask).append(value)
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

    fun findTexture(buffer: IFramebuffer, type: DeferredLayerType): ITexture2D? {
        val layer = semanticLayers.firstOrNull2 { it.type == type } ?: return null
        return findTexture(buffer, layer)
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
}