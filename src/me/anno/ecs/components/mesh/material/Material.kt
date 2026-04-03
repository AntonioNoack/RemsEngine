package me.anno.ecs.components.mesh.material

import me.anno.ecs.annotations.Docs
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

    // todo bug: input is allowing values < 1
    @Docs("Defines refraction/reflection strength for transparent materials")
    @Range(1.0, 5.0)
    var indexOfRefraction = 1.5f

    override fun bind(shader: GPUShader) {

        // all the data, the shader needs to know from the material
        // GFX.check()

        val white = TextureLib.whiteTexture
        val n001 = TextureLib.normalTexture

        val f = if (linearFiltering) Filtering.LINEAR else Filtering.NEAREST
        val c = clamping
        bindTexture(shader, "occlusionMap", occlusionMap, white, f, c)
        bindTexture(shader, "metallicMap", metallicMap, white, f, c)
        bindTexture(shader, "roughnessMap", roughnessMap, white, f, c)
        bindTexture(shader, "emissiveMap", emissiveMap, white, f, c)
        val sheenNormalTex = bindTexture(shader, "sheenNormalMap", sheenNormalMap, n001, f, c)
        val normalTex = bindTexture(shader, "normalMap", normalMap, n001, f, c)
        bindTexture(shader, "diffuseMap", diffuseMap, white, f, c)

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
        displacementMap,
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
        result = 31 * result + displacementMap.hashCode()
        result = 31 * result + occlusionStrength.hashCode()
        result = 31 * result + occlusionMap.hashCode()
        result = 31 * result + translucency.hashCode()
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
        if (displacementMap != other.displacementMap) return false
        if (occlusionStrength != other.occlusionStrength) return false
        if (occlusionMap != other.occlusionMap) return false
        if (translucency != other.translucency) return false

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