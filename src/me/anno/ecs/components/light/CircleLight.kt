package me.anno.ecs.components.light

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.LineShapes.drawCircle
import me.anno.gpu.pipeline.Pipeline
import me.anno.mesh.Shapes.smoothCube
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Quaterniond
import org.joml.Vector3d

class CircleLight() : LightComponent(LightType.CIRCLE) {

    constructor(src: CircleLight) : this() {
        src.copyInto(this)
    }

    @Range(0.0, 1.0)
    var radius = 0.1f

    override fun getShaderV0(): Float = radius

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        dstCameraMatrix: Matrix4f,
        dstCameraPosition: Vector3d,
        dstCameraDirection: Vector3d,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
    }

    override fun drawShape() {
        drawCircle(entity, radius.toDouble(), 0, 1, 0.0)
    }

    override fun getLightPrimitive(): Mesh = smoothCube.front

    override fun clone() = CircleLight(this)

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CircleLight
        dst.radius = radius
    }

    override val className: String get() = "TubeLight"

    companion object {
        fun getShaderCode(cutoffContinue: String?, withShadows: Boolean): String {
            return "" +
                    "float lightDistXY = length(lightPos.xy);\n" +
                    "lightPos.xy *= max(lightDistXY-shaderV0, 0.001) * (1.0+shaderV0) / lightDistXY;\n" +
                    (if (cutoffContinue != null) "if(dot(lightPos,lightPos)>1.0) $cutoffContinue;\n" else "") + // outside
                    "lightDir = normalize(-lightPos);\n" +
                    "NdotL = dot(lightDir, lightNor);\n" +
                    "effectiveDiffuse = lightColor * $falloff;\n" +
                    "effectiveSpecular = effectiveDiffuse;\n"
        }
    }
}