package me.anno.ecs.components.mesh.material

import me.anno.ecs.components.mesh.material.shaders.DecalShader
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.GPUShader
import me.anno.utils.types.Booleans.hasFlag
import me.anno.maths.Maths.min
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f

// decal pass:
//  Input: pos, normal (we could pass in color theoretically, but idk)
//  Output: new color, new normal, new emissive
// these attributes are mixed in deferred layers, so we probably need to do it in the shader... could be expensive,
// because we need a copy on some platforms to operate on
// todo different blend modes: additive, subtractive, default, ...
class DecalMaterial : Material() {

    companion object {
        private val shaderLib = HashMap<Int, ECSMeshShader>()
    }

    // can we support this in forward rendering?
    // yes, but it will be a bit more expensive
    // a) list of all decals for all pixels -> bad
    // b) render normal extra; and apply lighting twice

    var writeColor = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeNormal = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeEmissive = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeRoughness = false
        set(value) {
            field = value
            shader = getShader()
        }

    var writeMetallic = false
        set(value) {
            field = value
            shader = getShader()
        }

    /**
     * fading sharpness of decal on edges, by local xyz, and normal
     * */
    var decalSharpness = Vector4f(5f)
        set(value) {
            field.set(value)
        }

    init {
        pipelineStage = PipelineStage.DECAL
        shader = getShader()
    }

    override fun bind(shader: GPUShader) {
        super.bind(shader)
        bindDepthUniforms(shader)
        // bind textures
        val buffer = GFXState.currentBuffer
        val sett = GFXState.currentRenderer.deferredSettings
        if (sett != null) {
            val layers = sett.storageLayers
            for (index in 0 until min(layers.size, buffer.numTextures)) {
                buffer.getTextureI(index).bindTrulyNearest(shader, layers[index].nameIn0)
            }
        }
        shader.v4f("decalSharpness", decalSharpness)
        shader.v2f("windowSize", buffer.width.toFloat(), buffer.height.toFloat())
        buffer.depthTexture?.bindTrulyNearest(shader, "depth_in0")
    }

    fun getShader(): ECSMeshShader {
        val flags = writeColor.toInt() + writeNormal.toInt(2) +
                writeEmissive.toInt(4) + writeRoughness.toInt(8) + writeMetallic.toInt(16)
        return shaderLib.getOrPut(flags) {
            val layers = ArrayList<DeferredLayerType>(5)
            if (flags.hasFlag(1)) layers.add(DeferredLayerType.COLOR)
            if (flags.hasFlag(2)) layers.add(DeferredLayerType.NORMAL)
            if (flags.hasFlag(4)) layers.add(DeferredLayerType.EMISSIVE)
            if (flags.hasFlag(8)) layers.add(DeferredLayerType.ROUGHNESS)
            if (flags.hasFlag(16)) layers.add(DeferredLayerType.METALLIC)
            DecalShader(layers)
        }
    }
}