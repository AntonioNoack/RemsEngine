package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GPUShader
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

// todo clear-coat and sheen are very specific ->
//  render them as a base, and later render over them, when light-levels are known???
open class Material : MaterialBase() {

    @Type("Tiling")
    @Group("Color")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var diffuseTiling = Vector4f(1f, 1f, 0f, 0f)

    @Group("Color")
    @Docs("Color and transparency as far as it's supported")
    @Range(0.0, 1.0)
    @Type("Color4")
    var diffuseBase = Vector4f(1f)
        set(value) {
            field.set(value)
        }

    @Group("Color")
    @Docs("Diffuse Texture")
    @Type("Texture/Reference")
    var diffuseMap: FileReference = InvalidRef

    @Type("Tiling")
    @Group("Normals")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var normalTiling = Vector4f(1f, 1f, 0f, 0f)

    @Group("Normals")
    @Docs("Strength for normal map. Negative values flip the map inside out.")
    @Range(-100.0, 100.0)
    var normalStrength = 1f

    @Group("Normals")
    @Docs("Typically cyan colored with patterns; bump maps (grayscale height) work, too")
    @Type("Texture/Reference")
    var normalMap: FileReference = InvalidRef

    // translucency:
    // only little light directionality
    @Group("Glass")
    @Docs("Internal diffuse refractions. The higher, the less the material cares about light directions.")
    @Range(0.0, 1.0)
    var translucency = 0f

    @Type("Tiling")
    @Group("Lighting")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var emissiveTiling = Vector4f(1f, 1f, 0f, 0f)

    // base * map
    @Group("Lighting")
    @Docs("How much light is emitted by the surface")
    @Range(0.0, 100.0)
    @Type("Color3HDR")
    var emissiveBase = Vector3f(0f)
        set(value) {
            field.set(value)
        }

    @Group("Lighting")
    @Docs("How much light is emitted by the surface, texture")
    @Type("Texture/Reference")
    var emissiveMap: FileReference = InvalidRef

    @Type("Tiling")
    @Group("PBR")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var roughnessTiling = Vector4f(1f, 1f, 0f, 0f)

    // mix(min,max,value(uv)) or 1 if undefined)
    @Group("PBR")
    @Range(0.0, 1.0)
    var roughnessMinMax = Vector2f(0f, 1f)

    @Group("PBR")
    @Docs("Texture for roughness. Black gets mapped to roughnessMinMax.x, white to roughnessMinMax.y.")
    @Type("Texture/Reference")
    var roughnessMap: FileReference = InvalidRef

    @Type("Tiling")
    @Group("PBR")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var metallicTiling = Vector4f(1f, 1f, 0f, 0f)

    // mix(min,max,map(uv)) or 1 if undefined)
    @Group("PBR")
    @Range(0.0, 1.0)
    var metallicMinMax = Vector2f(0f, 0f)

    @Group("PBR")
    @Docs("Texture for metallic. Black gets mapped to metallicMinMax.x, white to metallicMinMax.y.")
    @Type("Texture/Reference")
    var metallicMap: FileReference = InvalidRef

    @Type("Tiling")
    @Group("PBR")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var occlusionTiling = Vector4f(1f, 1f, 0f, 0f)

    @Group("Lighting")
    @Docs("Constant factor for baked ambient occlusion: light *= 1-(1-occlusion) * strength")
    @Range(0.0, 100.0)
    var occlusionStrength = 1f

    @Group("Lighting")
    @Docs("UV-based factor for baked ambient occlusion: light *= 1-(1-occlusion) * strength")
    @Type("Texture/Reference")
    var occlusionMap: FileReference = InvalidRef

    // the last component is the strength
    // clear coat is a thin coating over the material,
    // which typically has a single color,
    // and is only seen at steep angles
    // this is typically seen on cars
    // if you don't like the monotonicity, you can add your own fresnel effect in the shader
    @Group("Metals")
    @Type("Color3")
    var clearCoatColor = Vector3f(1f, 1f, 1f)
        set(value) {
            field.set(value)
        }

    @Group("Metals")
    @Range(0.0, 1.0)
    var clearCoatStrength = 0f

    @Group("Metals")
    @Range(0.0, 1.0)
    var clearCoatMetallic = 1f

    @Group("Metals")
    @Range(0.0, 1.0)
    var clearCoatRoughness = 0f

    @Type("Tiling")
    @Group("Fabric")
    @Docs("uv = uv * tiling.xy + tiling.zw")
    var sheenTiling = Vector4f(1f, 1f, 0f, 0f)

    // adds soft diffuse light capture on steep angles,
    // can be used for clothing, where the fibers catch the light
    @Group("Fabric")
    @Range(0.0, 1.0)
    var sheen = 0f

    // e.g. for patterns, e.g. stroking over some clothes leaves patterns
    @Group("Fabric")
    @Type("Texture/Reference")
    var sheenNormalMap: FileReference = InvalidRef

    // todo bug: input is allowing values < 1
    @Group("Glass")
    @Docs("Defines refraction/reflection strength for transparent materials")
    @Range(1.0, 5.0)
    var indexOfRefraction = 1.5f

    override fun bind(shader: GPUShader) {

        // all the data, the shader needs to know from the material
        // GFX.check()

        val white = TextureLib.whiteTexture
        val n001 = TextureLib.normalTexture

        bindTexture(shader, "occlusionMap", occlusionMap, white)
        bindTexture(shader, "metallicMap", metallicMap, white)
        bindTexture(shader, "roughnessMap", roughnessMap, white)
        bindTexture(shader, "emissiveMap", emissiveMap, white)
        val sheenNormalTex = bindTexture(shader, "sheenNormalMap", sheenNormalMap, n001)
        val normalTex = bindTexture(shader, "normalMap", normalMap, n001)
        bindTexture(shader, "diffuseMap", diffuseMap, white)

        // GFX.check()

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

        shader.v4f("diffuseTiling", diffuseTiling)
        shader.v4f("emissiveTiling", emissiveTiling)
        shader.v4f("normalTiling", normalTiling)
        shader.v4f("roughnessTiling", roughnessTiling)
        shader.v4f("metallicTiling", metallicTiling)
        shader.v4f("sheenTiling", sheenTiling)

        // GFX.check()

        if (clearCoatStrength > 0f) {
            shader.v4f("clearCoat", clearCoatColor, clearCoatStrength)
            shader.v2f("clearCoatRoughMetallic", clearCoatRoughness, clearCoatMetallic)
        } else {
            shader.v4f("clearCoat", 0f)
        }

        super.bind(shader)

    }

    override fun listTextures() = listOf(
        diffuseMap,
        emissiveMap,
        normalMap,
        roughnessMap,
        metallicMap,
        occlusionMap,
    )

    override fun listTiling() = listOf(
        diffuseTiling,
        emissiveTiling,
        normalTiling,
        roughnessTiling,
        metallicTiling,
        occlusionTiling
    )

    override fun hashCode(): Int {
        // only hash common properties?
        var result = super.hashCode()
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
        result = 31 * result + occlusionStrength.hashCode()
        result = 31 * result + occlusionMap.hashCode()
        result = 31 * result + translucency.hashCode()
        result = 31 * result + diffuseTiling.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // if you have a customized Material class, you must implement your own equals function
        if (this::class != Material::class || other !is Material ||
            other::class != Material::class
        ) return false

        return equalProperties(other)
    }

    override fun equalProperties(other: MaterialBase): Boolean {
        if (!super.equalProperties(other)) return false
        if (other !is Material) return false

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
        if (occlusionStrength != other.occlusionStrength) return false
        if (occlusionMap != other.occlusionMap) return false
        if (translucency != other.translucency) return false
        if (diffuseTiling != other.diffuseTiling) return false

        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Material) return
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
        dst.roughnessMap = roughnessMap
        dst.roughnessMinMax.set(roughnessMinMax)
        dst.metallicMap = metallicMap
        dst.metallicMinMax.set(metallicMinMax)
        dst.clearCoatColor.set(clearCoatColor)
        dst.clearCoatStrength = clearCoatStrength
        dst.clearCoatRoughness = clearCoatRoughness
        dst.clearCoatMetallic = clearCoatMetallic
        dst.diffuseTiling.set(diffuseTiling)
    }

    companion object {

        val defaultMaterial = Material()

        fun diffuse(color: Int): Material {
            val mat = Material()
            color.toVecRGBA(mat.diffuseBase)
            mat.diffuseBase.w = 1f
            return mat
        }

        fun metallic(color: Int, roughness: Float): Material {
            val base = diffuse(color)
            base.roughnessMinMax.set(roughness)
            base.metallicMinMax.set(1f)
            return base
        }
    }
}