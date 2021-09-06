package me.anno.ecs.components.light

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.cubeMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.gui.LineShapes.drawArrowZ
import me.anno.engine.gui.LineShapes.drawCone
import me.anno.gpu.drawing.Perspective.setPerspective2
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import org.joml.*
import kotlin.math.atan
import kotlin.math.tan

// a cone light
class SpotLight() : LightComponent(LightType.SPOT) {

    constructor(src: SpotLight) : this() {
        src.copy(this)
    }

    // for a large angle, it just becomes a point light
    @Range(0.0, 100.0)
    var coneAngle = 1.0

    @HideInInspector
    @NotSerializedProperty
    val fovRadians
        get() = 2.0 * atan(coneAngle)

    @HideInInspector
    @NotSerializedProperty
    val halfFovRadians
        get() = tan(coneAngle)

    @SerializedProperty
    @Range(1e-6, 1.0)
    var near = 0.001

    override fun updateShadowMap(
        cascadeScale: Double,
        worldScale: Double,
        cameraMatrix: Matrix4f,
        drawTransform: Matrix4x3d,
        pipeline: Pipeline,
        resolution: Int,
        position: Vector3d,
        rotation: Quaterniond
    ) {
        val far = 1.0
        val coneAngle = coneAngle * cascadeScale
        val fovYRadians = 2.0 * atan(coneAngle)
        setPerspective2(cameraMatrix, coneAngle.toFloat(), near.toFloat(), far.toFloat())
        cameraMatrix.rotate(Quaternionf(rotation).invert())
        pipeline.frustum.definePerspective(
            near / worldScale, far / worldScale, fovYRadians, resolution, resolution,
            1.0, position, rotation,
        )
    }

    override fun drawShape() {
        drawCone(entity, coneAngle)
        drawArrowZ(entity, 0.0, -1.0)
    }

    // for deferred rendering, this could be optimized
    override fun getLightPrimitive(): Mesh = cubeMesh

    override fun clone() = SpotLight(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SpotLight
        clone.coneAngle = coneAngle
        clone.near = near
    }

    override val className: String = "SpotLight"

    companion object {
        val coneFunction = "clamp((localNormal.z-(1.0-coneAngle))/coneAngle,0.0,1.0)"
        // val coneFunction = "smoothstep(0.0, 1.0, (localNormal.z-(1.0-coneAngle))/coneAngle)"
    }

}