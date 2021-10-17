package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.builder.Variable

object ECSShaderLib {

    lateinit var pbrModelShader: BaseShader
    lateinit var clearingPbrModelShader: BaseShader

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
        shader.ignoreUniformWarnings(textures)
        shader.setTextureIndices(textures)
        shader.glslVersion = 330

        clearingPbrModelShader = BaseShader(
            "clear-pbr", "" +
                    "attribute vec3 coords;\n" +
                    "uniform mat4 transform;\n" +
                    "void main(){\n" +
                    "   vec3 finalPosition = coords;\n" +
                    "   finalNormal = normalize(coords);\n" +
                    "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    "   finalPosition *= 1e5;\n" +
                    "}", listOf(Variable("vec3", "finalNormal")), "" +
                    "void main(){\n" +
                    // tricking the detection for variable definitions,
                    // because it doesn't check the varyings, it seems
                    "   // finalNormal\n" +
                    "   vec3 finalPosition = finalNormal * 1e35;\n" + // 1e38 is max for float
                    "}"
        )

    }

}