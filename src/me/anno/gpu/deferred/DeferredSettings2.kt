package me.anno.gpu.deferred

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.Shader
import kotlin.math.max

class DeferredSettings2(
    val layerTypes: List<DeferredLayerType>,
    fpLights: Boolean
) {

    class Layer(val type: DeferredLayerType, val textureName: String, val mapping: String)

    val layers = ArrayList<Layer>()
    val layers2 = ArrayList<DeferredLayer>()

    init {

        val maxTextures = 16
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

        for (i in 0 until usedTextures0) {
            val layer2 = DeferredLayer(
                "vec4", "defLayer$i",
                if (needsHighPrecision[i]) TargetType.FloatTarget4
                else TargetType.UByteTarget4
            )
            layers2.add(layer2)
        }

    }

    val settingsV1 = DeferredSettings(layers2, fpLights)

    fun createBaseBuffer() = DeferredBuffers.getBaseBuffer(settingsV1)
    fun createLightBuffer() = DeferredBuffers.getLightBuffer(settingsV1)

    // packing and unpacking procedures:
    // assign ids and mappings to all layers
    val glslFragmentShaderEnd = layers.joinToString("") { layer ->
        "${layer.textureName}.${layer.mapping} = ${layer.type.glslName};\n"
    }

    val glslPostProcessingStart = layers.joinToString("") { layer ->
        "${glslTypes[layer.type.dimensions-1]} ${layer.type.glslName} = texture2d(uv, ${layer.textureName}).${layer.mapping};\n"
    }

    // todo for each shader define default values for the deferred layer values (color, normal, ...), if they are not defined

    fun createShader(
        shaderName: String,
        vertexSource: String,
        varyingSource: String,
        fragmentSource: String,
        textures: List<String>
    ): Shader {
        // what do we do, if the position is missing? we cannot do anything...
        val vertex = vertexSource + ""
        val varying = varyingSource + ""
        val fragment = fragmentSource.substring(0, fragmentSource.lastIndexOf('}')) +
                glslFragmentShaderEnd + "}"
        val shader = Shader(shaderName, vertex, varying, fragment)
        shader.setTextureIndices(textures)
        return shader
    }

    fun createPostProcessingShader(
        shaderName: String,
        vertexSource: String,
        varyingSource: String,
        fragmentSource: String,
        textures: List<String>?
    ): Shader {
        val vertex = vertexSource + ""
        val varying = varyingSource + ""
        val index = fragmentSource.indexOf("main")
        val index2 = fragmentSource.indexOf('{', index + 4)
        val fragment = fragmentSource.substring(0, index2) +
                glslFragmentShaderEnd +
                fragmentSource.substring(index2)
        val shader = Shader(shaderName, vertex, varying, fragment)
        shader.setTextureIndices(textures)
        return shader
    }

    companion object {
        val glslTypes = listOf("float", "vec2", "vec3", "vec4")
    }

}