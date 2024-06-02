package me.anno.ecs.components.light

import me.anno.gpu.shader.GLSLType

enum class LightType(val id: Int, val shadowMapType: GLSLType) {
    // with shadows
    DIRECTIONAL(0, GLSLType.S2DAShadow),
    SPOT(1, GLSLType.S2DAShadow),
    POINT(2, GLSLType.SCubeShadow),
    // without shadows for now
    CIRCLE(3, GLSLType.S2D),
    RECTANGLE(4, GLSLType.S2D);

    companion object {
        fun getShaderCode(type: LightType, cutoffKeyword: String, withShadows: Boolean): String {
            return when (type) {
                SPOT -> SpotLight.getShaderCode(cutoffKeyword, withShadows)
                DIRECTIONAL -> DirectionalLight.getShaderCode(cutoffKeyword, withShadows)
                POINT -> PointLight.getShaderCode(cutoffKeyword, withShadows)
                CIRCLE -> CircleLight.getShaderCode(cutoffKeyword, withShadows)
                RECTANGLE -> RectangleLight.getShaderCode(cutoffKeyword, withShadows)
            }
        }
    }
}