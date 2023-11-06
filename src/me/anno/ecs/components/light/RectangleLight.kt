package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.smoothCube
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d

class RectangleLight() : LightComponent(LightType.RECTANGLE) {

    constructor(src: RectangleLight) : this() {
        src.copyInto(this)
    }

    @Range(0.0, 1.0)
    var width = 0.1f

    @Range(0.0, 1.0)
    var height = 0.1f

    override fun getShaderV0(): Float = width
    override fun getShaderV1(): Float = height

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        cameraRotation: Quaterniond,
        cameraDirection: Vector3d,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int
    ) {
    }

    override fun drawShape() {
        // todo show rectangle instead
        val dx = width * 0.5
        drawLine(entity, -dx, 0.0, 0.0, dx, 0.0, 0.0)
        val dy = height * 0.5
        drawLine(entity, 0.0, -dy, 0.0, 0.0, dy, 0.0)
    }

    override fun getLightPrimitive(): Mesh = smoothCube.front

    override fun clone() = RectangleLight(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as RectangleLight
        dst.width = width
        dst.height = height
    }

    override val className: String get() = "RectangleLight"

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