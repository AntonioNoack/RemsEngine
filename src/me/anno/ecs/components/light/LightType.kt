package me.anno.ecs.components.light

import me.anno.gpu.shader.GLSLType

enum class LightType(val id: Int, val shadowMapType: GLSLType) {
    // with shadows
    DIRECTIONAL(0, GLSLType.S2DShadow),
    SPOT(1, GLSLType.S2DShadow),
    POINT(2, GLSLType.SCubeShadow),
    // without shadows for now
    CIRCLE(3, GLSLType.S2D),
    RECTANGLE(4, GLSLType.S2D);

    companion object {
        fun getShaderCode(type: LightType, co: String, ws: Boolean): String {
            return when (type) {
                SPOT -> SpotLight.getShaderCode(co, ws)
                DIRECTIONAL -> DirectionalLight.getShaderCode(co, ws)
                POINT -> PointLight.getShaderCode(co, ws)
                CIRCLE -> CircleLight.getShaderCode(co, ws)
                RECTANGLE -> RectangleLight.getShaderCode(co, ws)
            }
        }
    }
}