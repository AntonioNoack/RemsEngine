package me.anno.ecs.components.light

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

abstract class TexturedLight(lightType: LightType) : LightComponent(lightType) {

    @Type("Texture/Reference")
    var texture: FileReference = InvalidRef

    @Docs("How big the projected texture shall be; ignored for SpotLight")
    @Range(0.0, 3.0)
    var textureSize = 0.1f

    var filtering: Filtering = Filtering.TRULY_LINEAR
    var clamping: Clamping = Clamping.CLAMP

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is TexturedLight) return
        dst.texture = texture
        dst.textureSize = textureSize
        dst.clamping = clamping
        dst.filtering = filtering
    }

    companion object {
        val lightColorCode = "" +
                "ivec2 colorSize = textureSize(lightColorMap,0);\n" +
                "vec2 colorUV = lightPos0 * float(max(colorSize.x, colorSize.y)) / (shaderV2 * vec2(colorSize)) * 0.5 + 0.5;\n" +
                // choose mipmap based on distance?... we would be special mipmaps though...
                "vec4 color = texture(lightColorMap, colorUV);\n" +
                "lightColor *= color.rgb;\n"
    }

}