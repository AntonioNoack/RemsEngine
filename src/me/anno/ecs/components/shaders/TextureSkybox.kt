package me.anno.ecs.components.shaders

import me.anno.ecs.components.mesh.TypeValueV2
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.TextureLib
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

abstract class TextureSkybox : SkyboxBase() {

    var imageFile: FileReference = InvalidRef
    var applyInverseTonemapping = false

    init {
        skyColor.set(1f) // changing default value
        material.shaderOverrides["applyInverseToneMapping"] = TypeValueV2(GLSLType.V1B) {
            applyInverseTonemapping
        }
        material.shaderOverrides["skyTexture"] = TypeValueV2(GLSLType.S2D) {
            val texture = ImageGPUCache[imageFile, true] ?: TextureLib.whiteTexture
            val shader = shader?.value
            if (shader != null) {
                // ensure proper filtering
                texture.bind(shader, "skyTexture", GPUFiltering.LINEAR, Clamping.REPEAT)
            }
            texture
        }
    }
}