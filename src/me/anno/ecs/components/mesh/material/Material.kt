package me.anno.ecs.components.mesh.material

import me.anno.ecs.Transform
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
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
import me.anno.gpu.texture.TextureLib
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

// todo MatCap material (I imagine they could be really useful for mobile)
//  https://learn.foundry.com/modo/content/help/pages/shading_lighting/shader_items/matcap.html
//  https://github.com/nidorx/matcaps
open class Material : PrefabSaveable(), Renderable {

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

    @Docs("Color and transparency as far as it's supported")
    @Range(0.0, 1.0)
    @Type("Color4")
    var diffuseBase = Vector4f(1f)
        set(value) {
            field.set(value)
        }

    @Docs("Diffuse Texture")
    @Type("Texture/Reference")
    var diffuseMap: FileReference = InvalidRef

    @Docs("Strength for normal map. Negative values flip the map inside out.")
    @Range(-100.0, 100.0)
    var normalStrength = 1f

    @Docs("Typically cyan colored with patterns; bump maps (grayscale height) work, too")
    @Type("Texture/Reference")
    var normalMap: FileReference = InvalidRef

    // translucency:
    // only little light directionality
    @Docs("Internal diffuse refractions. The higher, the less the material cares about light directions.")
    @Range(0.0, 1.0)
    var translucency = 0f

    @Docs("Most meshes need to be seen from the outside only (FRONT), some need both (BOTH). Some may be reversed (BACK).")
    var cullMode = CullMode.FRONT
    val isDoubleSided get() = cullMode == CullMode.BOTH

    // base * map
    @Docs("How much light is emitted by the surface")
    @Range(0.0, 100.0)
    @Type("Color3HDR")
    var emissiveBase = Vector3f(0f)
        set(value) {
            field.set(value)
        }

    @Docs("How much light is emitted by the surface, texture")
    @Type("Texture/Reference")
    var emissiveMap: FileReference = InvalidRef

    // mix(min,max,value(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var roughnessMinMax = Vector2f(0f, 1f)

    @Docs("Texture for roughness. Black gets mapped to roughnessMinMax.x, white to roughnessMinMax.y.")
    @Type("Texture/Reference")
    var roughnessMap: FileReference = InvalidRef

    // mix(min,max,map(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var metallicMinMax = Vector2f(0f, 0f)

    @Docs("Texture for metallic. Black gets mapped to metallicMinMax.x, white to metallicMinMax.y.")
    @Type("Texture/Reference")
    var metallicMap: FileReference = InvalidRef

    @Docs("Future bump map for maybe tesselation, maybe parallax mapping; not yet supported.")
    @Type("Texture/Reference")
    var displacementMap: FileReference = InvalidRef

    @Docs("Constant factor for baked ambient occlusion: light *= 1-(1-occlusion) * strength")
    @Range(0.0, 100.0)
    var occlusionStrength = 1f

    @Docs("UV-based factor for baked ambient occlusion: light *= 1-(1-occlusion) * strength")
    @Type("Texture/Reference")
    var occlusionMap: FileReference = InvalidRef

    // the last component is the strength
    // clear coat is a thin coating over the material,
    // which typically has a single color,
    // and is only seen at steep angles
    // this is typically seen on cars
    // if you don't like the monotonicity, you can add your own fresnel effect in the shader
    @Type("Color3")
    var clearCoatColor = Vector3f(1f, 1f, 1f)
        set(value) {
            field.set(value)
        }

    @Range(0.0, 1.0)
    var clearCoatStrength = 0f

    @Range(0.0, 1.0)
    var clearCoatMetallic = 1f

    @Range(0.0, 1.0)
    var clearCoatRoughness = 0f


    // adds soft diffuse light capture on steep angles,
    // can be used for clothing, where the fibers catch the light
    @Range(0.0, 1.0)
    var sheen = 0f

    // e.g. for patterns, e.g. stroking over some clothes leaves patterns
    @Type("Texture/Reference")
    var sheenNormalMap: FileReference = InvalidRef

    @Docs("Defines refraction/reflection strength for transparent materials")
    @Range(1.0, 5.0)
    var indexOfRefraction = 1.5f

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
        GFX.check()

        val white = TextureLib.whiteTexture
        val n001 = TextureLib.normalTexture

        val f = if (linearFiltering) Filtering.LINEAR else Filtering.NEAREST
        val c = clamping
        bindTexture(shader, "occlusionMap", occlusionMap, white, f, c)
        bindTexture(shader, "metallicMap", metallicMap, white, f, c)
        bindTexture(shader, "roughnessMap", roughnessMap, white, f, c)
        bindTexture(shader, "emissiveMap", emissiveMap, white, f, c)
        val sheenNormalTex = bindTexture(shader, "sheenNormalMap", sheenNormalMap, white, f, c)
        val normalTex = bindTexture(shader, "normalMap", normalMap, n001, f, c)
        bindTexture(shader, "diffuseMap", diffuseMap, white, f, c)

        GFX.check()

        shader.v4f("diffuseBase", diffuseBase)
        shader.v2f(
            "normalStrength",
            if (normalTex == null) 0f else normalStrength,
            if (sheenNormalTex == null) 0f else 1f
        )
        shader.v3f("emissiveBase", emissiveBase)
        shader.v2f("roughnessMinMax", roughnessMinMax)
        shader.v2f("metallicMinMax", metallicMinMax)
        shader.v1f("occlusionStrength", occlusionStrength)
        shader.v1f("finalTranslucency", translucency)
        shader.v1f("finalSheen", sheen)
        shader.v1f("sheen", sheen)
        shader.v1f("IOR", indexOfRefraction)
        shader.v1f("lodBias", lodBias)

        GFX.check()

        if (clearCoatStrength > 0f) {
            shader.v4f("clearCoat", clearCoatColor, clearCoatStrength)
            shader.v2f("clearCoatRoughMetallic", clearCoatRoughness, clearCoatMetallic)
        } else {
            shader.v4f("clearCoat", 0f)
        }

        if (shaderOverrides.isNotEmpty()) {
            for ((uniformName, valueType) in shaderOverrides) {
                valueType.bind(shader, uniformName)
            }
        }

        GFX.check()
    }

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        val mesh = Pipeline.sampleMesh
        val stage = pipeline.findStage(this)
        val materialSource = root.ref
        mesh.material = materialSource
        stage.add(Pipeline.sampleMeshComponent, mesh, transform, this, 0)
        return clickId + 1
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

    open fun listTextures() = listOf(
        diffuseMap,
        emissiveMap,
        normalMap,
        roughnessMap,
        metallicMap,
        occlusionMap,
        displacementMap,
    )

    override fun hashCode(): Int {
        var result = shaderOverrides.hashCode()
        result = 31 * result + diffuseBase.hashCode()
        result = 31 * result + diffuseMap.hashCode()
        result = 31 * result + normalStrength.hashCode()
        result = 31 * result + normalMap.hashCode()
        result = 31 * result + emissiveBase.hashCode()
        result = 31 * result + emissiveMap.hashCode()
        result = 31 * result + roughnessMinMax.hashCode()
        result = 31 * result + roughnessMap.hashCode()
        result = 31 * result + metallicMinMax.hashCode()
        result = 31 * result + metallicMap.hashCode()
        result = 31 * result + displacementMap.hashCode()
        result = 31 * result + occlusionStrength.hashCode()
        result = 31 * result + occlusionMap.hashCode()
        result = 31 * result + translucency.hashCode()
        result = 31 * result + linearFiltering.hashCode()
        result = 31 * result + clamping.hashCode()
        return result
    }

    @Suppress("RedundantIf")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Material) return false

        if (pipelineStage != other.pipelineStage) return false
        if (shader !== other.shader) return false
        if (diffuseBase != other.diffuseBase) return false
        if (diffuseMap != other.diffuseMap) return false
        if (normalStrength != other.normalStrength) return false
        if (normalMap != other.normalMap) return false
        if (emissiveBase != other.emissiveBase) return false
        if (emissiveMap != other.emissiveMap) return false
        if (roughnessMinMax != other.roughnessMinMax) return false
        if (roughnessMap != other.roughnessMap) return false
        if (metallicMinMax != other.metallicMinMax) return false
        if (metallicMap != other.metallicMap) return false
        if (displacementMap != other.displacementMap) return false
        if (occlusionStrength != other.occlusionStrength) return false
        if (occlusionMap != other.occlusionMap) return false
        if (translucency != other.translucency) return false
        if (shaderOverrides != other.shaderOverrides) return false
        if (linearFiltering != other.linearFiltering) return false
        if (clamping != other.clamping) return false

        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Material
        dst.diffuseBase.set(diffuseBase)
        dst.diffuseMap = diffuseMap
        dst.emissiveBase.set(emissiveBase)
        dst.emissiveMap = emissiveMap
        dst.normalMap = normalMap
        dst.normalStrength = normalStrength
        dst.sheen = sheen
        dst.sheenNormalMap = sheenNormalMap
        dst.occlusionStrength = occlusionStrength
        dst.occlusionMap = occlusionMap
        dst.shaderOverrides.clear()
        dst.shaderOverrides.putAll(shaderOverrides)
        dst.roughnessMap = roughnessMap
        dst.roughnessMinMax.set(roughnessMinMax)
        dst.metallicMap = metallicMap
        dst.metallicMinMax.set(metallicMinMax)
        dst.clearCoatColor.set(clearCoatColor)
        dst.clearCoatStrength = clearCoatStrength
        dst.clearCoatRoughness = clearCoatRoughness
        dst.clearCoatMetallic = clearCoatMetallic
        dst.shader = shader
        dst.pipelineStage = pipelineStage
        dst.cullMode = cullMode
        dst.linearFiltering = linearFiltering
        dst.clamping = clamping
    }

    companion object {

        // what is a good timeout???
        var timeout = 30000L

        var lodBias = 0f

        val defaultMaterial = Material()

        fun getTex(image: FileReference): ITexture2D? = TextureCache[image, timeout, true]

        fun bindTexture(shader: Shader, name: String, file: FileReference, default: Texture2D): ITexture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index)
                tex
            } else null
        }

        fun bindTexture(
            shader: GPUShader,
            name: String,
            file: FileReference,
            default: Texture2D,
            filtering: Filtering,
            clamping: Clamping
        ): ITexture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index, filtering, clamping)
                tex
            } else null
        }

        fun diffuse(color: Int): Material {
            val mat = Material()
            color.toVecRGBA(mat.diffuseBase)
            return mat
        }
    }
}