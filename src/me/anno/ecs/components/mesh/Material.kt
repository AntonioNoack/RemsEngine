package me.anno.ecs.components.mesh

import me.anno.Engine
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageGPUCache
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.zip.InnerTmpFile
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class Material : PrefabSaveable() {

    // todo most properties here should be defined by the shader, not this class
    // todo we then somehow must display them dynamically

    @Type("Map<String,TypeValue>")
    @SerializedProperty
    var shaderOverrides = HashMap<String, TypeValue>()
        set(value) {
            if (field !== value) {
                field.clear()
                field.putAll(value)
            }
        }

    // or not yet...
    // todo this needs to be easy to change for transparency/non-transparent objects
    @Type("PipelineStage?")
    @NotSerializedProperty
    var pipelineStage: PipelineStage? = null

    // or not yet...
    @Type("BaseShader?")
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
    var emissiveBase = Vector3f(0f)

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

    operator fun set(name: String, type: GLSLType, value: Any) {
        shaderOverrides[name] = TypeValue(type, value)
    }

    operator fun set(name: String, value: TypeValue) {
        shaderOverrides[name] = value
    }

    fun defineShader(shader: Shader) {

        // all the data, the shader needs to know from the material

        val white = TextureLib.whiteTexture
        val n001 = TextureLib.normalTexture

        bindTexture(shader, "occlusionMap", occlusionMap, white)
        bindTexture(shader, "metallicMap", metallicMap, white)
        bindTexture(shader, "roughnessMap", roughnessMap, white)
        bindTexture(shader, "emissiveMap", emissiveMap, white)
        val sheenNormalTex = bindTexture(shader, "sheenNormalMap", sheenNormalMap, white)
        val normalTex = bindTexture(shader, "normalMap", normalMap, n001)
        bindTexture(shader, "diffuseMap", diffuseMap, white)

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
        if (shaderOverrides != other.shaderOverrides) return false

        return true
    }

    override fun clone(): Material {
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
        clone.shaderOverrides.clear()
        clone.shaderOverrides.putAll(shaderOverrides)
        clone.roughnessMinMax = roughnessMinMax
        clone.roughnessMap = roughnessMap
        clone.metallicMap = metallicMap
        clone.metallicMinMax = metallicMinMax
        clone.clearCoatColor = clearCoatColor
        clone.clearCoatRoughness = clearCoatRoughness
        clone.clearCoatMetallic = clearCoatMetallic
        clone.shader = shader
        clone.pipelineStage = pipelineStage
        clone.isDoubleSided = isDoubleSided
        // todo other stuff, that we missed
    }

    override val className: String = "Material"

    companion object {

        val timeout = 1000L
        private val LOGGER = LogManager.getLogger(Material::class)

        fun getTex(image: FileReference) = ImageGPUCache.getImage(image, timeout, true)

        fun bindTexture(shader: Shader, name: String, file: FileReference, default: Texture2D): Texture2D? {
            val index = shader.getTextureIndex(name)
            return if (index >= 0) {
                val tex = getTex(file)
                (tex ?: default).bind(index)
                tex
            } else {
                LOGGER.warn("Didn't find texture $name in ${shader.name}")
                null
            }
        }

        /**
         * create a procedural material:
         * @return material instance, and file reference (which then can be set in mesh components, meshes and such)
         * */
        fun create(): Material {
            val prefab = Prefab("Material")
            val material = prefab.getSampleInstance() as Material
            material.prefab = prefab
            material.prefabPath = Path.ROOT_PATH
            prefab.source = InnerTmpFile.InnerTmpPrefabFile(prefab)
            return material
        }

        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.initNoGFX()
            val prefab = PrefabCache[OS.documents.getChild("cube bricks.glb")]!!
            for (change in prefab.adds) {
                LOGGER.info(change)
            }
            for (change in prefab.sets) {
                LOGGER.info(change)
            }
            val instance = prefab.createInstance()
            LOGGER.info(instance)
            Engine.requestShutdown()
        }

    }

}