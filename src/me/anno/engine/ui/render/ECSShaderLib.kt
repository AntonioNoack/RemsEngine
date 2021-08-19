package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader

object ECSShaderLib {

    lateinit var pbrModelShader: BaseShader

    fun init() {

        pbrModelShader = ECSMeshShader("model")
        pbrModelShader.setTextureIndices(
            listOf(
                "diffuseMap",
                "normalMap",
                "emissiveMap",
                "roughnessMap",
                "metallicMap",
                "occlusionMap"
            )
        )
        pbrModelShader.glslVersion = 330

    }

}