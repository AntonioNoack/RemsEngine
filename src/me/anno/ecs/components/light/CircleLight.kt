package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.effectiveSpecular
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawCircle
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.smoothCube
import org.joml.Matrix4f
import org.joml.Matrix4x3m
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class CircleLight : LightComponent(LightType.CIRCLE) {

    @Range(0.0, 1.0)
    var radius = 0.1f

    override fun getShaderV0(): Float = radius

    override fun updateShadowMap(
        cascadeScale: Float,
        worldScale: Float,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaternionf,
        cameraDirection: Vector3f,
        drawTransform: Matrix4x3m,
        pipeline: Pipeline,
        resolution: Int
    ) {
    }

    override fun drawShape(pipeline: Pipeline) {
        drawCircle(entity, radius.toDouble(), 0, 1, 0.0)
    }

    override fun getLightPrimitive(): Mesh = smoothCube.front

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CircleLight) return
        dst.radius = radius
    }

    companion object {
        // todo calculate how much light shines by reflections
        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    "lightPos.z *= pow(1.0+shaderV0, 2.0);\n" +
                    "float lightDistXY = length(lightPos.xy);\n" +
                    "lightPos.xy *= max(lightDistXY-shaderV0, 0.001) / lightDistXY;\n" +
                    (if (cutoffContinue != null) "if(dot(lightPos,lightPos)>1.0) $cutoffContinue;\n" else "") + // outside
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    effectiveSpecular
        }
    }
}