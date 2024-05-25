package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.EditorField
import me.anno.ecs.components.mesh.material.shaders.DecalShader
import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.FLAG_COLOR
import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.FLAG_EMISSIVE
import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.FLAG_METALLIC
import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.FLAG_NORMAL
import me.anno.ecs.components.mesh.material.shaders.DecalShader.Companion.FLAG_ROUGHNESS
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.GFXState
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.GPUShader
import me.anno.maths.Maths.min
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Booleans.withFlag
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

// todo different blend modes: additive, subtractive, default, ...
// todo make this work for (limited) forward rendering: all properties get replaced
class DecalMaterial : Material() {

    companion object {
        private val LOGGER = LogManager.getLogger(DecalMaterial::class)
    }

    @SerializedProperty
    private var writeFlags = FLAG_COLOR

    @EditorField
    @NotSerializedProperty
    var writeColor: Boolean
        get() = writeFlags.hasFlag(FLAG_COLOR)
        set(value) {
            writeFlags = writeFlags.withFlag(FLAG_COLOR, value)
            shader = getShader()
        }

    @EditorField
    @NotSerializedProperty
    var writeNormal: Boolean
        get() = writeFlags.hasFlag(FLAG_NORMAL)
        set(value) {
            writeFlags = writeFlags.withFlag(FLAG_NORMAL, value)
            shader = getShader()
        }

    @EditorField
    @NotSerializedProperty
    var writeEmissive: Boolean
        get() = writeFlags.hasFlag(FLAG_EMISSIVE)
        set(value) {
            writeFlags = writeFlags.withFlag(FLAG_EMISSIVE, value)
            shader = getShader()
        }

    @EditorField
    @NotSerializedProperty
    var writeRoughness: Boolean
        get() = writeFlags.hasFlag(FLAG_ROUGHNESS)
        set(value) {
            writeFlags = writeFlags.withFlag(FLAG_ROUGHNESS, value)
            shader = getShader()
        }

    @EditorField
    @NotSerializedProperty
    var writeMetallic: Boolean
        get() = writeFlags.hasFlag(FLAG_METALLIC)
        set(value) {
            writeFlags = writeFlags.withFlag(FLAG_METALLIC, value)
            shader = getShader()
        }

    /**
     * fading sharpness of decal on edges, by local xyz
     * */
    var decalSharpness = Vector3f(5f)
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
        shader.v3f("decalSharpness", decalSharpness)
        // bind textures from the layer below us
        var buffer = DecalShader.srcBuffer
        val layers = GFXState.currentRenderer.deferredSettings?.storageLayers
        if (buffer != null && layers != null) {
            for (index in 0 until min(layers.size, buffer.numTextures)) {
                buffer.getTextureI(index).bindTrulyNearest(shader, layers[index].nameIn0)
            }
        }
        buffer = buffer ?: GFXState.currentBuffer
        shader.v2f("windowSize", buffer.width.toFloat(), buffer.height.toFloat())
        (buffer.depthTexture ?: buffer.getTextureIMS(buffer.numTextures - 1)) // ok so?
            .bindTrulyNearest(shader, "depth_in0")
    }

    private fun getShader(): ECSMeshShader {
        val flags = writeColor.toInt() + writeNormal.toInt(2) +
                writeEmissive.toInt(4) + writeRoughness.toInt(8) + writeMetallic.toInt(16)
        return DecalShader.shaderLib[flags]
    }
}