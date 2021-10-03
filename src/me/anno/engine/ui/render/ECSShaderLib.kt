package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader

object ECSShaderLib {

    lateinit var pbrModelShader: BaseShader

    fun init() {

        // todo automatic texture mapping and indices
        // todo use shader stages for that everywhere

        val shader = ECSMeshShader("model")
        pbrModelShader = shader
        shader.ignoreUniformWarnings(
            listOf(
                "finalSheen", "finalTranslucency", "metallicMinMax",
                "emissiveBase", "normalStrength", "ambientLight",
                "occlusionStrength", "invLocalTransform",
                "numberOfLights", "roughnessMinMax", "finalClearCoat"
            )
        )
        val textures = mutableListOf(
            "diffuseMap",
            "normalMap",
            "emissiveMap",
            "roughnessMap",
            "metallicMap",
            "occlusionMap",
            "sheenNormalMap",
            "reflectionPlane"
        )
        for (i in 0 until Renderers.MAX_PLANAR_LIGHTS) {
            textures.add("shadowMapPlanar$i")
        }
        for (i in 0 until Renderers.MAX_CUBEMAP_LIGHTS) {
            textures.add("shadowMapCubic$i")
        }
        pbrModelShader.ignoreUniformWarnings(textures)
        pbrModelShader.setTextureIndices(textures)
        pbrModelShader.glslVersion = 330

    }

}