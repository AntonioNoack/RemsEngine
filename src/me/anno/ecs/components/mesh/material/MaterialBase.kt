package me.anno.ecs.components.mesh.material

import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.CullMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import org.joml.Vector4f

open class MaterialBase : PrefabSaveable(), Renderable {

    // to do most properties here could be defined by the shader, not this class
    // to do we then somehow would need to display them dynamically

    @Docs("Whether linear filtering shall be applied to textures. If you need to mix, bind those textures yourself.")
    @SerializedProperty
    var linearFiltering = true

    @Docs("How UVs outside the standard square are handled")
    @SerializedProperty
    var clamping = Clamping.REPEAT

    @Docs("Uniforms to be overridden in the shader when applying this material")
    @Type("Map<String,TypeValue>")
    @NotSerializedProperty // cannot really be overridden yet
    var shaderOverrides = HashMap<String, TypeValue>()
        set(value) {
            if (field !== value) {
                field.clear()
                field.putAll(value)
            }
        }

    @Docs("Decides when/how a mesh will be rendered. Most meshes should be rendered opaque")
    @SerializedProperty
    var pipelineStage: PipelineStage = PipelineStage.OPAQUE

    @Docs("Shader override, by default is ECSShaderLib.pbrModelShader")
    @Type("BaseShader?")
    @NotSerializedProperty
    var shader: BaseShader? = null

    @Docs("Most meshes need to be seen from the outside only (FRONT), some need both (BOTH). Some may be reversed (BACK).")
    var cullMode = CullMode.FRONT

    val isDoubleSided get() = cullMode == CullMode.BOTH

    // (useful for Synty meshes, which sometimes have awkward vertex colors)
    @Docs("Whether vertex colors shall be used.")
    @SerializedProperty
    var enableVertexColors = true

    operator fun set(name: String, type: GLSLType, value: Any) {
        shaderOverrides[name] = TypeValue(type, value)
    }

    operator fun set(name: String, value: TypeValue) {
        shaderOverrides[name] = value
    }

    open fun bind(shader: GPUShader) {

        // all the data, the shader needs to know from the material
        shader.v1f("lodBias", Materials.lodBias)
        shader.v2f("jitterInPixels", Materials.jitterInPixels)

        // GFX.check()

        if (shaderOverrides.isNotEmpty()) {
            for ((uniformName, valueType) in shaderOverrides) {
                valueType.bind(shader, uniformName)
            }
        }

        // GFX.check()
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        val mesh = Pipeline.sampleMesh
        val stage = pipeline.findStage(this)
        mesh.materials = listOf(root.ref)
        stage.add(Pipeline.sampleMeshComponent, mesh, transform, this, 0)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (!setSerializableProperty(name, value)) {
            super.setProperty(name, value)
        }
    }

    open fun listTextures(): List<FileReference> = emptyList()
    open fun listTiling(): List<Vector4f> = emptyList()

    @Type("Tiling")
    @Docs("Change all tiling values at once")
    @NotSerializedProperty
    var tiling: Vector4f
        get() = listTiling().firstOrNull() ?: Vector4f(1f, 1f, 0f, 0f)
        set(value) {
            for (dst in listTiling()) {
                dst.set(value)
            }
        }

    override fun hashCode(): Int {
        // only hash common properties?
        var result = shaderOverrides.hashCode()
        result = 31 * result + linearFiltering.hashCode()
        result = 31 * result + clamping.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // if you have a customized Material class, you must implement your own equals function
        if (this::class != MaterialBase::class || other !is MaterialBase ||
            other::class != MaterialBase::class
        ) return false

        return equalProperties(other)
    }

    open fun equalProperties(other: MaterialBase): Boolean {
        if (pipelineStage != other.pipelineStage) return false
        if (shader !== other.shader) return false
        if (shaderOverrides != other.shaderOverrides) return false
        if (linearFiltering != other.linearFiltering) return false
        if (clamping != other.clamping) return false

        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is MaterialBase) return
        dst.shaderOverrides.clear()
        dst.shaderOverrides.putAll(shaderOverrides)
        dst.shader = shader
        dst.pipelineStage = pipelineStage
        dst.cullMode = cullMode
        dst.linearFiltering = linearFiltering
        dst.clamping = clamping
    }

    fun noVertexColors(): MaterialBase {
        enableVertexColors = false
        return this
    }

    companion object {

        fun getTex(image: FileReference): ITexture2D? =
            TextureCache[image, TextureCache.timeoutMillis].value?.createdOrNull()

        fun bindTexture(shader: Shader, name: String, file: FileReference, default: ITexture2D): ITexture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index)
                tex
            } else null
        }

        fun bindTexture(
            shader: GPUShader, name: String,
            file: FileReference, default: ITexture2D,
            filtering: Filtering, clamping: Clamping
        ): ITexture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                bindTexture(tex ?: default, filtering, clamping, index)
                tex
            } else null
        }

        fun bindTexture(tex: ITexture2D, filtering: Filtering, clamping: Clamping, index: Int) {
            val needsMipmap = filtering.needsMipmap && (tex is Texture2D && !tex.hasMipmap)
            // creating mipmaps can be really expensive, so only do it when we have the budget available
            val filtering2 = if (needsMipmap && !Texture2D.requestBudget(tex.width * tex.height)) {
                filtering.withoutMipmap
            } else filtering
            tex.bind(index, filtering2, clamping)
        }
    }
}