package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.smoothCube
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class RectangleLight : LightComponent(LightType.RECTANGLE) {

    @Range(0.0, 1.0)
    var width = 0.1f

    @Range(0.0, 1.0)
    var height = 0.1f

    override fun getShaderV0(): Float = width
    override fun getShaderV1(): Float = height

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
    }

    override fun drawShape(pipeline: Pipeline) {
        LineShapes.drawBox(
            entity, JomlPools.vec3d.borrow().set(
                width.toDouble(), height.toDouble(), 0.0
            )
        )
    }

    override fun getLightPrimitive(): Mesh = smoothCube.front

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is RectangleLight) return
        dst.width = width
        dst.height = height
    }

    companion object {
        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    "lightPos.x = max(abs(lightPos.x)-shaderV0, 0.001) * (1.0+shaderV0);\n" +
                    "lightPos.y = max(abs(lightPos.y)-shaderV1, 0.001) * (1.0+shaderV1);\n" +
                    (if (cutoffContinue != null) "if(dot(lightPos,lightPos)>1.0) $cutoffContinue;\n" else "") + // outside
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    "effectiveSpecular = effectiveDiffuse;\n"
        }
    }
}