package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageGPUCache
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

open class Material : PrefabSaveable(), Renderable {

    // to do most properties here could be defined by the shader, not this class
    // to do we then somehow would need to display them dynamically

    @SerializedProperty
    var linearFiltering = true

    @SerializedProperty
    var clamping = Clamping.REPEAT

    @Type("Map<String,TypeValue>")
    @SerializedProperty
    var shaderOverrides = HashMap<String, TypeValue>()
        set(value) {
            if (field !== value) {
                field.clear()
                field.putAll(value)
            }
        }

    // todo create a standard for what which value means
    // 0 = opaque
    // 1 = transparent
    // 2 = decal
    @NotSerializedProperty
    var pipelineStage: Int = 0

    @Type("BaseShader?")
    @NotSerializedProperty
    var shader: BaseShader? = null

    @Range(0.0, 1.0)
    @Type("Color4HDR")
    var diffuseBase = Vector4f(1f)
        set(value) {
            field.set(value)
        }
    var diffuseMap: FileReference = InvalidRef

    @Range(-100.0, 100.0)
    var normalStrength = 1f
    var normalMap: FileReference = InvalidRef

    // translucency:
    // only little light directionality
    @Range(0.0, 1.0)
    var translucency = 0f

    // todo instead back/front/both?
    var isDoubleSided = false

    // base * map
    @Range(0.0, 100.0)
    @Type("Color3HDR")
    var emissiveBase = Vector3f(0f)
        set(value) {
            field.set(value)
        }

    @Type("Texture/Reference")
    var emissiveMap: FileReference = InvalidRef

    // mix(min,max,value(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var roughnessMinMax = Vector2f(0f, 1f)

    @Type("Texture/Reference")
    var roughnessMap: FileReference = InvalidRef

    // mix(min,max,map(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var metallicMinMax = Vector2f(0f, 0f)

    @Type("Texture/Reference")
    var metallicMap: FileReference = InvalidRef

    @Type("Texture/Reference")
    var displacementMap: FileReference = InvalidRef

    // light *= 1-(1-map(uv)) * strength
    @Range(0.0, 100.0)
    var occlusionStrength = 1f

    @Type("Texture/Reference")
    var occlusionMap: FileReference = InvalidRef

    // the last component is the strength
    // clear coat is a thin coating over the material,
    // which typically has a single color,
    // and is only seen at steep angles
    // this is typically seen on cars
    // if you don't like the monotonicity, you can add your own fresnel effect in the shader
    @Type("Color4")
    var clearCoatColor = Vector4f(1f, 1f, 1f, 0f)
        set(value) {
            field.set(value)
        }

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

    // not yet supported
    @Range(1.0, 5.0)
    var indexOfRefraction = 1f

    // todo respect this property
    // (useful for Synty meshes, which sometimes have awkward vertex colors)
    @SerializedProperty
    var enableVertexColors = true

    operator fun set(name: String, type: GLSLType, value: Any) {
        shaderOverrides[name] = TypeValue(type, value)
    }

    operator fun set(name: String, value: TypeValue) {
        shaderOverrides[name] = value
    }

    open fun bind(shader: Shader) {

        // all the data, the shader needs to know from the material

        val white = TextureLib.whiteTexture
        val n001 = TextureLib.normalTexture

        val f = if (linearFiltering) GPUFiltering.LINEAR else GPUFiltering.NEAREST
        val c = clamping
        bindTexture(shader, "occlusionMap", occlusionMap, white, f, c)
        bindTexture(shader, "metallicMap", metallicMap, white, f, c)
        bindTexture(shader, "roughnessMap", roughnessMap, white, f, c)
        bindTexture(shader, "emissiveMap", emissiveMap, white, f, c)
        val sheenNormalTex = bindTexture(shader, "sheenNormalMap", sheenNormalMap, white, f, c)
        val normalTex = bindTexture(shader, "normalMap", normalMap, n001, f, c)
        bindTexture(shader, "diffuseMap", diffuseMap, white, f, c)

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

        if (clearCoatColor.w > 0f) {
            shader.v4f("finalClearCoat", clearCoatColor)
            shader.v2f("finalClearCoatRoughMetallic", clearCoatRoughness, clearCoatMetallic)
        } else {
            shader.v4f("finalClearCoat", 0f)
        }

        if (shaderOverrides.isNotEmpty()) {
            for ((uniformName, valueType) in shaderOverrides) {
                valueType.bind(shader, uniformName)
            }
        }
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        val mesh = Pipeline.sampleMesh
        val stage = pipeline.findStage(this)
        val materialSource = root.ref
        mesh.material = materialSource
        stage.add(Pipeline.sampleMeshComponent, mesh, entity, 0, clickId)
        return clickId
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
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
        dst.clearCoatRoughness = clearCoatRoughness
        dst.clearCoatMetallic = clearCoatMetallic
        dst.shader = shader
        dst.pipelineStage = pipelineStage
        dst.isDoubleSided = isDoubleSided
        dst.linearFiltering = linearFiltering
        dst.clamping = clamping
    }

    override val className get() = "Material"

    companion object {

        var timeout = 1000L

        fun getTex(image: FileReference) = ImageGPUCache[image, timeout, true]

        fun bindTexture(shader: Shader, name: String, file: FileReference, default: Texture2D): Texture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index)
                tex
            } else null
        }

        fun bindTexture(
            shader: Shader,
            name: String,
            file: FileReference,
            default: Texture2D,
            filtering: GPUFiltering,
            clamping: Clamping
        ): Texture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index, filtering, clamping)
                tex
            } else null
        }

    }

}