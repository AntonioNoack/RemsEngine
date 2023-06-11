package me.anno.ecs.components.mesh.decal

import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.shader.DepthTransforms.bindDepthToPosition
import me.anno.gpu.shader.Shader
import me.anno.maths.Maths.hasFlag
import me.anno.utils.types.Booleans.toInt

// decal pass:
//  Input: pos, normal (we could pass in color theoretically, but idk)
//  Output: new color, new normal, new emissive
// todo different blend modes: additive, subtractive, default, ...
class DecalMaterial : Material() {

    companion object {
        private val shaderLib = HashMap<Int, ECSMeshShader>()
        val sett get() = GFXState.currentRenderer.deferredSettings
    }

    // can we support this in forward rendering?
    // yes, but it will be a bit more expensive
    // a) list of all decals for all pixels -> bad
    // b) render normal extra; and apply lighting twice

    var writeColor = true
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

    init {
        pipelineStage = 2
        shader = getShader()
    }

    override fun bind(shader: Shader) {
        super.bind(shader)
        bindDepthToPosition(shader)
        // bind textures
        val buff = GFXState.currentBuffer
        val sett = sett
        if (sett != null) {
            val layers = sett.layers2
            for (index in layers.indices) {
                buff.getTextureI(index).bindTrulyNearest(shader, layers[index].name + "_in0")
            }
        }
        shader.v2f("windowSize", buff.w.toFloat(), buff.h.toFloat())
        buff.depthTexture?.bindTrulyNearest(shader, "depth_in0")
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

    override val className: String get() = "DecalMaterial"

}