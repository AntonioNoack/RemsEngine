package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.smoothCube
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * depending on the relative position, select color from texture
 * depending on the distance relative to the size, choose mipmap level? ...
 * todo support shadows for one-sided rectangle lights? -> use SpotLight with texture for now
 * todo factor to make one side dimmer? if so, same for circle-light
 * */
class RectangleLight : TexturedLight(LightType.RECTANGLE) {

    @Range(0.0, 1.0)
    var width = 0.1f

    @Range(0.0, 1.0)
    var height = 0.1f

    override fun getShaderV0(): Float = width
    override fun getShaderV1(): Float = height
    override fun getShaderV2(): Float = textureSize

    override fun drawShape(pipeline: Pipeline) {
        LineShapes.drawBox(
            entity, LineShapes.defaultColor,
            width.toDouble(), height.toDouble(), 0.0
        )
    }

    override fun getLightPrimitive(): Mesh = smoothCube.front

    override fun updateShadowMap(
        cascadeScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf,
        cameraDirection: Vector3f,
        drawTransform: Matrix4x3,
        pipeline: Pipeline,
        resolution: Int
    ) {
        // todo same logic as for spot-light shadows... what angle should we support?
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is RectangleLight) return
        dst.width = width
        dst.height = height
    }

    companion object {
        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    "vec2 lightPos0 = lightPos.xy;\n" +
                    "lightPos.x = max(abs(lightPos.x)-shaderV0, 0.001) * (1.0+shaderV0);\n" +
                    "lightPos.y = max(abs(lightPos.y)-shaderV1, 0.001) * (1.0+shaderV1);\n" +
                    (if (cutoffContinue != null) "if(dot(lightPos,lightPos)>1.0) $cutoffContinue;\n" else "") + // outside
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" +

                    lightColorCode +

                    // todo shader-code similar to SpotLights

                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    "effectiveSpecular = effectiveDiffuse;\n"
        }
    }
}