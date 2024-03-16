package me.anno.ecs.components.light.sky

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.material.utils.TypeValueV2
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

abstract class TextureSkybox : SkyboxBase() {

    var imageFile: FileReference = InvalidRef
    var applyInverseTonemapping = false

    @Docs("The input file might produce too bright colors; set this to limit the input, and to reduce bloom with it")
    @Range(0.0, 1e38)
    var maxBrightness = 250f

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextureSkybox
        dst.imageFile = imageFile
        dst.applyInverseTonemapping = applyInverseTonemapping
    }

    init {
        skyColor.set(1f) // changing default value
        material.shaderOverrides["maxBrightness"] = TypeValueV2(GLSLType.V1F) { maxBrightness }
        material.shaderOverrides["applyInverseToneMapping"] = TypeValueV2(GLSLType.V1B) {
            applyInverseTonemapping
        }
        material.shaderOverrides["skyTexture"] = TypeValueV2(GLSLType.S2D) {
            val texture = TextureCache[imageFile, true] ?: TextureLib.whiteTexture
            val shader = shader?.value
            if (shader != null) {
                // ensure proper filtering
                texture.bind(shader, "skyTexture", Filtering.LINEAR, Clamping.REPEAT)
            }
            texture
        }
    }
}