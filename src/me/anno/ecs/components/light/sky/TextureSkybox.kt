package me.anno.ecs.components.light.sky

import me.anno.ecs.components.mesh.TypeValueV2
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

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextureSkybox
        dst.imageFile = imageFile
        dst.applyInverseTonemapping = applyInverseTonemapping
    }

    init {
        skyColor.set(1f) // changing default value
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