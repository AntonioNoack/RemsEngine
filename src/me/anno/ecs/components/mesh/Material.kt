package me.anno.ecs.components.mesh

import me.anno.image.ImageGPUCache
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.LOGGER
import me.anno.utils.OS
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class Material : PrefabSaveable() {

    @SerializedProperty
    var shaderOverrides = HashMap<String, TypeValue>()

    // or not yet...
    @NotSerializedProperty
    var pipelineStage: PipelineStage? = null

    // or not yet...
    @NotSerializedProperty
    var shader: BaseShader? = null

    @Range(0.0, 1.0)
    @Type("Color4HDR")
    var diffuseBase = Vector4f(1f)
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
    var emissiveBase = Vector3f(1f)
    var emissiveMap: FileReference = InvalidRef

    // mix(min,max,value(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var roughnessMinMax = Vector2f(0f, 1f)
    var roughnessMap: FileReference = InvalidRef

    // mix(min,max,map(uv)) or 1 if undefined)
    @Range(0.0, 1.0)
    var metallicMinMax = Vector2f(0f, 0f)
    var metallicMap: FileReference = InvalidRef

    var displacementMap: FileReference = InvalidRef

    // light *= 1-(1-map(uv)) * strength
    @Range(0.0, 100.0)
    var occlusionStrength = 1f
    var occlusionMap: FileReference = InvalidRef

    // the last component is the strength
    // clear coat is a thin coating over the material,
    // which typically has a single color,
    // and is only seen at steep angles
    // this is typically seen on cars
    // if you don't like the monotonicity, you can add your own fresnel effect in the shader
    @Type("Color4")
    var clearCoatColor = Vector4f(1f, 1f, 1f, 0f)

    @Range(0.0, 1.0)
    var clearCoatMetallic = 1f

    @Range(0.0, 1.0)
    var clearCoatRoughness = 0f


    // adds soft diffuse light capture on steep angles,
    // can be used for clothing, where the fibers catch the light
    @Range(0.0, 1.0)
    var sheen = 0f

    // e.g. for patterns, e.g. stroking over some clothes leaves patterns
    var sheenNormalMap: FileReference = InvalidRef

    operator fun set(name: String, type: GLSLType, value: Any) {
        shaderOverrides[name] = TypeValue(type, value)
    }

    operator fun set(name: String, value: TypeValue) {
        shaderOverrides[name] = value
    }

    val timeout = 1000L
    fun getTex(image: FileReference) = ImageGPUCache.getImage(image, timeout, true)

    fun defineShader(shader: Shader) {
        // todo there will be shadow maps: find the correct texture indices!
        // all the data, the shader needs to know from the material

        // todo allow swizzling for alpha, roughness, metallic and such
        val metallicTex = getTex(metallicMap)
        val roughnessTex = getTex(roughnessMap)
        val emissiveTex = getTex(emissiveMap)
        val normalTex = getTex(normalMap)
        val diffuseTex = getTex(diffuseMap)
        val occlusionTex = getTex(occlusionMap)
        val sheenNormalTex = getTex(sheenNormalMap)

        val white = TextureLib.whiteTexture
        val black = TextureLib.blackTexture
        val n001 = TextureLib.normalTexture
        val filtering = GPUFiltering.LINEAR
        val clamping = Clamping.REPEAT

        (sheenNormalTex ?: white).bind(6, filtering, clamping)
        (occlusionTex ?: white).bind(5, filtering, clamping)
        (metallicTex ?: white).bind(4, filtering, clamping)
        (roughnessTex ?: white).bind(3, filtering, clamping)
        (emissiveTex ?: black).bind(2, filtering, clamping)
        (normalTex ?: n001).bind(1, filtering, clamping)
        (diffuseTex ?: white).bind(0, filtering, clamping)

        shader.v4("diffuseBase", diffuseBase)
        shader.v2(
            "normalStrength",
            if (normalTex == null) 0f else normalStrength,
            if (sheenNormalTex == null) 0f else 1f
        )
        shader.v3("emissiveBase", emissiveBase)
        shader.v2("roughnessMinMax", roughnessMinMax)
        shader.v2("metallicMinMax", metallicMinMax)
        shader.v1("occlusionStrength", occlusionStrength)
        shader.v1("finalTranslucency", translucency)
        shader.v1("finalSheen", sheen)
        shader.v1("sheen", sheen)

        if (clearCoatColor.w > 0f) {
            shader.v4("finalClearCoat", clearCoatColor)
            shader.v2("finalClearCoatRoughMetallic", clearCoatRoughness, clearCoatMetallic)
        } else {
            shader.v4("finalClearCoat", 0f)
        }

        for ((uniformName, valueType) in shaderOverrides) {
            valueType.bind(shader, uniformName)
        }
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

    override fun hashCode(): Int {
        var result = shaderOverrides.hashCode()
        result = 31 * result + (pipelineStage?.hashCode() ?: 0)
        result = 31 * result + (shader?.hashCode() ?: 0)
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
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Material

        if (shaderOverrides != other.shaderOverrides) return false
        if (pipelineStage != other.pipelineStage) return false
        if (shader != other.shader) return false
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

        return true
    }

    override fun clone(): Material {
        // todo fast copy option
        val material = Material()
        copy(material)
        return material
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Material
        clone.diffuseBase = diffuseBase
        clone.diffuseMap = diffuseMap
        clone.emissiveBase = emissiveBase
        clone.emissiveMap = emissiveMap
        clone.normalMap = normalMap
        clone.normalStrength = normalStrength
        clone.sheen = sheen
        clone.sheenNormalMap = sheenNormalMap
        clone.occlusionStrength = occlusionStrength
        clone.occlusionMap = occlusionMap
        clone.shaderOverrides = shaderOverrides
        clone.roughnessMinMax = roughnessMinMax
        clone.roughnessMap = roughnessMap
        clone.metallicMap = metallicMap
        clone.metallicMinMax = metallicMinMax
        clone.clearCoatColor = clearCoatColor
        clone.clearCoatRoughness = clearCoatRoughness
        clone.clearCoatMetallic = clearCoatMetallic
        clone.shader = shader
        clone.pipelineStage = pipelineStage
        // todo other stuff, that we missed
    }

    override val className: String = "Material"

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.initNoGFX()
            val prefab = loadPrefab(OS.documents.getChild("cube bricks.glb"))!!
            for (change in prefab.adds) {
                LOGGER.info(change)
            }
            for (change in prefab.sets) {
                LOGGER.info(change)
            }
            val instance = prefab.createInstance()
            LOGGER.info(instance)
        }

    }

}